package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.anonchatsecure.bramble.test.TestMessageFactory;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.util.List;

import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.MIN_SCHEMA_VERSION;
import static org.junit.Assert.assertEquals;

/**
 * Checks the schema versions published to the rest of the app against the
 * migrations that back them, so a new migration can't leave them stale.
 */
public class SchemaVersionTest extends BrambleTestCase {

	@Test
	public void testMigrationsCoverTheSupportedSchemaVersions() {
		JdbcDatabase db = new H2Database(
				new TestDatabaseConfig(new File("test.tmp")),
				new TestMessageFactory(), new SystemClock());
		List<Migration<Connection>> migrations = db.getMigrations();
		int version = MIN_SCHEMA_VERSION;
		for (Migration<Connection> m : migrations) {
			assertEquals(version, m.getStartVersion());
			version = m.getEndVersion();
		}
		assertEquals(CODE_SCHEMA_VERSION, version);
	}
}
