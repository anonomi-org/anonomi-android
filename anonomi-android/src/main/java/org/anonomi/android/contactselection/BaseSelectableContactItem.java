package org.anonomi.android.contactselection;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonomi.android.contact.ContactItem;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@NotNullByDefault
public abstract class BaseSelectableContactItem extends ContactItem {

	private boolean selected;

	public BaseSelectableContactItem(Contact contact, AuthorInfo authorInfo,
			boolean selected) {
		super(contact, authorInfo);
		this.selected = selected;
	}

	boolean isSelected() {
		return selected;
	}

	void toggleSelected() {
		selected = !selected;
	}

	public abstract boolean isDisabled();

}
