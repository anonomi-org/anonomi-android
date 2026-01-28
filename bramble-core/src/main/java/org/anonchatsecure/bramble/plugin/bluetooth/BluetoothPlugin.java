package org.anonchatsecure.bramble.plugin.bluetooth;

import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
public interface BluetoothPlugin extends DuplexPlugin {

	boolean isDiscovering();

	void disablePolling();

	void enablePolling();

	@Nullable
	DuplexTransportConnection discoverAndConnectForSetup(String uuid);

	void stopDiscoverAndConnect();
}
