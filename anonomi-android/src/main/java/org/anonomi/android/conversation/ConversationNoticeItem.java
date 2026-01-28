package org.anonomi.android.conversation;

import org.anonchatsecure.anonchat.api.conversation.ConversationRequest;
import org.anonchatsecure.anonchat.api.conversation.ConversationResponse;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.lifecycle.LiveData;

@NotThreadSafe
@NotNullByDefault
class ConversationNoticeItem extends ConversationItem {

	@Nullable
	private final String msgText;

	ConversationNoticeItem(@LayoutRes int layoutRes, String text,
			LiveData<String> contactName, ConversationRequest<?> r) {
		super(layoutRes, r, contactName);
		this.text = text;
		this.msgText = r.getText();
	}

	ConversationNoticeItem(@LayoutRes int layoutRes, String text,
			LiveData<String> contactName, ConversationResponse r) {
		super(layoutRes, r, contactName);
		this.text = text;
		this.msgText = null;
	}

	@Nullable
	String getMsgText() {
		return msgText;
	}

}
