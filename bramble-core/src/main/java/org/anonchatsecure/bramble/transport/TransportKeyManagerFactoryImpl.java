package org.anonchatsecure.bramble.transport;

import org.anonchatsecure.bramble.api.crypto.TransportCrypto;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.system.TaskScheduler;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class TransportKeyManagerFactoryImpl implements
		TransportKeyManagerFactory {

	private final DatabaseComponent db;
	private final TransportCrypto transportCrypto;
	private final Executor dbExecutor;
	private final TaskScheduler scheduler;
	private final Clock clock;

	@Inject
	TransportKeyManagerFactoryImpl(DatabaseComponent db,
			TransportCrypto transportCrypto,
			@DatabaseExecutor Executor dbExecutor,
			TaskScheduler scheduler,
			Clock clock) {
		this.db = db;
		this.transportCrypto = transportCrypto;
		this.dbExecutor = dbExecutor;
		this.scheduler = scheduler;
		this.clock = clock;
	}

	@Override
	public TransportKeyManager createTransportKeyManager(
			TransportId transportId, long maxLatency) {
		return new TransportKeyManagerImpl(db, transportCrypto, dbExecutor,
				scheduler, clock, transportId, maxLatency);
	}

}
