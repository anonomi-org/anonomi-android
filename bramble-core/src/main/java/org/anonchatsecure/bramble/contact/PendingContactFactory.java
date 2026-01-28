package org.anonchatsecure.bramble.contact;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.UnsupportedVersionException;
import org.anonchatsecure.bramble.api.contact.PendingContact;
import org.anonchatsecure.bramble.api.crypto.PublicKey;

interface PendingContactFactory {

	/**
	 * Creates a {@link PendingContact} from the given handshake link and alias.
	 *
	 * @throws UnsupportedVersionException If the link uses a format version
	 * that is not supported
	 * @throws FormatException If the link is invalid
	 */
	PendingContact createPendingContact(String link, String alias)
			throws FormatException;

	/**
	 * Creates a handshake link from the given public key.
	 */
	String createHandshakeLink(PublicKey k);
}
