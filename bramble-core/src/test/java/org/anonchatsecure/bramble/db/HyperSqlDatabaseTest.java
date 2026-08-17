package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.anonchatsecure.bramble.test.TestMessageFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;

import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.isCryptoStrengthUnlimited;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.fail;

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

	@Test
	public void testBackupIsNotSupported() throws Exception {
		File dir = getTestDirectory();
		JdbcDatabase db = createDatabase(new TestDatabaseConfig(dir),
				new TestMessageFactory(), new SystemClock());
		db.open(getSecretKey(), null);
		try {
			Connection txn = db.startTransaction();
			try {
				db.backupTo(txn, new File(dir, "backup.zip"));
				fail();
			} catch (UnsupportedOperationException expected) {
				// Expected
			} finally {
				db.abortTransaction(txn);
			}
		} finally {
			db.close();
			deleteTestDirectory(dir);
		}
	}
}
