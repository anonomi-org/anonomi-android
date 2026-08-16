package org.anonomi.android.blog;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.blog.BlogCommentHeader;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonomi.android.blog.BaseViewModel.BlogLiker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import static java.util.Collections.emptyList;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.LIKE_MARKER;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.UNLIKE_MARKER;
import static org.anonchatsecure.anonchat.api.blog.MessageType.COMMENT;
import static org.anonchatsecure.anonchat.api.blog.MessageType.POST;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A post's like count is one per author, whether it is reached by a full
 * reload or by incremental updates, and both routes must agree.
 */
public class BlogReactionAggregationTest {

	private final GroupId groupId = new GroupId(getRandomId());
	private final Author alice = getAuthor();
	private final Author bob = getAuthor();
	private final Author carol = getAuthor();
	private final AuthorInfo info = new AuthorInfo(UNVERIFIED);

	private BlogPostHeader postHeader(Author author, long timestamp) {
		return new BlogPostHeader(POST, groupId,
				new MessageId(getRandomId()), timestamp, timestamp, author,
				info, false, true);
	}

	private BlogPostItem post(BlogPostHeader h) {
		return new BlogPostItem(h, "the post text");
	}

	private BlogCommentItem reaction(Author author, BlogPostHeader target,
			@Nullable String marker, long timestamp) {
		BlogCommentHeader h = new BlogCommentHeader(COMMENT, groupId, marker,
				target, new MessageId(getRandomId()), timestamp, timestamp,
				author, info, true);
		return new BlogCommentItem(h);
	}

	// ---------- the reload path ----------

