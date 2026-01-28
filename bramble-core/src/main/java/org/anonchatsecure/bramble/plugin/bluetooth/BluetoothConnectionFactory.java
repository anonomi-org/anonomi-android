package org.anonchatsecure.bramble.plugin.bluetooth;

import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
interface BluetoothConnectionFactory<S> {

	DuplexTransportConnection wrapSocket(DuplexPlugin plugin, S socket)
			throws IOException;
}
