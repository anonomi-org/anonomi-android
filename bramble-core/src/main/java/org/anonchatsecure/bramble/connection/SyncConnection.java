package org.anonchatsecure.bramble.connection;

import org.anonchatsecure.bramble.api.connection.ConnectionRegistry;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.plugin.TransportConnectionReader;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.properties.TransportPropertyManager;
import org.anonchatsecure.bramble.api.sync.PriorityHandler;
import org.anonchatsecure.bramble.api.sync.SyncSession;
import org.anonchatsecure.bramble.api.sync.SyncSessionFactory;
import org.anonchatsecure.bramble.api.transport.KeyManager;
import org.anonchatsecure.bramble.api.transport.StreamContext;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.briarproject.nullsafety.NullSafety.requireNonNull;

@NotNullByDefault
class SyncConnection extends Connection {

	final SyncSessionFactory syncSessionFactory;
	final TransportPropertyManager transportPropertyManager;

	SyncConnection(KeyManager keyManager, ConnectionRegistry connectionRegistry,
			StreamReaderFactory streamReaderFactory,
			StreamWriterFactory streamWriterFactory,
			SyncSessionFactory syncSessionFactory,
			TransportPropertyManager transportPropertyManager) {
		super(keyManager, connectionRegistry, streamReaderFactory,
				streamWriterFactory);
		this.syncSessionFactory = syncSessionFactory;
		this.transportPropertyManager = transportPropertyManager;
	}

	@Nullable
	StreamContext allocateStreamContext(ContactId contactId,
			TransportId transportId) {
		try {
			return keyManager.getStreamContext(contactId, transportId);
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			return null;
		}
	}

	SyncSession createIncomingSession(StreamContext ctx,
			TransportConnectionReader r, PriorityHandler handler)
			throws IOException {
		InputStream streamReader = streamReaderFactory.createStreamReader(
				r.getInputStream(), ctx);
		ContactId c = requireNonNull(ctx.getContactId());
		return syncSessionFactory
				.createIncomingSession(c, streamReader, handler);
	}
}
