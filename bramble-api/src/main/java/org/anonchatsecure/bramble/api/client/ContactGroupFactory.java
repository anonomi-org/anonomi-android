package org.anonchatsecure.bramble.api.client;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.Group;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface ContactGroupFactory {

	/**
	 * Creates a group that is not shared with any contacts.
	 */
	Group createLocalGroup(ClientId clientId, int majorVersion);

	/**
	 * Creates a group for the given client to share with the given contact.
	 */
	Group createContactGroup(ClientId clientId, int majorVersion,
			Contact contact);

	/**
	 * Creates a group for the given client to share between the given authors
	 * identified by their AuthorIds.
	 */
	Group createContactGroup(ClientId clientId, int majorVersion,
			AuthorId authorId1, AuthorId authorId2);

}
