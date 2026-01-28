package org.anonomi.android.privategroup.reveal;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonomi.android.contactselection.BaseSelectableContactItem;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.privategroup.Visibility;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import static org.anonchatsecure.anonchat.api.privategroup.Visibility.INVISIBLE;

@NotThreadSafe
@NotNullByDefault
class RevealableContactItem extends BaseSelectableContactItem {

	private final Visibility visibility;

	RevealableContactItem(Contact contact, AuthorInfo authorInfo,
			boolean selected, Visibility visibility) {
		super(contact, authorInfo, selected);
		this.visibility = visibility;
	}

	Visibility getVisibility() {
		return visibility;
	}

	@Override
	public boolean isDisabled() {
		return visibility != INVISIBLE;
	}
}
