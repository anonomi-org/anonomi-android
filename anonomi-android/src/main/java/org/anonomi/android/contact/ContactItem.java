package org.anonomi.android.contact;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ContactItem {

	private final Contact contact;
	private final AuthorInfo authorInfo;
	private final boolean connected;

	public ContactItem(Contact contact, AuthorInfo authorInfo) {
		this(contact, authorInfo, false);
	}

	public ContactItem(Contact contact, AuthorInfo authorInfo,
			boolean connected) {
		this.contact = contact;
		this.authorInfo = authorInfo;
		this.connected = connected;
	}

	public Contact getContact() {
		return contact;
	}

	public AuthorInfo getAuthorInfo() {
		return authorInfo;
	}

	boolean isConnected() {
		return connected;
	}

}
