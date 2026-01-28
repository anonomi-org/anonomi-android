package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;

public class H2DatabaseTest extends JdbcDatabaseTest {

	@Override
	protected JdbcDatabase createDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new H2Database(config, messageFactory, clock);
	}
}
