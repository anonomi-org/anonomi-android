package org.anonomi.android.blog;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.blog.BlogCommentHeader;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.LIKE_MARKER;
import static org.anonchatsecure.anonchat.api.blog.MessageType.COMMENT;
import static org.anonchatsecure.anonchat.api.blog.MessageType.POST;
import static org.anonchatsecure.anonchat.api.blog.MessageType.WRAPPED_POST;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * A post is identified by the ID it had in the blog it was first posted to,
 * so a wrapped copy is the same post while two posts that merely share an
 * author and an instant are not.
 */
public class BlogPostIdentityTest {

	private final GroupId groupId = new GroupId(getRandomId());
	private final GroupId otherGroupId = new GroupId(getRandomId());
	private final Author alice = getAuthor();
	private final Author bob = getAuthor();
	private final AuthorInfo info = new AuthorInfo(UNVERIFIED);

	private BlogPostHeader post(Author author, long timestamp) {
		return new BlogPostHeader(POST, groupId, new MessageId(getRandomId()),
				timestamp, timestamp, author, info, false, true);
	}

	/**
	 * The same post as it appears after being wrapped into another blog.
	 * Only ever reached as a comment's parent: getPostHeaders returns POST
	 * and COMMENT only, so a wrapped header is never a list item.
	 */
	private BlogPostHeader wrappedCopyOf(BlogPostHeader original) {
		return new BlogPostHeader(WRAPPED_POST, otherGroupId,
				new MessageId(getRandomId()), null, original.getTimestamp(),
				original.getTimeReceived(), original.getAuthor(), info, false,
				true, false, original.getOriginalId());
	}

	@Test
	public void anUnwrappedPostIsItsOwnOriginal() {
		BlogPostHeader h = post(alice, 1000);
		assertEquals(h.getId(), h.getOriginalId());
		assertEquals(h.getId(), BaseViewModel.postKey(h));
	}

	@Test
	public void twoPostsFromOneAuthorAtTheSameInstantAreDistinct() {
		// an RSS feed gives every entry the same local author, and entries
		// with a date-only publication date land on the same millisecond
		BlogPostHeader first = post(alice, 1000);
		BlogPostHeader second = post(alice, 1000);
		assertNotEquals(BaseViewModel.postKey(first),
				BaseViewModel.postKey(second));
	}

	@Test
	public void deduplicateKeepsPostsThatOnlyShareAuthorAndTimestamp() {
		List<BlogPostItem> items = new ArrayList<>();
		items.add(new BlogPostItem(post(alice, 1000), "first entry"));
		items.add(new BlogPostItem(post(alice, 1000), "second entry"));
		items.add(new BlogPostItem(post(alice, 1000), "third entry"));

		assertEquals(3, BaseViewModel.deduplicate(items).size());
	}

	@Test
	public void aWrappedCopyKeepsTheIdentityOfTheOriginal() {
		BlogPostHeader original = post(alice, 1000);
		BlogPostHeader copy = wrappedCopyOf(original);
		assertNotEquals(original.getId(), copy.getId());
		assertEquals(BaseViewModel.postKey(original),
				BaseViewModel.postKey(copy));
	}

	@Test
	public void deduplicateCollapsesTheSamePostListedTwice() {
		// loadBlogPost appends the post it is opening to a list that already
		// contains it, which is the duplicate deduplicate actually sees
		BlogPostHeader original = post(alice, 1000);
		List<BlogPostItem> items = new ArrayList<>();
		items.add(new BlogPostItem(original, "the post"));
		items.add(new BlogPostItem(original, "the post"));

		assertEquals(1, BaseViewModel.deduplicate(items).size());
	}

	@Test
	public void aLikeOnAWrappedCopyCountsTowardsTheOriginal() {
		BlogPostHeader original = post(alice, 1000);
		BlogPostHeader copy = wrappedCopyOf(original);
		// bob likes the copy he can see in his own blog
		BlogCommentHeader like = new BlogCommentHeader(COMMENT, otherGroupId,
				LIKE_MARKER, copy, new MessageId(getRandomId()), 2000, 2000,
				bob, info, true);

		List<BlogPostItem> items = new ArrayList<>();
		items.add(new BlogPostItem(original, "the post"));
		items.add(new BlogCommentItem(like));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(1, items.size());
		assertEquals(1, items.get(0).getLikeCount());
	}

	@Test
	public void aLikeDoesNotReachAPostThatMerelySharesAuthorAndTimestamp() {
		BlogPostHeader liked = post(alice, 1000);
		BlogPostHeader other = post(alice, 1000);
		BlogCommentHeader like = new BlogCommentHeader(COMMENT, groupId,
				LIKE_MARKER, liked, new MessageId(getRandomId()), 2000, 2000,
				bob, info, true);

		List<BlogPostItem> items = new ArrayList<>();
		items.add(new BlogPostItem(liked, "the liked post"));
		items.add(new BlogPostItem(other, "a different post"));
		items.add(new BlogCommentItem(like));

		BaseViewModel.filterAndAggregateLikes(items, alice.getId());

		assertEquals(2, items.size());
		assertEquals(1, items.get(0).getLikeCount());
		assertEquals(0, items.get(1).getLikeCount());
	}
}
