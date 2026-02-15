package org.anonomi.android.blog;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.Spanned;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.conversation.MapMessageData;
import org.anonomi.android.view.AuthorView;
import org.anonomi.android.view.ImageViewActivity;
import org.anonchatsecure.anonchat.api.blog.BlogCommentHeader;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

import androidx.annotation.UiThread;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.anonomi.android.activity.BriarActivity.GROUP_ID;
import static org.anonomi.android.blog.BlogPostFragment.POST_ID;
import static org.anonomi.android.util.UiUtils.TEASER_LENGTH;
import static org.anonomi.android.util.UiUtils.getSpanned;
import static org.anonomi.android.util.UiUtils.getTeaser;
import static org.anonomi.android.util.UiUtils.makeLinksClickable;
import static org.anonomi.android.view.AuthorView.COMMENTER;
import static org.anonomi.android.view.AuthorView.REBLOGGER;
import static org.anonomi.android.view.AuthorView.RSS_FEED_REBLOGGED;

@UiThread
@NotNullByDefault
class BlogPostViewHolder extends RecyclerView.ViewHolder {

	private final Context ctx;
	private final ViewGroup layout;
	private final AuthorView reblogger;
	private final AuthorView author;
	private final ImageButton reblogButton;
	private final ImageButton likeButton;
	private final TextView likeCountText;
	@Nullable
	private final ImageButton commentButton;
	private final TextView text;
	private final TextView reblogCommentText;
	private final ViewGroup commentContainer;
	@Nullable
	private final ImageView imageContent;
	private final TextView mapContent;
	private final boolean fullText, authorClickable;
	private final int padding;

	private final OnBlogPostClickListener listener;

	BlogPostViewHolder(View v, boolean fullText,
			OnBlogPostClickListener listener, boolean authorClickable) {
		super(v);
		this.fullText = fullText;
		this.listener = listener;
		this.authorClickable = authorClickable;

		ctx = v.getContext();
		layout = v.findViewById(R.id.postLayout);
		reblogger = v.findViewById(R.id.rebloggerView);
		author = v.findViewById(R.id.authorView);
		reblogButton = v.findViewById(R.id.commentView);
		likeButton = v.findViewById(R.id.likeButton);
		likeCountText = v.findViewById(R.id.likeCount);
		commentButton = v.findViewById(R.id.commentButton);
		text = v.findViewById(R.id.textView);
		reblogCommentText = v.findViewById(R.id.reblogCommentText);
		commentContainer = v.findViewById(R.id.commentContainer);
		imageContent = v.findViewById(R.id.imageContent);
		mapContent = v.findViewById(R.id.mapContent);
		padding = ctx.getResources()
				.getDimensionPixelSize(R.dimen.listitem_vertical_margin);
	}

	void hideReblogButton() {
		reblogButton.setVisibility(GONE);
	}

	void hideActionButtons() {
		if (likeButton != null) likeButton.setVisibility(GONE);
		if (likeCountText != null) likeCountText.setVisibility(GONE);
		if (commentButton != null) commentButton.setVisibility(GONE);
	}

	void updateDate(long time) {
		author.setDate(time);
	}

	void setTransitionName(MessageId id) {
		ViewCompat.setTransitionName(layout, getTransitionName(id));
	}

	private String getTransitionName(MessageId id) {
		return "blogPost" + id.hashCode();
	}

