package org.anonchatsecure.bramble.plugin.bluetooth;

import org.anonchatsecure.bramble.api.io.TimeoutMonitor;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;

import javax.annotation.concurrent.Immutable;
import javax.microedition.io.StreamConnection;

@Immutable
@NotNullByDefault
class JavaBluetoothConnectionFactory
		implements BluetoothConnectionFactory<StreamConnection> {

	private final BluetoothConnectionLimiter connectionLimiter;
	private final TimeoutMonitor timeoutMonitor;

	JavaBluetoothConnectionFactory(
			BluetoothConnectionLimiter connectionLimiter,
			TimeoutMonitor timeoutMonitor) {
		this.connectionLimiter = connectionLimiter;
		this.timeoutMonitor = timeoutMonitor;
	}

	@Override
	public DuplexTransportConnection wrapSocket(DuplexPlugin plugin,
			StreamConnection socket) throws IOException {
		return new JavaBluetoothTransportConnection(plugin, connectionLimiter,
				timeoutMonitor, socket);
	}
}
