package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.junit.Ignore;

import java.io.File;
import java.sql.Connection;

import javax.annotation.Nonnull;

@Ignore
public class H2DatabaseTraceTest extends DatabaseTraceTest {

	@Override
	Database<Connection> createDatabase(DatabaseConfig databaseConfig,
			MessageFactory messageFactory, Clock clock) {
		return new H2Database(databaseConfig, messageFactory, clock) {
			@Override
			@Nonnull
			String getUrl() {
				return super.getUrl() + ";TRACE_LEVEL_FILE=3";
			}
		};
	}

	@Override
	protected File getTraceFile() {
		return new File(testDir, "db.trace.db");
	}

	@Override
	protected String getTestName() {
		return getClass().getSimpleName();
	}
}