	@Test
	public void repeatedLikesFromOneAuthorCountOnce() {
		BlogPostHeader target = postHeader(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));
		items.add(reaction(bob, target, LIKE_MARKER, 2001));
		items.add(reaction(bob, target, LIKE_MARKER, 2002));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(1, items.size()); // reactions are filtered out
		assertEquals(1, items.get(0).getLikeCount());
		assertEquals(1, items.get(0).getLikers().size());
	}

	@Test
	public void latestActionPerAuthorWins() {
		BlogPostHeader target = postHeader(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));
		items.add(reaction(bob, target, UNLIKE_MARKER, 3000));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(0, items.get(0).getLikeCount());
		assertTrue(items.get(0).getLikers().isEmpty());
	}

	@Test
	public void unlikeFromAuthorWhoNeverLikedDoesNotDecrement() {
		BlogPostHeader target = postHeader(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));
		items.add(reaction(carol, target, UNLIKE_MARKER, 3000));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(1, items.get(0).getLikeCount());
	}

	@Test
	public void likeCountAlwaysEqualsLikerCount() {
		BlogPostHeader target = postHeader(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));
		items.add(reaction(carol, target, LIKE_MARKER, 2001));
		items.add(reaction(alice, target, LIKE_MARKER, 2002));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		BlogPostItem p = items.get(0);
		assertEquals(3, p.getLikeCount());
		assertEquals(p.getLikeCount(), p.getLikers().size());
		assertTrue(p.isLikedByMe()); // alice is the local author
	}

	@Test
	public void likedByMeIsFalseWhenOnlyOthersLiked() {
		BlogPostHeader target = postHeader(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertFalse(items.get(0).isLikedByMe());
	}

	// ---------- the incremental path ----------

	@Test
	public void repeatedLikeIsIgnoredIncrementally() {
		List<BlogLiker> likers =
				BaseViewModel.applyLike(emptyList(), bob, info, true);
		assertNotNull(likers);
		assertEquals(1, likers.size());
		// the same author liking again changes nothing
		assertNull(BaseViewModel.applyLike(likers, bob, info, true));
	}

	@Test
	public void unlikeFromNonLikerIsIgnoredIncrementally() {
		List<BlogLiker> likers =
				BaseViewModel.applyLike(emptyList(), bob, info, true);
		assertNotNull(likers);
		assertNull(BaseViewModel.applyLike(likers, carol, info, false));
	}

	@Test
	public void unlikeRemovesOnlyThatAuthor() {
		List<BlogLiker> likers =
				BaseViewModel.applyLike(emptyList(), bob, info, true);
		likers = BaseViewModel.applyLike(likers, carol, info, true);
		assertNotNull(likers);
		assertEquals(2, likers.size());
		likers = BaseViewModel.applyLike(likers, bob, info, false);
		assertNotNull(likers);
		assertEquals(1, likers.size());
		assertEquals(carol.getId(), likers.get(0).author.getId());
	}

	// ---------- the two paths must not disagree ----------

	@Test
	public void incrementalMatchesReloadWhenEventsArriveInOrder() {
		BlogPostHeader target = postHeader(alice, 1000);
		// bob likes twice and keeps it, carol likes then takes it back
		Object[][] events = {
				{bob, LIKE_MARKER, 2000L},
				{bob, LIKE_MARKER, 2001L},
				{carol, LIKE_MARKER, 2002L},
				{carol, UNLIKE_MARKER, 2003L},
				{bob, LIKE_MARKER, 2004L},
		};

		// incremental: apply each event as it arrives
		List<BlogLiker> incremental = emptyList();
		for (Object[] ev : events) {
			List<BlogLiker> next = BaseViewModel.applyLike(incremental,
					(Author) ev[0], info, LIKE_MARKER.equals(ev[1]));
			if (next != null) incremental = next;
		}

		// reload: aggregate the same events from scratch
		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		for (Object[] ev : events) {
			items.add(reaction((Author) ev[0], target, (String) ev[1],
					(Long) ev[2]));
		}
		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(items.get(0).getLikeCount(), incremental.size());
		assertEquals(1, incremental.size());
		assertEquals(bob.getId(), incremental.get(0).author.getId());
	}

	/**
	 * Arrival order decides incrementally, latest timestamp on a reload, so
	 * one author's own like and unlike arriving reversed disagree until the
	 * next reload. Pinned so a change to either rule is deliberate.
	 */
	@Test
	public void incrementalAndReloadDifferWhenOneAuthorsEventsArriveReversed() {
		BlogPostHeader target = postHeader(alice, 1000);

		// the unlike is newer, but it arrives first
		List<BlogLiker> incremental = emptyList();
		List<BlogLiker> afterUnlike =
				BaseViewModel.applyLike(incremental, bob, info, false);
		if (afterUnlike != null) incremental = afterUnlike;
		List<BlogLiker> afterLike =
				BaseViewModel.applyLike(incremental, bob, info, true);
		if (afterLike != null) incremental = afterLike;

		List<BlogPostItem> items = new ArrayList<>();
		items.add(post(target));
		items.add(reaction(bob, target, UNLIKE_MARKER, 3000));
		items.add(reaction(bob, target, LIKE_MARKER, 2000));
		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(1, incremental.size());          // incremental: liking
		assertEquals(0, items.get(0).getLikeCount()); // reload: not liking
	}

	@Test
	public void repeatedLikesFromOneAuthorStillCountOnce() {
		List<BlogLiker> likers = emptyList();
		for (int i = 0; i < 50; i++) {
			List<BlogLiker> next =
					BaseViewModel.applyLike(likers, bob, info, true);
			if (next != null) likers = next;
		}
		assertEquals(1, likers.size());
	}

	@Test
	public void unlikesFromANonLikerLeaveTheCountAlone() {
		List<BlogLiker> likers =
				BaseViewModel.applyLike(emptyList(), alice, info, true);
		assertNotNull(likers);
		for (int i = 0; i < 50; i++) {
			List<BlogLiker> next =
					BaseViewModel.applyLike(likers, bob, info, false);
			if (next != null) likers = next;
		}
		assertEquals(1, likers.size());
		assertEquals(alice.getId(), likers.get(0).author.getId());
	}

	@Test
	public void localAuthorIdIsNotSpecialInTheRule() {
		AuthorId localId = alice.getId();
		List<BlogLiker> likers =
				BaseViewModel.applyLike(emptyList(), alice, info, true);
		assertNotNull(likers);
		assertEquals(localId, likers.get(0).author.getId());
		assertNull(BaseViewModel.applyLike(likers, alice, info, true));
	}
}
