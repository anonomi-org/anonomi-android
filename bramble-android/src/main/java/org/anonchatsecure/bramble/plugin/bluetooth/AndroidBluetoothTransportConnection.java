package org.anonchatsecure.bramble.plugin.bluetooth;

import android.bluetooth.BluetoothSocket;

import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLock;
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.anonchatsecure.bramble.api.io.TimeoutMonitor;
import org.anonchatsecure.bramble.api.plugin.Plugin;
import org.anonchatsecure.bramble.api.plugin.duplex.AbstractDuplexTransportConnection;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.anonchatsecure.bramble.api.plugin.BluetoothConstants.PROP_ADDRESS;
import static org.anonchatsecure.bramble.util.AndroidUtils.isValidBluetoothAddress;

@NotNullByDefault
class AndroidBluetoothTransportConnection
		extends AbstractDuplexTransportConnection {

	private final BluetoothConnectionLimiter connectionLimiter;
	private final BluetoothSocket socket;
	private final InputStream in;
	private final AndroidWakeLock wakeLock;

	AndroidBluetoothTransportConnection(Plugin plugin,
			BluetoothConnectionLimiter connectionLimiter,
			AndroidWakeLockManager wakeLockManager,
			TimeoutMonitor timeoutMonitor,
			BluetoothSocket socket) throws IOException {
		super(plugin);
		this.connectionLimiter = connectionLimiter;
		this.socket = socket;
		InputStream socketIn = socket.getInputStream();
		if (socketIn == null) throw new IOException();
		in = timeoutMonitor.createTimeoutInputStream(socketIn,
				plugin.getMaxIdleTime() * 2L);
		wakeLock = wakeLockManager.createWakeLock("BluetoothConnection");
		wakeLock.acquire();
		String address = socket.getRemoteDevice().getAddress();
		if (isValidBluetoothAddress(address)) remote.put(PROP_ADDRESS, address);
	}

	@Override
	protected InputStream getInputStream() {
		return in;
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		OutputStream socketOut = socket.getOutputStream();
		if (socketOut == null) throw new IOException();
		return socketOut;
	}

	@Override
	protected void closeConnection(boolean exception) throws IOException {
		try {
			socket.close();
			in.close();
		} finally {
			wakeLock.release();
			connectionLimiter.connectionClosed(this);
		}
	}
}
