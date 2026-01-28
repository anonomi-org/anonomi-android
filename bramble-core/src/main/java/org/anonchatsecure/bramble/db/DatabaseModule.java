package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventExecutor;
import org.anonchatsecure.bramble.api.lifecycle.ShutdownManager;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;

import java.sql.Connection;
import java.util.concurrent.Executor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DatabaseModule {

	@Provides
	@Singleton
	Database<Connection> provideDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new H2Database(config, messageFactory, clock);
	}

	@Provides
	@Singleton
	DatabaseComponent provideDatabaseComponent(Database<Connection> db,
			EventBus eventBus, @EventExecutor Executor eventExecutor,
			ShutdownManager shutdownManager) {
		return new DatabaseComponentImpl<>(db, Connection.class, eventBus,
				eventExecutor, shutdownManager);
	}

	@Provides
	TransactionManager provideTransactionManager(DatabaseComponent db) {
		return db;
	}
}
