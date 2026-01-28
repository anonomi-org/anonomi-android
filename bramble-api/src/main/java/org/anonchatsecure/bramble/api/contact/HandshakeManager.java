package org.anonchatsecure.bramble.api.contact;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.transport.StreamWriter;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;

@NotNullByDefault
public interface HandshakeManager {

	/**
	 * Handshakes with the given pending contact. Returns an ephemeral master
	 * key authenticated with both parties' handshake key pairs and a flag
	 * indicating whether the local peer is Alice or Bob.
	 *
	 * @param in An incoming stream for the handshake, which must be secured in
	 * handshake mode
	 * @param out An outgoing stream for the handshake, which must be secured
	 * in handshake mode
	 */
	HandshakeResult handshake(PendingContactId p, InputStream in,
			StreamWriter out) throws DbException, IOException;

	class HandshakeResult {

		private final SecretKey masterKey;
		private final boolean alice;

		public HandshakeResult(SecretKey masterKey, boolean alice) {
			this.masterKey = masterKey;
			this.alice = alice;
		}

		public SecretKey getMasterKey() {
			return masterKey;
		}

		public boolean isAlice() {
			return alice;
		}
	}
}
