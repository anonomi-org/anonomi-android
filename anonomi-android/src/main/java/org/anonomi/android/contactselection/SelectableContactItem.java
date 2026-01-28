package org.anonomi.android.contactselection;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.sharing.SharingManager.SharingStatus;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import static org.anonchatsecure.anonchat.api.sharing.SharingManager.SharingStatus.SHAREABLE;

@NotThreadSafe
@NotNullByDefault
public class SelectableContactItem extends BaseSelectableContactItem {

	private final SharingStatus sharingStatus;

	public SelectableContactItem(Contact contact, AuthorInfo authorInfo,
			boolean selected, SharingStatus sharingStatus) {
		super(contact, authorInfo, selected);
		this.sharingStatus = sharingStatus;
	}

	public SharingStatus getSharingStatus() {
		return sharingStatus;
	}

	@Override
	public boolean isDisabled() {
		return sharingStatus != SHAREABLE;
	}

}
