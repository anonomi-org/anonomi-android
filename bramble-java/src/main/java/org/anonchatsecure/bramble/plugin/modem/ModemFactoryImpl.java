package org.anonchatsecure.bramble.plugin.modem;

import org.anonchatsecure.bramble.api.reliability.ReliabilityLayerFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.system.SystemClock;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class ModemFactoryImpl implements ModemFactory {

	private final Executor ioExecutor;
	private final ReliabilityLayerFactory reliabilityFactory;
	private final Clock clock;

	ModemFactoryImpl(Executor ioExecutor,
			ReliabilityLayerFactory reliabilityFactory) {
		this.ioExecutor = ioExecutor;
		this.reliabilityFactory = reliabilityFactory;
		clock = new SystemClock();
	}

	@Override
	public Modem createModem(Modem.Callback callback, String portName) {
		return new ModemImpl(ioExecutor, reliabilityFactory, clock, callback,
				new SerialPortImpl(portName));
	}
}