	void bindItem(BlogPostItem item) {
		setTransitionName(item.getId());
		if (!fullText) {
			layout.setClickable(true);
			layout.setOnClickListener(v -> listener.onBlogPostClick(item));
		}

		boolean isReblog = item instanceof BlogCommentItem;

		// author and date
		BlogPostHeader post = item.getPostHeader();
		author.setAuthor(post.getAuthor(), post.getAuthorInfo());
		author.setDate(post.getTimestamp());
		author.setPersona(
				item.isRssFeed() ? AuthorView.RSS_FEED : AuthorView.NORMAL);
		// TODO make author clickable more often #624
		if (authorClickable && !isReblog) {
			author.setAuthorClickable(v -> listener.onAuthorClick(item));
		} else {
			author.setAuthorNotClickable();
		}

		// image content
		if (imageContent != null) {
			if (item.hasImage()) {
				byte[] data = item.getImageData();
				if (data != null) {
					android.graphics.Bitmap bitmap =
							BitmapFactory.decodeByteArray(data, 0, data.length);
					if (bitmap != null) {
						imageContent.setImageBitmap(bitmap);
						imageContent.setVisibility(VISIBLE);
						imageContent.setOnClickListener(
								v -> ImageViewActivity.start(ctx, data));
					} else {
						imageContent.setVisibility(GONE);
					}
				} else {
					imageContent.setVisibility(GONE);
				}
			} else {
				imageContent.setVisibility(GONE);
			}
		}

		// post text and map message
		String postTextStr = item.getText();
		String regularText = null;
		String mapPart = null;

		if (postTextStr != null) {
			int mapIndex = postTextStr.indexOf("::map:");
			if (mapIndex >= 0) {
				regularText = postTextStr.substring(0, mapIndex).trim();
				mapPart = postTextStr.substring(mapIndex).trim();
			} else {
				regularText = postTextStr;
			}
		}

		// render regular text
		text.setOnClickListener(null);
		if (regularText != null && !regularText.isEmpty()) {
			Spanned postText = getSpanned(regularText);
			if (fullText) {
				text.setText(postText);
				text.setTextIsSelectable(true);
				makeLinksClickable(text, listener::onLinkClick);
			} else {
				text.setTextIsSelectable(false);
				if (postText.length() > TEASER_LENGTH)
					postText = getTeaser(ctx, postText);
				text.setText(postText);
			}
			text.setVisibility(VISIBLE);
		} else {
			text.setText("");
			text.setVisibility(GONE);
		}

		// render map content
		if (mapPart != null) {
			MapMessageData mapData = parseMapMessage(mapPart);
			mapContent.setText("\uD83D\uDCCD" + mapData.label +
					"\n   " + mapData.latitude +
					"\n   " + mapData.longitude +
					"\n   " + ctx.getString(R.string.tap_to_view_on_map));
			mapContent.setOnClickListener(
					v -> listener.onMapMessageClicked(mapData));
			mapContent.setVisibility(VISIBLE);
		} else {
			mapContent.setText("");
			mapContent.setOnClickListener(null);
			mapContent.setVisibility(GONE);
		}

		// reblog button
		reblogButton.setOnClickListener(v -> {
			Intent i = new Intent(ctx, ReblogActivity.class);
			i.putExtra(GROUP_ID, item.getGroupId().getBytes());
			i.putExtra(POST_ID, item.getId().getBytes());
			ctx.startActivity(i);
		});

		// like button
		if (likeButton != null) {
			if (item.isLikedByMe()) {
				likeButton.setImageResource(R.drawable.ic_heart_filled);
				likeButton.setImageTintList(null);
			} else {
				likeButton.setImageResource(R.drawable.ic_heart_outline);
				likeButton.setImageTintList(
						reblogButton.getImageTintList());
			}
			likeButton.setOnClickListener(
					v -> listener.onLikeClick(item));
		}
		if (likeCountText != null) {
			int count = item.getLikeCount();
			if (count > 0) {
				likeCountText.setText(String.valueOf(count));
				likeCountText.setVisibility(VISIBLE);
			} else {
				likeCountText.setVisibility(GONE);
			}
		}

		// comment button
		if (commentButton != null) {
			commentButton.setOnClickListener(
					v -> listener.onCommentClick(item));
		}

		// comments
		commentContainer.removeAllViews();
		if (isReblog) {
			onBindComment((BlogCommentItem) item, authorClickable);
		} else {
			reblogger.setVisibility(GONE);
			reblogCommentText.setVisibility(GONE);
		}

		if (fullText) {
			// cross-blog comments (from ::comment: entries)
			for (BaseViewModel.BlogComment bc : item.getBlogComments()) {
				View cv = LayoutInflater.from(ctx).inflate(
						R.layout.list_item_blog_comment, commentContainer,
						false);
				AuthorView commentAuthor = cv.findViewById(R.id.authorView);
				TextView commentText = cv.findViewById(R.id.textView);
				commentAuthor.setAuthor(bc.author, bc.authorInfo);
				commentAuthor.setDate(bc.timestamp);
				commentText.setText(bc.text);
				Linkify.addLinks(commentText, Linkify.WEB_URLS);
				commentText.setMovementMethod(null);
				if (fullText) {
					commentText.setTextIsSelectable(true);
					makeLinksClickable(commentText, listener::onLinkClick);
				}
				commentContainer.addView(cv);
			}
		}
	}

