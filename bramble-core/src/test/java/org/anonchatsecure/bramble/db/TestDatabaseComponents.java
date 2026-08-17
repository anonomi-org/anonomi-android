package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.NullEventBus;
import org.anonchatsecure.bramble.test.NullShutdownManager;
import org.anonchatsecure.bramble.test.TestMessageFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.sql.Connection;

/**
 * Builds a real database component for tests outside this package, which
 * cannot reach {@link DatabaseComponentImpl} or {@link H2Database}.
 */
@NotNullByDefault
public class TestDatabaseComponents {

	public static DatabaseComponent create(DatabaseConfig config) {
		H2Database db = new H2Database(config, new TestMessageFactory(),
				new SystemClock());
		return new DatabaseComponentImpl<>(db, Connection.class,
				new NullEventBus(), Runnable::run, new NullShutdownManager());
	}
}
