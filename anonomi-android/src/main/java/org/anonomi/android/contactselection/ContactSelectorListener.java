package org.anonomi.android.contactselection;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

import androidx.annotation.UiThread;

@NotNullByDefault
public interface ContactSelectorListener {

	@UiThread
	void contactsSelected(Collection<ContactId> contacts);

}