	private MapMessageData parseMapMessage(String text) {
		String payload = text.substring(6); // Remove "::map:"
		String[] parts = payload.split(";");
		String label = (parts.length > 0) ? parts[0].trim() : "Unknown";
		String location = (parts.length > 1) ? parts[1].trim() : "";
		String zoom = (parts.length > 2) ? parts[2].trim() : "";

		try {
			String[] latLon = location.split(",");
			String latStr = (latLon.length > 0) ?
					latLon[0].trim().replace(":", "") : "0";
			String lonStr = (latLon.length > 1) ?
					latLon[1].trim().replace(":", "") : "0";
			double lat = Double.parseDouble(latStr);
			double lon = Double.parseDouble(lonStr);
			return new MapMessageData(label, lat, lon, zoom);
		} catch (Exception e) {
			return new MapMessageData(label, 0, 0, zoom);
		}
	}

	private void onBindComment(BlogCommentItem item, boolean authorClickable) {
		// reblogger
		reblogger.setAuthor(item.getAuthor(), item.getAuthorInfo());
		reblogger.setDate(item.getTimestamp());
		if (authorClickable) {
			reblogger.setAuthorClickable(v -> listener.onAuthorClick(item));
		} else {
			reblogger.setAuthorNotClickable();
		}
		reblogger.setVisibility(VISIBLE);
		reblogger.setPersona(REBLOGGER);

		// reblogger's own comment text (shown above the reblogged post)
		String reblogComment = item.getHeader().getComment();
		if (reblogComment != null && !reblogComment.isEmpty()
				&& !BaseViewModel.isSpecialComment(reblogComment)) {
			reblogCommentText.setText(reblogComment);
			reblogCommentText.setVisibility(VISIBLE);
		} else {
			reblogCommentText.setVisibility(GONE);
		}

		author.setPersona(item.getHeader().getRootPost().isRssFeed() ?
				RSS_FEED_REBLOGGED : COMMENTER);

		if (fullText) {
			// comments (skip reblogger's own comment since it's shown above)
			// TODO use nested RecyclerView instead like we do for Image Attachments
			BlogCommentHeader rebloggerHeader = item.getHeader();
			for (BlogCommentHeader c : item.getComments()) {
				if (BaseViewModel.isSpecialComment(c.getComment())) continue;
				if (c == rebloggerHeader) continue;
				View v = LayoutInflater.from(ctx).inflate(
						R.layout.list_item_blog_comment, commentContainer, false);

				AuthorView author = v.findViewById(R.id.authorView);
				TextView text = v.findViewById(R.id.textView);

				author.setAuthor(c.getAuthor(), c.getAuthorInfo());
				author.setDate(c.getTimestamp());
				// TODO make author clickable #624

				text.setText(c.getComment());
				Linkify.addLinks(text, Linkify.WEB_URLS);
				text.setMovementMethod(null);
				if (fullText) {
					text.setTextIsSelectable(true);
					makeLinksClickable(text, listener::onLinkClick);
				}

				commentContainer.addView(v);
			}
		}
	}
}
