package org.anonchatsecure.bramble.plugin.modem;

import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.plugin.PluginCallback;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPlugin;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.anonchatsecure.bramble.api.reliability.ReliabilityLayerFactory;
import org.anonchatsecure.bramble.util.StringUtils;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
public class ModemPluginFactory implements DuplexPluginFactory {

	private static final int MAX_LATENCY = 30 * 1000; // 30 seconds

	private final ModemFactory modemFactory;
	private final SerialPortList serialPortList;

	@Inject
	public ModemPluginFactory(@IoExecutor Executor ioExecutor,
			ReliabilityLayerFactory reliabilityFactory) {
		modemFactory = new ModemFactoryImpl(ioExecutor, reliabilityFactory);
		serialPortList = new SerialPortListImpl();
	}

	@Override
	public TransportId getId() {
		return ModemPlugin.ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Override
	public DuplexPlugin createPlugin(PluginCallback callback) {
		// This plugin is not enabled by default
		String enabled = callback.getSettings().get("enabled");
		if (StringUtils.isNullOrEmpty(enabled)) return null;
		return new ModemPlugin(modemFactory, serialPortList, callback,
				MAX_LATENCY);
	}
}
