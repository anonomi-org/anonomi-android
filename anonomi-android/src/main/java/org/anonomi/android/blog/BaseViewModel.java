package org.anonomi.android.blog;

import android.app.Application;

import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.anonomi.android.viewmodel.DbViewModel;
import org.anonomi.android.viewmodel.LiveResult;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogCommentHeader;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.util.HtmlUtils;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.LogUtils.now;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.COMMENT_MARKER;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.isComment;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.LIKE_MARKER;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.UNLIKE_MARKER;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.OURSELVES;

@NotNullByDefault
abstract class BaseViewModel extends DbViewModel implements EventListener {

	private static final Logger LOG = getLogger(BaseViewModel.class.getName());

	private final EventBus eventBus;
	protected final IdentityManager identityManager;
	protected final AndroidNotificationManager notificationManager;
	protected final BlogManager blogManager;

	protected final MutableLiveData<LiveResult<ListUpdate>> blogPosts =
			new MutableLiveData<>();

	@Nullable
	protected volatile LocalAuthor localAuthor;

	BaseViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			EventBus eventBus,
			IdentityManager identityManager,
			AndroidNotificationManager notificationManager,
			BlogManager blogManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor);
		this.eventBus = eventBus;
		this.identityManager = identityManager;
		this.notificationManager = notificationManager;
		this.blogManager = blogManager;
		eventBus.addListener(this);
		loadLocalAuthor();
	}

	private void loadLocalAuthor() {
		runOnDbThread(() -> {
			try {
				localAuthor = identityManager.getLocalAuthor();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		eventBus.removeListener(this);
	}

	@DatabaseExecutor
	protected List<BlogPostItem> loadBlogPosts(Transaction txn, GroupId groupId)
			throws DbException {
		long start = now();
		List<BlogPostHeader> headers =
				blogManager.getPostHeaders(txn, groupId);
		logDuration(LOG, "Loading headers", start);
		List<BlogPostItem> items = new ArrayList<>(headers.size());
		start = now();
		for (BlogPostHeader h : headers) {
			BlogPostItem item = getItem(txn, h);
			items.add(item);
		}
		logDuration(LOG, "Loading bodies", start);
		return items;
	}

	/**
	 * Loads items from all subscribed blogs into one list.
	 */
	@DatabaseExecutor
	protected List<BlogPostItem> loadAllBlogItems(Transaction txn)
			throws DbException {
		List<BlogPostItem> all = new ArrayList<>();
		for (GroupId g : blogManager.getBlogIds(txn)) {
			all.addAll(loadBlogPosts(txn, g));
		}
		return all;
	}

	@DatabaseExecutor
	protected BlogPostItem getItem(Transaction txn, BlogPostHeader h)
			throws DbException {
		String text;
		if (h instanceof BlogCommentHeader) {
			BlogCommentHeader c = (BlogCommentHeader) h;
			BlogCommentItem item = new BlogCommentItem(c);
			BlogPostHeader postHeader = item.getPostHeader();
			text = getPostText(txn, postHeader.getId());
			item.setText(text);
			if (postHeader.hasImage()) {
				item.setImageData(
						blogManager.getPostImageData(txn, postHeader.getId()),
						blogManager.getPostImageContentType(txn,
								postHeader.getId()));
			}
			return item;
		} else {
			text = getPostText(txn, h.getId());
			if (h.hasImage()) {
				byte[] imageData =
						blogManager.getPostImageData(txn, h.getId());
				String imageContentType =
						blogManager.getPostImageContentType(txn, h.getId());
				return new BlogPostItem(h, text, imageData, imageContentType);
			}
			return new BlogPostItem(h, text);
		}
	}

	@DatabaseExecutor
	private String getPostText(Transaction txn, MessageId m)
			throws DbException {
		return HtmlUtils.cleanArticle(blogManager.getPostText(txn, m));
	}

	LiveData<LiveResult<BlogPostItem>> loadBlogPost(GroupId g, MessageId m) {
		MutableLiveData<LiveResult<BlogPostItem>> result =
				new MutableLiveData<>();
		runOnDbThread(true, txn -> {
			long start = now();
			BlogPostHeader header = blogManager.getPostHeader(txn, g, m);
			BlogPostItem item = getItem(txn, header);
			// Load all blog items to aggregate cross-blog likes/comments
			List<BlogPostItem> allItems = loadAllBlogItems(txn);
			AuthorId localAuthorId =
					identityManager.getLocalAuthor().getId();
			// Add the target post so aggregation can attach data to it
			allItems.add(item);
			filterAndAggregateLikes(allItems, localAuthorId);
			// deduplicate
			allItems = deduplicate(allItems);
			// Find the item again after deduplication
			for (BlogPostItem i : allItems) {
				if (postKey(i.getHeader()).equals(postKey(header))) {
					result.postValue(new LiveResult<>(i));
					return;
				}
			}
			logDuration(LOG, "Loading post", start);
		}, e -> {
			logException(LOG, WARNING, e);
			result.postValue(new LiveResult<>(e));
		});
		return result;
	}

	protected void onBlogPostAdded(BlogPostHeader header, boolean local) {
		runOnDbThread(true, txn -> {
			BlogPostItem item = getItem(txn, header);
			AuthorId localAuthorId = identityManager.getLocalAuthor().getId();
			// Check if this is a like/unlike/comment special entry
			boolean isSpecial = false;
			if (item instanceof BlogCommentItem) {
				BlogCommentItem commentItem = (BlogCommentItem) item;
				String comment = commentItem.getHeader().getComment();
				isSpecial = isSpecialComment(comment);
			}
			boolean finalIsSpecial = isSpecial;
			txn.attach(() -> {
				if (finalIsSpecial) {
					onSpecialCommentAdded(item, localAuthorId);
				} else {
					onBlogPostItemAdded(item, local);
				}
			});
		}, this::handleException);
	}

	@UiThread
	private void onSpecialCommentAdded(BlogPostItem specialItem,
			AuthorId localAuthorId) {
		List<BlogPostItem> items = getBlogPostItems();
		if (items == null || !(specialItem instanceof BlogCommentItem)) return;
		BlogCommentItem commentItem = (BlogCommentItem) specialItem;
		BlogCommentHeader header = commentItem.getHeader();
		String comment = header.getComment();
		MessageId targetKey = postKey(header.getParent());

		// Find the target post
		int targetIndex = -1;
		for (int i = 0; i < items.size(); i++) {
			if (postKey(items.get(i).getHeader()).equals(targetKey)) {
				targetIndex = i;
				break;
			}
		}
		if (targetIndex == -1) return;

		BlogPostItem target = items.get(targetIndex).copy();

		// Incrementally update the target post's state
		if (isLikeOrUnlike(comment)) {
			boolean isLike = LIKE_MARKER.equals(comment);
			List<BlogLiker> likers = applyLike(target.getLikers(),
					header.getAuthor(), header.getAuthorInfo(), isLike);
			if (likers == null) return; // nothing changes for this author
			target.setLikers(likers);
			target.setLikeCount(likers.size());
			if (header.getAuthor().getId().equals(localAuthorId)) {
				target.setLikedByMe(isLike);
			}
		} else if (isComment(comment)) {
			String commentText = comment.substring(COMMENT_MARKER.length());
			List<BlogComment> comments =
					new ArrayList<>(target.getBlogComments());

			// Remove optimistic version if it exists
			Iterator<BlogComment> it = comments.iterator();
			while (it.hasNext()) {
				BlogComment bc = it.next();
				if (bc.optimistic &&
						bc.author.getId().equals(header.getAuthor().getId()) &&
						bc.text.equals(commentText)) {
					it.remove();
				}
			}

			// Check if we already have this comment (non-optimistic)
			for (BlogComment c : comments) {
				if (c.author.getId().equals(header.getAuthor().getId()) &&
						c.timestamp == header.getTimestamp() &&
						c.text.equals(commentText)) {
					return;
				}
			}
			comments.add(new BlogComment(header.getAuthor(),
					header.getAuthorInfo(), commentText,
					header.getTimestamp(), header.getTimeReceived(), false));
			sortComments(comments);
			target.setBlogComments(comments);
		}

		List<BlogPostItem> newList = new ArrayList<>(items);
		newList.set(targetIndex, target);
		blogPosts.setValue(new LiveResult<>(new ListUpdate(null, newList)));
	}

	@UiThread
	private void onBlogPostItemAdded(BlogPostItem item, boolean local) {
		List<BlogPostItem> items = getBlogPostItems();
		if (items == null) return;
		
		List<BlogPostItem> updatedList = new ArrayList<>(items);
		updatedList.add(item);
		updatedList = deduplicate(updatedList);
		
		Collections.sort(updatedList);
		blogPosts.setValue(new LiveResult<>(new ListUpdate(local, updatedList)));
	}

	void repeatPost(BlogPostItem item, @Nullable String comment) {
		runOnDbThread(() -> {
			try {
				LocalAuthor a = identityManager.getLocalAuthor();
				Blog b = blogManager.getPersonalBlog(a);
				BlogPostHeader h = item.getHeader();
				blogManager.addLocalComment(a, b.getId(), comment, h);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@UiThread
	void likePost(BlogPostItem item) {
		updateBlogPostItemOptimistically(item, true, null);
		runOnDbThread(() -> {
			try {
				LocalAuthor a = identityManager.getLocalAuthor();
				Blog b = blogManager.getPersonalBlog(a);
				blogManager.addLocalComment(a, b.getId(), LIKE_MARKER,
						item.getHeader());
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@UiThread
	void unlikePost(BlogPostItem item) {
		updateBlogPostItemOptimistically(item, false, null);
		runOnDbThread(() -> {
			try {
				LocalAuthor a = identityManager.getLocalAuthor();
				Blog b = blogManager.getPersonalBlog(a);
				blogManager.addLocalComment(a, b.getId(), UNLIKE_MARKER,
						item.getHeader());
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@UiThread
	void commentOnPost(BlogPostItem item, String comment) {
		updateBlogPostItemOptimistically(item, null, comment);
		runOnDbThread(() -> {
			try {
				LocalAuthor a = identityManager.getLocalAuthor();
				Blog b = blogManager.getPersonalBlog(a);
				blogManager.addLocalComment(a, b.getId(),
						COMMENT_MARKER + comment, item.getHeader());
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@UiThread
	private void updateBlogPostItemOptimistically(BlogPostItem item,
			@Nullable Boolean liked, @Nullable String comment) {
		List<BlogPostItem> items = getBlogPostItems();
		if (items == null) return;

		MessageId targetKey = postKey(item.getHeader());
		int targetIndex = -1;
		for (int i = 0; i < items.size(); i++) {
			if (postKey(items.get(i).getHeader()).equals(targetKey)) {
				targetIndex = i;
				break;
			}
		}

		if (targetIndex != -1) {
			BlogPostItem target = items.get(targetIndex).copy();
			if (liked != null) {
				if (liked && !target.isLikedByMe()) {
					setLocalLike(target, true);
				} else if (!liked && target.isLikedByMe()) {
					setLocalLike(target, false);
				}
			}
			if (comment != null) {
				if (localAuthor == null) {
					// Fallback to async if localAuthor not yet loaded
					runOnDbThread(true, txn -> {
						localAuthor = identityManager.getLocalAuthor(txn);
						androidExecutor.runOnUiThread(() ->
								updateBlogPostItemOptimistically(item, null,
										comment));
					}, e -> logException(LOG, WARNING, e));
					return;
				}
				List<BlogComment> comments =
						new ArrayList<>(target.getBlogComments());
				long now = System.currentTimeMillis();
				comments.add(new BlogComment(localAuthor,
						new AuthorInfo(OURSELVES), comment,
						now, now, true));
				sortComments(comments);
				target.setBlogComments(comments);
			}
			List<BlogPostItem> newList = new ArrayList<>(items);
			newList.set(targetIndex, target);
			blogPosts.setValue(new LiveResult<>(new ListUpdate(null, newList)));
		}
	}

	/** Keeps likeCount equal to likers.size() once the local author is known. */
	@UiThread
	private void setLocalLike(BlogPostItem target, boolean liked) {
		target.setLikedByMe(liked);
		LocalAuthor a = localAuthor;
		if (a == null) {
			// Not loaded yet: move the count so the tap feels immediate and
			// let the event for our own comment rebuild the list.
			target.setLikeCount(liked ? target.getLikeCount() + 1
					: Math.max(0, target.getLikeCount() - 1));
			return;
		}
		List<BlogLiker> likers = applyLike(target.getLikers(), a,
				new AuthorInfo(OURSELVES), liked);
		if (likers == null) return;
		target.setLikers(likers);
		target.setLikeCount(likers.size());
	}

	/**
	 * Applies one like or unlike, or returns null if it changes nothing for
	 * that author.
	 * <p>
	 * Known defect, not a rule: this decides on arrival order while a reload
	 * decides on the latest timestamp, so an author's own like and unlike
	 * arriving reversed leaves the wrong state until the next reload. Fixing
	 * it needs each author's last action kept per item, not just the likers.
	 */
	@Nullable
	static List<BlogLiker> applyLike(List<BlogLiker> current, Author author,
			AuthorInfo authorInfo, boolean isLike) {
		List<BlogLiker> likers = new ArrayList<>(current);
		boolean wasLiking = false;
		for (BlogLiker l : likers) {
			if (l.author.getId().equals(author.getId())) {
				wasLiking = true;
				break;
			}
		}
		if (isLike == wasLiking) return null;
		if (isLike) {
			likers.add(new BlogLiker(author, authorInfo));
		} else {
			Iterator<BlogLiker> it = likers.iterator();
			while (it.hasNext()) {
				if (it.next().author.getId().equals(author.getId())) {
					it.remove();
					break;
				}
			}
		}
		return likers;
	}

	static boolean isLikeOrUnlike(@Nullable String comment) {
		return LIKE_MARKER.equals(comment) || UNLIKE_MARKER.equals(comment);
	}

	/**
	 * Returns true if this comment text is a special marker (like, unlike,
	 * or comment) that should be filtered from the visible post list.
	 */
	static boolean isSpecialComment(@Nullable String comment) {
		return isLikeOrUnlike(comment) || isComment(comment);
	}

	/**
	 * Identifies a post across every blog it has been wrapped into, so a like
	 * on a reblog counts towards the same post as one on the original.
	 */
	static MessageId postKey(BlogPostHeader h) {
		return h.getOriginalId();
	}

	/**
	 * Filters like/unlike/comment entries from the list and aggregates them
	 * onto matching posts. Likes become counts; comments are attached as
	 * {@link BlogComment} objects for display.
	 */
	static void filterAndAggregateLikes(List<BlogPostItem> items,
			AuthorId localAuthorId) {
		// Map from target post to per-author like state
		Map<MessageId, Map<AuthorId, LikeAction>> postLikes =
				new HashMap<>();
		// Map from target post key to list of comments
		Map<MessageId, List<BlogComment>> postComments = new HashMap<>();

		// Collect like/unlike/comment actions and mark items for removal
		List<BlogPostItem> toRemove = new ArrayList<>();
		for (BlogPostItem item : items) {
			if (!(item instanceof BlogCommentItem)) continue;
			BlogCommentItem commentItem = (BlogCommentItem) item;
			BlogCommentHeader header = commentItem.getHeader();
			String comment = header.getComment();

			if (isLikeOrUnlike(comment)) {
				// Use direct parent as target (not root post) so likes
				// on reblogs attach to the reblog, not the original
				MessageId targetKey = postKey(header.getParent());
				AuthorId authorId = header.getAuthor().getId();
				boolean isLike = LIKE_MARKER.equals(comment);
				long timestamp = header.getTimestamp();

				Map<AuthorId, LikeAction> authorMap =
						postLikes.get(targetKey);
				if (authorMap == null) {
					authorMap = new HashMap<>();
					postLikes.put(targetKey, authorMap);
				}

				LikeAction existing = authorMap.get(authorId);
				if (existing == null || timestamp > existing.timestamp) {
					authorMap.put(authorId,
							new LikeAction(isLike, timestamp,
									header.getAuthor(),
									header.getAuthorInfo()));
				}
				toRemove.add(item);
			} else if (isComment(comment)) {
				// Use direct parent as target
				MessageId targetKey = postKey(header.getParent());
				String commentText =
						comment.substring(COMMENT_MARKER.length());
				long timestamp = header.getTimestamp();
				long timeReceived = header.getTimeReceived();

				List<BlogComment> comments = postComments.get(targetKey);
				if (comments == null) {
					comments = new ArrayList<>();
					postComments.put(targetKey, comments);
				}
				
				// Deduplicate comments (same author, timestamp and text)
				boolean duplicate = false;
				for (BlogComment existing : comments) {
					if (existing.author.getId().equals(header.getAuthor().getId()) &&
							existing.timestamp == timestamp &&
							existing.text.equals(commentText)) {
						duplicate = true;
						break;
					}
				}
				if (!duplicate) {
					comments.add(new BlogComment(header.getAuthor(),
							header.getAuthorInfo(), commentText, timestamp, timeReceived, false));
				}
				toRemove.add(item);
			}
		}

		// Remove like/unlike/comment items from the list
		items.removeAll(toRemove);

		// Apply like counts, likedByMe, and comments to matching posts.
		// Match on the item's own header (not inner post header) so
		// reblogs get their own like/comment counts.
		for (BlogPostItem item : items) {
			MessageId key = postKey(item.getHeader());

			// Likes
			Map<AuthorId, LikeAction> authorMap = postLikes.get(key);
			if (authorMap != null) {
				boolean likedByMe = false;
				List<BlogLiker> likers = new ArrayList<>();
				for (Map.Entry<AuthorId, LikeAction> entry :
						authorMap.entrySet()) {
					LikeAction la = entry.getValue();
					if (!la.isLike) continue;
					if (entry.getKey().equals(localAuthorId)) {
						likedByMe = true;
					}
					likers.add(new BlogLiker(la.author, la.authorInfo));
				}
				// One fact, not two: keep the count derived from the list.
				item.setLikeCount(likers.size());
				item.setLikedByMe(likedByMe);
				item.setLikers(likers);
			}

			// Comments
			List<BlogComment> comments = postComments.get(key);
			if (comments != null) {
				sortComments(comments);
				item.setBlogComments(comments);
				// Deduplicated commenter authors
				Map<AuthorId, BlogLiker> commenterMap =
						new LinkedHashMap<>();
				for (BlogComment c : comments) {
					AuthorId cId = c.author.getId();
					if (!commenterMap.containsKey(cId)
							&& c.authorInfo != null) {
						commenterMap.put(cId,
								new BlogLiker(c.author, c.authorInfo));
					}
				}
				item.setCommenterAuthors(
						new ArrayList<>(commenterMap.values()));
			}
		}
	}

	protected static void sortComments(List<BlogComment> comments) {
		Collections.sort(comments, (a, b) -> {
			int res = Long.compare(a.timeReceived, b.timeReceived);
			if (res != 0) return res;
			return a.text.compareTo(b.text);
		});
	}

	static List<BlogPostItem> buildAndAggregatePosts(
			List<BlogPostHeader> headers, BlogManager blogManager,
			AuthorId localAuthorId) throws DbException {
		List<BlogPostItem> items = new ArrayList<>(headers.size());
		for (BlogPostHeader h : headers) {
			if (h instanceof BlogCommentHeader) {
				BlogCommentHeader c = (BlogCommentHeader) h;
				BlogCommentItem item = new BlogCommentItem(c);
				BlogPostHeader postHeader = item.getPostHeader();
				String text = HtmlUtils.cleanArticle(
						blogManager.getPostText(postHeader.getId()));
				item.setText(text);
				items.add(item);
			} else {
				String text = HtmlUtils.cleanArticle(
						blogManager.getPostText(h.getId()));
				items.add(new BlogPostItem(h, text));
			}
		}
		filterAndAggregateLikes(items, localAuthorId);
		return deduplicate(items);
	}

	protected static List<BlogPostItem> deduplicate(List<BlogPostItem> items) {
		Map<MessageId, BlogPostItem> unique = new LinkedHashMap<>();
		for (BlogPostItem item : items) {
			MessageId key = postKey(item.getHeader());
			BlogPostItem existing = unique.get(key);
			if (existing == null || isBetter(item, existing)) {
				unique.put(key, item);
			}
		}
		return new ArrayList<>(unique.values());
	}

	private static boolean isBetter(BlogPostItem newItem, BlogPostItem existing) {
		if (newItem instanceof BlogCommentItem && !(existing instanceof BlogCommentItem)) return true;
		return newItem.getHeader().getTimeReceived() > existing.getHeader().getTimeReceived();
	}

	private static class LikeAction {
		final boolean isLike;
		final long timestamp;
		final org.anonchatsecure.bramble.api.identity.Author author;
		final AuthorInfo authorInfo;

		LikeAction(boolean isLike, long timestamp,
				org.anonchatsecure.bramble.api.identity.Author author,
				AuthorInfo authorInfo) {
			this.isLike = isLike;
			this.timestamp = timestamp;
			this.author = author;
			this.authorInfo = authorInfo;
		}
	}

	static class BlogLiker {
		final org.anonchatsecure.bramble.api.identity.Author author;
		final AuthorInfo authorInfo;

		BlogLiker(org.anonchatsecure.bramble.api.identity.Author author,
				AuthorInfo authorInfo) {
			this.author = author;
			this.authorInfo = authorInfo;
		}
	}

	static class BlogComment {
		final org.anonchatsecure.bramble.api.identity.Author author;
		@Nullable
		final org.anonchatsecure.anonchat.api.identity.AuthorInfo authorInfo;
		final String text;
		final long timestamp;
		final long timeReceived;
		final boolean optimistic;

		BlogComment(
				org.anonchatsecure.bramble.api.identity.Author author,
				@Nullable org.anonchatsecure.anonchat.api.identity.AuthorInfo
						authorInfo,
				String text, long timestamp, long timeReceived, boolean optimistic) {
			this.author = author;
			this.authorInfo = authorInfo;
			this.text = text;
			this.timestamp = timestamp;
			this.timeReceived = timeReceived;
			this.optimistic = optimistic;
		}
	}

	LiveData<LiveResult<ListUpdate>> getBlogPosts() {
		return blogPosts;
	}

	@UiThread
	@Nullable
	protected List<BlogPostItem> getBlogPostItems() {
		LiveResult<ListUpdate> value = blogPosts.getValue();
		if (value == null) return null;
		ListUpdate result = value.getResultOrNull();
		return result == null ? null : result.getItems();
	}

	/**
	 * Call this after {@link ListUpdate#getPostAddedWasLocal()} was processed.
	 * This prevents it from getting processed again.
	 */
	@UiThread
	void resetLocalUpdate() {
		LiveResult<ListUpdate> value = blogPosts.getValue();
		if (value == null) return;
		ListUpdate result = value.getResultOrNull();
		if (result != null) result.postAddedWasLocal = null;
	}

	static class ListUpdate {

		@Nullable
		private Boolean postAddedWasLocal;
		private final List<BlogPostItem> items;

		ListUpdate(@Nullable Boolean postAddedWasLocal,
				List<BlogPostItem> items) {
			this.postAddedWasLocal = postAddedWasLocal;
			this.items = items;
		}

		/**
		 * @return null when not a single post was added with this update.
		 * true when a single post was added locally and false if remotely.
		 */
		@Nullable
		public Boolean getPostAddedWasLocal() {
			return postAddedWasLocal;
		}

		public List<BlogPostItem> getItems() {
			return items;
		}
	}
}
