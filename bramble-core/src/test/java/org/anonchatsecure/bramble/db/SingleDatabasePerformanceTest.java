package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.anonchatsecure.bramble.test.TestMessageFactory;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getMean;
import static org.anonchatsecure.bramble.test.TestUtils.getMedian;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getStandardDeviation;

public abstract class SingleDatabasePerformanceTest
		extends DatabasePerformanceTest {

	abstract Database<Connection> createDatabase(DatabaseConfig databaseConfig,
			MessageFactory messageFactory, Clock clock);

	private SecretKey databaseKey = getSecretKey();

	@Override
	protected void benchmark(String name,
			BenchmarkTask<Database<Connection>> task) throws Exception {
		deleteTestDirectory(testDir);
		Database<Connection> db = openDatabase();
		populateDatabase(db);
		db.close();
		db = openDatabase();
		// Measure the first iteration
		long firstDuration = measureOne(db, task);
		// Measure blocks of iterations until we reach a steady state
		SteadyStateResult result = measureSteadyState(db, task);
		db.close();
		writeResult(name, result.blocks, firstDuration, result.durations);
	}

	private Database<Connection> openDatabase() throws DbException {
		Database<Connection> db = createDatabase(
				new TestDatabaseConfig(testDir), new TestMessageFactory(),
				new SystemClock());
		db.open(databaseKey, null);
		return db;
	}

	private void writeResult(String name, int blocks, long firstDuration,
			List<Double> durations) throws IOException {
		String result = String.format("%s\t%d\t%,d\t%,d\t%,d\t%,d", name,
				blocks, firstDuration, (long) getMean(durations),
				(long) getMedian(durations),
				(long) getStandardDeviation(durations));
		writeResult(result);
	}
}
