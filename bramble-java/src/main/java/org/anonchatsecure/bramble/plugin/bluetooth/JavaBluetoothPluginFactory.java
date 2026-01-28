package org.anonchatsecure.bramble.plugin.bluetooth;

import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.io.TimeoutMonitor;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.plugin.Backoff;
import org.anonchatsecure.bramble.api.plugin.BackoffFactory;
import org.anonchatsecure.bramble.api.plugin.PluginCallback;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.anonchatsecure.bramble.api.system.WakefulIoExecutor;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.SecureRandom;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.microedition.io.StreamConnection;

import static org.anonchatsecure.bramble.api.plugin.BluetoothConstants.ID;

@Immutable
@NotNullByDefault
public class JavaBluetoothPluginFactory implements DuplexPluginFactory {

	private static final int MAX_LATENCY = 30 * 1000; // 30 seconds
	private static final int MAX_IDLE_TIME = 30 * 1000; // 30 seconds
	private static final int MIN_POLLING_INTERVAL = 60 * 1000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 10 * 60 * 1000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor, wakefulIoExecutor;
	private final SecureRandom secureRandom;
	private final EventBus eventBus;
	private final TimeoutMonitor timeoutMonitor;
	private final BackoffFactory backoffFactory;

	@Inject
	public JavaBluetoothPluginFactory(@IoExecutor Executor ioExecutor,
			@WakefulIoExecutor Executor wakefulIoExecutor,
			SecureRandom secureRandom,
			EventBus eventBus,
			TimeoutMonitor timeoutMonitor,
			BackoffFactory backoffFactory) {
		this.ioExecutor = ioExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.secureRandom = secureRandom;
		this.eventBus = eventBus;
		this.timeoutMonitor = timeoutMonitor;
		this.backoffFactory = backoffFactory;
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Override
	public DuplexPlugin createPlugin(PluginCallback callback) {
		BluetoothConnectionLimiter connectionLimiter =
				new BluetoothConnectionLimiterImpl(eventBus);
		BluetoothConnectionFactory<StreamConnection> connectionFactory =
				new JavaBluetoothConnectionFactory(connectionLimiter,
						timeoutMonitor);
		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		JavaBluetoothPlugin plugin = new JavaBluetoothPlugin(connectionLimiter,
				connectionFactory, ioExecutor, wakefulIoExecutor, secureRandom,
				backoff, callback, MAX_LATENCY, MAX_IDLE_TIME);
		eventBus.addListener(plugin);
		return plugin;
	}
}
