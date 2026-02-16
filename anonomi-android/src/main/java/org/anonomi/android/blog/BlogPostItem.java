package org.anonomi.android.blog;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.NonNull;

@NotThreadSafe
public class BlogPostItem implements Comparable<BlogPostItem> {

	private final BlogPostHeader header;
	@Nullable
	protected String text;
	private final boolean read;
	@Nullable
	protected byte[] imageData;
	@Nullable
	protected String imageContentType;
	private int likeCount = 0;
	private boolean likedByMe = false;
	private List<BaseViewModel.BlogComment> blogComments =
			Collections.emptyList();
	private List<BaseViewModel.BlogLiker> likers = Collections.emptyList();
	private List<BaseViewModel.BlogLiker> commenterAuthors =
			Collections.emptyList();

	BlogPostItem(BlogPostHeader header, @Nullable String text) {
		this(header, text, null, null);
	}

	BlogPostItem(BlogPostHeader header, @Nullable String text,
			@Nullable byte[] imageData, @Nullable String imageContentType) {
		this.header = header;
		this.text = text;
		this.read = header.isRead();
		this.imageData = imageData;
		this.imageContentType = imageContentType;
	}

	protected BlogPostItem(BlogPostItem other) {
		this.header = other.header;
		this.text = other.text;
		this.read = other.read;
		this.imageData = other.imageData;
		this.imageContentType = other.imageContentType;
		this.likeCount = other.likeCount;
		this.likedByMe = other.likedByMe;
		this.blogComments = new ArrayList<>(other.blogComments);
		this.likers = new ArrayList<>(other.likers);
		this.commenterAuthors = new ArrayList<>(other.commenterAuthors);
	}

	public MessageId getId() {
		return header.getId();
	}

	public GroupId getGroupId() {
		return header.getGroupId();
	}

	public long getTimestamp() {
		return header.getTimestamp();
	}

	public Author getAuthor() {
		return header.getAuthor();
	}

	AuthorInfo getAuthorInfo() {
		return header.getAuthorInfo();
	}

	@Nullable
	public String getText() {
		return text;
	}

	boolean isRssFeed() {
		return header.isRssFeed();
	}

	public boolean isRead() {
		return read;
	}

	public BlogPostHeader getHeader() {
		return header;
	}

	BlogPostHeader getPostHeader() {
		return getHeader();
	}

	public boolean hasImage() {
		return imageData != null && imageData.length > 0;
	}

	@Nullable
	public byte[] getImageData() {
		return imageData;
	}

	@Nullable
	public String getImageContentType() {
		return imageContentType;
	}

	void setImageData(@Nullable byte[] imageData,
			@Nullable String imageContentType) {
		this.imageData = imageData;
		this.imageContentType = imageContentType;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}

	public boolean isLikedByMe() {
		return likedByMe;
	}

	public void setLikedByMe(boolean likedByMe) {
		this.likedByMe = likedByMe;
	}

	public List<BaseViewModel.BlogComment> getBlogComments() {
		return blogComments;
	}

	public void setBlogComments(List<BaseViewModel.BlogComment> comments) {
		this.blogComments = comments;
	}

	public List<BaseViewModel.BlogLiker> getLikers() {
		return likers;
	}

	public void setLikers(List<BaseViewModel.BlogLiker> likers) {
		this.likers = likers;
	}

	public List<BaseViewModel.BlogLiker> getCommenterAuthors() {
		return commenterAuthors;
	}

	public void setCommenterAuthors(
			List<BaseViewModel.BlogLiker> commenterAuthors) {
		this.commenterAuthors = commenterAuthors;
	}

	@Override
	public int compareTo(@NonNull BlogPostItem other) {
		if (this == other) return 0;
		return compare(getHeader(), other.getHeader());
	}

	protected static int compare(BlogPostHeader h1, BlogPostHeader h2) {
		// The newest post comes first
		return Long.compare(h2.getTimeReceived(), h1.getTimeReceived());
	}

	public BlogPostItem copy() {
		return new BlogPostItem(this);
	}
}
