package org.anonchatsecure.bramble.transport;

import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface TransportKeyManagerFactory {

	TransportKeyManager createTransportKeyManager(TransportId transportId,
			long maxLatency);

}
