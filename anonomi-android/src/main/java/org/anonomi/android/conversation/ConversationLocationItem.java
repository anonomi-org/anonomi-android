package org.anonomi.android.conversation;

import org.anonchatsecure.anonchat.api.messaging.Location;
import org.anonchatsecure.anonchat.api.messaging.PrivateLocationHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.lifecycle.LiveData;

@NotThreadSafe
@NotNullByDefault
class ConversationLocationItem extends ConversationItem {

	private final Location location;

	ConversationLocationItem(@LayoutRes int layoutRes, PrivateLocationHeader h,
			LiveData<String> contactName) {
		super(layoutRes, h, contactName);
		this.location = h.getLocation();
	}

	Location getLocation() {
		return location;
	}

}
