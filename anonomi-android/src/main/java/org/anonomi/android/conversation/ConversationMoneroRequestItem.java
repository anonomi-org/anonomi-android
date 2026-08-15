package org.anonomi.android.conversation;

import org.anonchatsecure.anonchat.api.messaging.MoneroRequest;
import org.anonchatsecure.anonchat.api.messaging.PrivateMoneroRequestHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.lifecycle.LiveData;

import static java.util.Collections.emptyList;

/**
 * Extends the message item because a payment request is shown in the same
 * layout, and the adapter chooses a view holder by layout.
 */
@NotThreadSafe
@NotNullByDefault
class ConversationMoneroRequestItem extends ConversationMessageItem {

	private final MoneroRequest request;

	ConversationMoneroRequestItem(@LayoutRes int layoutRes,
			PrivateMoneroRequestHeader h, LiveData<String> contactName) {
		super(layoutRes, h, contactName, emptyList());
		this.request = h.getRequest();
	}

	MoneroRequest getRequest() {
		return request;
	}

}
