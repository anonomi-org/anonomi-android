package org.anonchatsecure.bramble.db;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.db.Metadata;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.Identity;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.bramble.test.NullEventBus;
import org.anonchatsecure.bramble.test.NullShutdownManager;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.anonchatsecure.bramble.test.TestMessageFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Collections.singletonList;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.sync.validation.MessageState.DELIVERED;
import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getClientId;
import static org.anonchatsecure.bramble.test.TestUtils.getGroup;
import static org.anonchatsecure.bramble.test.TestUtils.getIdentity;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests H2's online backup on the URL the app really uses: the database is
 * open and encrypted, and the copy has to be openable with the same key.
 * <p>
 * The copy grows for as long as the database is being written to, so the
 * second test goes through {@link DatabaseComponentImpl}, whose write lock is
 * what keeps it bounded.
 */
public class H2BackupTest extends BrambleTestCase {

	private static final int WRITES_PER_THREAD = 50;
	private static final int WRITER_THREADS = 2;
	private static final long MAX_EXPECTED_ZIP_BYTES = 64 * 1024 * 1024;

	private final SecretKey key = getSecretKey();
	private final File testDir = getTestDirectory();
	private final File sourceDir = new File(testDir, "source");
	private final File restoreDir = new File(testDir, "restore");
	private final File zip = new File(testDir, "backup.zip");

	private final ClientId clientId = getClientId();
	private final Group group = getGroup(clientId, 123);
	private final GroupId groupId = group.getId();
	private final Author author = getAuthor();
	private final Identity identity = getIdentity();
	private final Message message = getMessage(groupId);
	private final MessageId messageId = message.getId();

	@Before
	public void setUp() {
		assertTrue(testDir.mkdirs());
	}

	@After
	public void tearDown() {
		deleteTestDirectory(testDir);
	}

	@Test
	public void testBackupOfOpenDatabaseCanBeRestored() throws Exception {
		Database<Connection> db = openDatabase(sourceDir);
		try {
			Connection txn = db.startTransaction();
			db.addIdentity(txn, identity);
			db.addContact(txn, author, identity.getId(), null, true);
			db.addGroup(txn, group);
			db.addMessage(txn, message, DELIVERED, true, false, null);
			db.commitTransaction(txn);
			// Take the backup the way DatabaseComponentImpl does: in a
			// transaction that does nothing else
			txn = db.startTransaction();
			db.backupTo(txn, zip);
			db.commitTransaction(txn);
		} finally {
			db.close();
		}
		assertTrue(zip.length() > 0);
		assertEquals(singletonList(BACKUP_DB_FILE_NAME), unzip());
		assertRestoredDatabaseHoldsTheData();
	}

	@Test
	public void testBackupUnderWriteLockIsBoundedWithConcurrentWriters()
			throws Exception {
		H2Database h2 = new H2Database(new TestDatabaseConfig(sourceDir),
				new TestMessageFactory(), new SystemClock());
		DatabaseComponent db = new DatabaseComponentImpl<>(h2, Connection.class,
				new NullEventBus(), Runnable::run, new NullShutdownManager());
		AtomicInteger writes = new AtomicInteger(0);
		AtomicReference<Exception> writerFailed = new AtomicReference<>();
		db.open(key, null);
		try {
			db.transaction(false, txn -> {
				db.addIdentity(txn, identity);
				db.addGroup(txn, group);
				db.addLocalMessage(txn, message, new Metadata(), true, false);
			});
			List<Thread> writers = new ArrayList<>();
			for (int i = 0; i < WRITER_THREADS; i++) {
				Thread writer = new Thread(() -> {
					try {
						for (int j = 0; j < WRITES_PER_THREAD; j++) {
							db.transaction(false, txn ->
									db.addLocalMessage(txn, getMessage(groupId),
											new Metadata(), true, false));
							writes.incrementAndGet();
						}
					} catch (Exception e) {
						writerFailed.set(e);
					}
				});
				writers.add(writer);
				writer.start();
			}
			db.backupDatabase(zip);
			for (Thread writer : writers) writer.join();
		} finally {
			db.close();
		}
		assertNull(writerFailed.get());
		assertEquals(WRITER_THREADS * WRITES_PER_THREAD, writes.get());
		assertTrue(zip.length() > 0);
		assertTrue(zip.length() < MAX_EXPECTED_ZIP_BYTES);
		assertEquals(singletonList(BACKUP_DB_FILE_NAME), unzip());
		assertRestoredDatabaseHoldsTheData();
	}

	private Database<Connection> openDatabase(File dir) throws Exception {
		DatabaseConfig config = new TestDatabaseConfig(dir);
		Database<Connection> db = new H2Database(config,
				new TestMessageFactory(), new SystemClock());
		db.open(key, null);
		return db;
	}

	/**
	 * Extracts the archive into the directory a restored account would use
	 * and returns the names of its entries.
	 */
	private List<String> unzip() throws Exception {
		File dir = new TestDatabaseConfig(restoreDir).getDatabaseDirectory();
		assertTrue(dir.mkdirs());
		List<String> names = new ArrayList<>();
		byte[] buf = new byte[4096];
		InputStream in = new FileInputStream(zip);
		try {
			ZipInputStream zin = new ZipInputStream(in);
			ZipEntry e;
			while ((e = zin.getNextEntry()) != null) {
				names.add(e.getName());
				OutputStream out =
						new FileOutputStream(new File(dir, e.getName()));
				try {
					int read;
					while ((read = zin.read(buf)) != -1) out.write(buf, 0, read);
				} finally {
					out.close();
				}
			}
		} finally {
			in.close();
		}
		return names;
	}

	private void assertRestoredDatabaseHoldsTheData() throws Exception {
		Database<Connection> db = openDatabase(restoreDir);
		try {
			Connection txn = db.startTransaction();
			assertTrue(db.containsIdentity(txn, identity.getId()));
			assertTrue(db.containsGroup(txn, groupId));
			assertTrue(db.containsMessage(txn, messageId));
			assertArrayEquals(message.getBody(),
					db.getMessage(txn, messageId).getBody());
			db.commitTransaction(txn);
		} finally {
			db.close();
		}
	}
}
