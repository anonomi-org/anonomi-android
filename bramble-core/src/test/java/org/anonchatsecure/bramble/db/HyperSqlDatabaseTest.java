package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.junit.Before;

import static org.anonchatsecure.bramble.test.TestUtils.isCryptoStrengthUnlimited;
import static org.junit.Assume.assumeTrue;

public class HyperSqlDatabaseTest extends JdbcDatabaseTest {

	@Before
	public void setUp() {
		assumeTrue(isCryptoStrengthUnlimited());
	}

	@Override
	protected JdbcDatabase createDatabase(DatabaseConfig config,
			MessageFactory messageFactory, Clock clock) {
		return new HyperSqlDatabase(config, messageFactory ,clock);
	}
}
