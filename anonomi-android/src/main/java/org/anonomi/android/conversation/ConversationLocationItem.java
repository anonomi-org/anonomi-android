package org.anonomi.android.conversation;

import org.anonchatsecure.anonchat.api.messaging.Location;
import org.anonchatsecure.anonchat.api.messaging.PrivateLocationHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.lifecycle.LiveData;

import static java.util.Collections.emptyList;

/**
 * Extends the message item because a location is shown in the same layout,
 * and the adapter chooses a view holder by layout.
 */
@NotThreadSafe
@NotNullByDefault
class ConversationLocationItem extends ConversationMessageItem {

	private final Location location;

	ConversationLocationItem(@LayoutRes int layoutRes, PrivateLocationHeader h,
			LiveData<String> contactName) {
		super(layoutRes, h, contactName, emptyList());
		this.location = h.getLocation();
	}

	Location getLocation() {
		return location;
	}

}
