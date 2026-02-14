package org.anonomi.android.conversation;

import android.content.Context;

import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.attachment.AttachmentItem;
import org.anonchatsecure.anonchat.api.blog.BlogInvitationRequest;
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse;
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageVisitor;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationRequest;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse;
import org.anonchatsecure.anonchat.api.introduction.IntroductionRequest;
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationRequest;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.List;

import javax.annotation.Nullable;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;

import static java.util.Collections.emptyList;
import static org.anonomi.android.conversation.ConversationRequestItem.RequestType.BLOG;
import static org.anonomi.android.conversation.ConversationRequestItem.RequestType.FORUM;
import static org.anonomi.android.conversation.ConversationRequestItem.RequestType.GROUP;
import static org.anonomi.android.conversation.ConversationRequestItem.RequestType.INTRODUCTION;
import static org.anonomi.android.util.UiUtils.getContactDisplayName;

@UiThread
@NotNullByDefault
class ConversationVisitor implements
		ConversationMessageVisitor<ConversationItem> {

	private final Context ctx;
	private final TextCache textCache;
	private final AttachmentCache attachmentCache;
	private final LiveData<String> contactName;

	ConversationVisitor(Context ctx, TextCache textCache,
			AttachmentCache attachmentCache, LiveData<String> contactName) {
		this.ctx = ctx;
		this.textCache = textCache;
		this.attachmentCache = attachmentCache;
		this.contactName = contactName;
	}

	@Override
	public ConversationItem visitPrivateMessageHeader(PrivateMessageHeader h) {
		List<AttachmentItem> attachments = h.getAttachmentHeaders().isEmpty()
				? emptyList()
				: attachmentCache.getAttachmentItems(h);

		boolean isOutgoing = h.isLocal();

		// Choose layout based on content type
		int layoutRes;
		if (!attachments.isEmpty()) {
			String contentType = attachments.get(0).getHeader().getContentType();

			if (contentType.startsWith("audio/")) {
				layoutRes = isOutgoing
						? R.layout.list_item_conversation_msg_audio
						: R.layout.list_item_conversation_msg_audio_in;
			} else if (contentType.startsWith("image/")) {
				boolean hasText = h.hasText();
				layoutRes = isOutgoing
						? (hasText ? R.layout.list_item_conversation_msg_image_text_out : R.layout.list_item_conversation_msg_image_out)
						: (hasText ? R.layout.list_item_conversation_msg_image_text : R.layout.list_item_conversation_msg_image);
			} else {
				layoutRes = isOutgoing
						? R.layout.list_item_conversation_msg_out
						: R.layout.list_item_conversation_msg_in;
			}
		} else {
			// No attachments → text message
			layoutRes = isOutgoing
					? R.layout.list_item_conversation_msg_out
					: R.layout.list_item_conversation_msg_in;
		}

		ConversationMessageItem item =
				new ConversationMessageItem(layoutRes, h, contactName, attachments);

		if (h.hasText()) {
			String text = textCache.getText(h.getId());
			if (text != null) item.setText(text);
		}

		return item;
	}

	@Override
	public ConversationItem visitBlogInvitationRequest(
			BlogInvitationRequest r) {
		if (r.isLocal()) {
			String text = ctx.getString(R.string.blogs_sharing_invitation_sent,
					r.getName(), contactName.getValue());
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text = ctx.getString(
					R.string.blogs_sharing_invitation_received,
					contactName.getValue(), r.getName());
			return new ConversationRequestItem(
					R.layout.list_item_conversation_request, text, contactName,
					BLOG, r);
		}
	}

	@Override
	public ConversationItem visitBlogInvitationResponse(
			BlogInvitationResponse r) {
		if (r.isLocal()) {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.blogs_sharing_response_accepted_sent,
						contactName.getValue());
			} else if (r.isAutoDecline()) {
				text = ctx.getString(
						R.string.blogs_sharing_response_declined_auto,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.blogs_sharing_response_declined_sent,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.blogs_sharing_response_accepted_received,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.blogs_sharing_response_declined_received,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_in, text,
					contactName, r);
		}
	}

	@Override
	public ConversationItem visitForumInvitationRequest(
			ForumInvitationRequest r) {
		if (r.isLocal()) {
			String text = ctx.getString(R.string.forum_invitation_sent,
					r.getName(), contactName.getValue());
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text = ctx.getString(
					R.string.forum_invitation_received,
					contactName.getValue(), r.getName());
			return new ConversationRequestItem(
					R.layout.list_item_conversation_request, text, contactName,
					FORUM, r);
		}
	}

	@Override
	public ConversationItem visitForumInvitationResponse(
			ForumInvitationResponse r) {
		if (r.isLocal()) {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.forum_invitation_response_accepted_sent,
						contactName.getValue());
			} else if (r.isAutoDecline()) {
				text = ctx.getString(
						R.string.forum_invitation_response_declined_auto,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.forum_invitation_response_declined_sent,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.forum_invitation_response_accepted_received,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.forum_invitation_response_declined_received,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_in, text,
					contactName, r);
		}
	}

	@Override
	public ConversationItem visitGroupInvitationRequest(
			GroupInvitationRequest r) {
		if (r.isLocal()) {
			String text = ctx.getString(
					R.string.groups_invitations_invitation_sent,
					contactName.getValue(), r.getName());
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text = ctx.getString(
					R.string.groups_invitations_invitation_received,
					contactName.getValue(), r.getName());
			return new ConversationRequestItem(
					R.layout.list_item_conversation_request, text, contactName,
					GROUP, r);
		}
	}

	@Override
	public ConversationItem visitGroupInvitationResponse(
			GroupInvitationResponse r) {
		if (r.isLocal()) {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.groups_invitations_response_accepted_sent,
						contactName.getValue());
			} else if (r.isAutoDecline()) {
				text = ctx.getString(
						R.string.groups_invitations_response_declined_auto,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.groups_invitations_response_declined_sent,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.groups_invitations_response_accepted_received,
						contactName.getValue());
			} else {
				text = ctx.getString(
						R.string.groups_invitations_response_declined_received,
						contactName.getValue());
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_in, text,
					contactName, r);
		}
	}

	@Override
	public ConversationItem visitIntroductionRequest(IntroductionRequest r) {
		String name = getContactDisplayName(r.getNameable(), r.getAlias());
		if (r.isLocal()) {
			String text = ctx.getString(R.string.introduction_request_sent,
					contactName.getValue(), name);
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text;
			if (r.wasAnswered()) {
				text = ctx.getString(
						R.string.introduction_request_answered_received,
						contactName.getValue(), name);
			} else if (r.isContact()) {
				text = ctx.getString(
						R.string.introduction_request_exists_received,
						contactName.getValue(), name);
			} else {
				text = ctx.getString(R.string.introduction_request_received,
						contactName.getValue(), name);
			}
			return new ConversationRequestItem(
					R.layout.list_item_conversation_request, text, contactName,
					INTRODUCTION, r);
		}
	}

	@Override
	public ConversationItem visitIntroductionResponse(IntroductionResponse r) {
		String introducedAuthor =
				getContactDisplayName(r.getIntroducedAuthor(),
						r.getIntroducedAuthorInfo().getAlias());
		if (r.isLocal()) {
			String text;
			if (r.wasAccepted()) {
				String suffix = r.canSucceed() ? "\n\n" + ctx.getString(
						R.string.introduction_response_accepted_sent_info,
						introducedAuthor) : "";
				text = ctx.getString(
						R.string.introduction_response_accepted_sent,
						introducedAuthor) + suffix;
			} else if (r.isAutoDecline()) {
				text = ctx.getString(
						R.string.introduction_response_declined_auto,
						introducedAuthor);
			} else {
				text = ctx.getString(
						R.string.introduction_response_declined_sent,
						introducedAuthor);
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_out, text,
					contactName, r);
		} else {
			String text;
			if (r.wasAccepted()) {
				text = ctx.getString(
						R.string.introduction_response_accepted_received,
						contactName.getValue(),
						introducedAuthor);
			} else if (r.isIntroducer()) {
				text = ctx.getString(
						R.string.introduction_response_declined_received,
						contactName.getValue(),
						introducedAuthor);
			} else {
				text = ctx.getString(
						R.string.introduction_response_declined_received_by_introducee,
						contactName.getValue(),
						introducedAuthor);
			}
			return new ConversationNoticeItem(
					R.layout.list_item_conversation_notice_in, text,
					contactName, r);
		}
	}

	interface TextCache {
		@Nullable
		String getText(MessageId m);
	}

	interface AttachmentCache {
		List<AttachmentItem> getAttachmentItems(PrivateMessageHeader h);
	}
}
