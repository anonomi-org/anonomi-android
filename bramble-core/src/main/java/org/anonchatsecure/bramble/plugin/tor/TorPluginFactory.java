package org.anonchatsecure.bramble.plugin.tor;

import org.anonchatsecure.bramble.api.battery.BatteryManager;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventExecutor;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.network.NetworkManager;
import org.anonchatsecure.bramble.api.plugin.Backoff;
import org.anonchatsecure.bramble.api.plugin.BackoffFactory;
import org.anonchatsecure.bramble.api.plugin.PluginCallback;
import org.anonchatsecure.bramble.api.plugin.TorConstants;
import org.anonchatsecure.bramble.api.plugin.TorControlPort;
import org.anonchatsecure.bramble.api.plugin.TorDirectory;
import org.anonchatsecure.bramble.api.plugin.TorSocksPort;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.system.WakefulIoExecutor;
import org.briarproject.nullsafety.NotNullByDefault;
import org.briarproject.onionwrapper.CircumventionProvider;
import org.briarproject.onionwrapper.LocationUtils;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.net.SocketFactory;

import static java.util.logging.Level.INFO;
import static java.util.logging.Logger.getLogger;

@Immutable
@NotNullByDefault
abstract class TorPluginFactory implements DuplexPluginFactory {

	protected static final Logger LOG =
			getLogger(TorPluginFactory.class.getName());

	protected static final int MAX_LATENCY = 30 * 1000; // 30 seconds
	protected static final int MAX_IDLE_TIME = 30 * 1000; // 30 seconds
	private static final int MIN_POLLING_INTERVAL = 60 * 1000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 10 * 60 * 1000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	protected final Executor ioExecutor, eventExecutor, wakefulIoExecutor;
	protected final NetworkManager networkManager;
	protected final LocationUtils locationUtils;
	protected final EventBus eventBus;
	protected final SocketFactory torSocketFactory;
	protected final BackoffFactory backoffFactory;
	protected final CircumventionProvider circumventionProvider;
	protected final BatteryManager batteryManager;
	protected final Clock clock;
	protected final CryptoComponent crypto;
	protected final File torDirectory;
	protected final int torSocksPort;
	protected final int torControlPort;

	TorPluginFactory(@IoExecutor Executor ioExecutor,
			@EventExecutor Executor eventExecutor,
			@WakefulIoExecutor Executor wakefulIoExecutor,
			NetworkManager networkManager,
			LocationUtils locationUtils,
			EventBus eventBus,
			SocketFactory torSocketFactory,
			BackoffFactory backoffFactory,
			CircumventionProvider circumventionProvider,
			BatteryManager batteryManager,
			Clock clock,
			CryptoComponent crypto,
			@TorDirectory File torDirectory,
			@TorSocksPort int torSocksPort,
			@TorControlPort int torControlPort) {
		this.ioExecutor = ioExecutor;
		this.eventExecutor = eventExecutor;
		this.wakefulIoExecutor = wakefulIoExecutor;
		this.networkManager = networkManager;
		this.locationUtils = locationUtils;
		this.eventBus = eventBus;
		this.torSocketFactory = torSocketFactory;
		this.backoffFactory = backoffFactory;
		this.circumventionProvider = circumventionProvider;
		this.batteryManager = batteryManager;
		this.clock = clock;
		this.crypto = crypto;
		this.torDirectory = torDirectory;
		this.torSocksPort = torSocksPort;
		this.torControlPort = torControlPort;
	}

	@Nullable
	abstract String getArchitectureForTorBinary();

	abstract TorPlugin createPluginInstance(Backoff backoff,
			TorRendezvousCrypto torRendezvousCrypto, PluginCallback callback,
			String architecture);

	@Override
	public TransportId getId() {
		return TorConstants.ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Override
	public DuplexPlugin createPlugin(PluginCallback callback) {
		// Check that we have a Tor binary for this architecture
		String architecture = getArchitectureForTorBinary();
		if (architecture == null) {
			LOG.warning("Tor is not supported on this architecture");
			return null;
		}

		if (LOG.isLoggable(INFO)) {
			LOG.info("The selected architecture for Tor is " + architecture);
		}

		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		TorRendezvousCrypto torRendezvousCrypto =
				new TorRendezvousCryptoImpl(crypto);
		TorPlugin plugin = createPluginInstance(backoff, torRendezvousCrypto,
				callback, architecture);
		eventBus.addListener(plugin);
		return plugin;
	}
}
