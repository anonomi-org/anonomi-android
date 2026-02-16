package org.anonomi.android.blog;

import org.anonomi.android.conversation.MapMessageData;

interface OnBlogPostClickListener {

	void onBlogPostClick(BlogPostItem post);

	void onAuthorClick(BlogPostItem post);

	void onLinkClick(String url);

	void onMapMessageClicked(MapMessageData data);

	void onLikeClick(BlogPostItem post);

	void onCommentClick(BlogPostItem post);

	void onInteractionsClick(BlogPostItem post);
}
