package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.AccountBackupConfig;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Drives the manager against a stand-in for the database layer: the snapshot
 * is a real archive of the shape H2 writes, so the zip handling, hashing and
 * manifest are exercised end to end.
 */
public class AccountBackupManagerImplTest extends BrambleMockTestCase {

	private static final String CODE = "123456789012345678901234567890";
	private static final long APP_VERSION_CODE = 10600;
	private static final String APP_VERSION_NAME = "1.6.0";
	private static final int DB_BYTES = 20000;

	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final AccountManager accountManager =
			context.mock(AccountManager.class);

	@Inject
	CryptoComponent crypto;
	@Inject
	StreamWriterFactory streamWriterFactory;
	@Inject
	StreamReaderFactory streamReaderFactory;
	@Inject
	BdfWriterFactory bdfWriterFactory;
	@Inject
	BdfReaderFactory bdfReaderFactory;

	private final File testDir = getTestDirectory();
	private final File tempDir = new File(testDir, "cache");
	private final byte[] dbBytes = getRandomBytes(DB_BYTES);
	private final SecretKey dbKey = getSecretKey();
	private final BackupProgressListener progress = (done, total) -> {
	};

	private AccountBackupManagerImpl manager;
	private DatabaseConfig databaseConfig;

	public AccountBackupManagerImplTest() {
		BackupContainerTestComponent component =
				DaggerBackupContainerTestComponent.builder().build();
		component.inject(this);
	}

	@Before
	public void setUp() {
		assertTrue(testDir.mkdirs());
		databaseConfig = new TestDatabaseConfig(testDir);
		// An open account always has one, and the size of the database cannot
		// be measured without it
		assertTrue(databaseConfig.getDatabaseDirectory().mkdirs());
		AccountBackupConfig config = new AccountBackupConfig() {

			@Override
			public File getBackupTempDirectory() {
				return tempDir;
			}

			@Override
			public long getAppVersionCode() {
				return APP_VERSION_CODE;
			}

			@Override
			public String getAppVersionName() {
				return APP_VERSION_NAME;
			}
		};
		manager = new AccountBackupManagerImpl(db, databaseConfig,
				accountManager, config, new SystemClock(), crypto,
				streamWriterFactory, streamReaderFactory, bdfWriterFactory,
				bdfReaderFactory);
	}

	@After
	public void tearDown() {
		deleteTestDirectory(testDir);
	}

	@Test
	public void testExportedAccountCanBeReadBack() throws Exception {
		expectSnapshot();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BackupManifest written = manager.exportAccount(out, CODE, progress);

		assertEquals(BACKUP_FORMAT_VERSION, written.getFormatVersion());
		assertEquals(APP_VERSION_CODE, written.getAppVersionCode());
		assertEquals(APP_VERSION_NAME, written.getAppVersionName());
		assertEquals(CODE_SCHEMA_VERSION, written.getSchemaVersion());
		assertEquals(DB_BYTES, written.getDbLength());
		assertEquals(BACKUP_DB_FILE_NAME, written.getDbFileName());

		File restored = new File(testDir, "restore/db.mv.db");
		BackupManifest read = manager.readBackup(
				new ByteArrayInputStream(out.toByteArray()), CODE, restored,
				progress);
		assertArrayEquals(dbBytes, readFile(restored));
		assertArrayEquals(dbKey.getBytes(), read.getDbKey().getBytes());
		assertArrayEquals(written.getDbSha256(), read.getDbSha256());
		assertEquals(written.getCreated(), read.getCreated());
	}

	@Test
	public void testExportAcceptsACodeTheUserHasRetyped() throws Exception {
		expectSnapshot();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		manager.exportAccount(out, RecoveryCode.format(CODE), progress);
		manager.verifyBackup(new ByteArrayInputStream(out.toByteArray()),
				RecoveryCode.format(CODE), progress);
	}

	@Test
	public void testVerifyBackupKeepsNoCopyOfTheDatabase() throws Exception {
		expectSnapshot();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BackupManifest written = manager.exportAccount(out, CODE, progress);
		BackupManifest verified = manager.verifyBackup(
				new ByteArrayInputStream(out.toByteArray()), CODE, progress);
		assertArrayEquals(written.getDbSha256(), verified.getDbSha256());
		// The snapshot is gone and verifying wrote nothing of its own
		assertEquals(0, countFiles(testDir));
	}

	@Test
	public void testTemporarySnapshotIsDeletedAfterAnExport() throws Exception {
		expectSnapshot();
		manager.exportAccount(new ByteArrayOutputStream(), CODE, progress);
		assertFalse(new File(tempDir, "db.zip").exists());
	}

	@Test
	public void testTemporarySnapshotIsDeletedWhenAnExportFails()
			throws Exception {
		expectSnapshot();
		try {
			manager.exportAccount(new FailingOutputStream(), CODE, progress);
			fail();
		} catch (IOException expected) {
			// Expected
		}
		assertFalse(new File(tempDir, "db.zip").exists());
	}

	@Test
	public void testUnreadableBackupLeavesNoDatabaseBehind() throws Exception {
		expectSnapshot();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		manager.exportAccount(out, CODE, progress);
		byte[] container = out.toByteArray();
		// Cut off the last frame
		byte[] truncated = new byte[container.length - 1];
		System.arraycopy(container, 0, truncated, 0, truncated.length);
		File restored = new File(testDir, "restore/db.mv.db");
		try {
			manager.readBackup(new ByteArrayInputStream(truncated), CODE,
					restored, progress);
			fail();
		} catch (InvalidBackupException expected) {
			// Expected
		}
		assertFalse(restored.exists());
	}

	@Test
	public void testArchiveHoldingMoreThanTheDatabaseIsRefused()
			throws Exception {
		context.checking(new Expectations() {{
			oneOf(accountManager).getDatabaseKey();
			will(returnValue(dbKey));
			oneOf(db).backupDatabase(new File(tempDir, "db.zip"));
			will(new CustomAction("writes an archive with a second entry") {
				@Override
				public Object invoke(Invocation invocation) {
					writeArchive((File) invocation.getParameter(0), true);
					return null;
				}
			});
		}});
		try {
			manager.exportAccount(new ByteArrayOutputStream(), CODE, progress);
			fail();
		} catch (IOException expected) {
			// Expected
		}
	}

	/**
	 * A directory that cannot be listed used to measure as empty, which let the
	 * check pass without having measured anything.
	 */
	@Test
	public void testDatabaseThatCannotBeMeasuredIsRefused() throws Exception {
		deleteTestDirectory(databaseConfig.getDatabaseDirectory());
		context.checking(new Expectations() {{
			oneOf(accountManager).getDatabaseKey();
			will(returnValue(dbKey));
		}});
		try {
			manager.exportAccount(new ByteArrayOutputStream(), CODE, progress);
			fail();
		} catch (IOException expected) {
			// Expected
		}
	}

	@Test
	public void testArchiveHoldingSomethingBeforeTheDatabaseIsRefused()
			throws Exception {
		context.checking(new Expectations() {{
			oneOf(accountManager).getDatabaseKey();
			will(returnValue(dbKey));
			oneOf(db).backupDatabase(new File(tempDir, "db.zip"));
			will(new CustomAction("writes an archive with a first entry") {
				@Override
				public Object invoke(Invocation invocation) {
					writeArchive((File) invocation.getParameter(0), true,
							false);
					return null;
				}
			});
		}});
		// Skipping past it would leave it out of the backup just as silently
		// as a trailing entry would
		try {
			manager.exportAccount(new ByteArrayOutputStream(), CODE, progress);
			fail();
		} catch (IOException expected) {
			// Expected
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testExportNeedsAnUnlockedAccount() throws Exception {
		context.checking(new Expectations() {{
			oneOf(accountManager).getDatabaseKey();
			will(returnValue(null));
		}});
		manager.exportAccount(new ByteArrayOutputStream(), CODE, progress);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExportNeedsARecoveryCode() throws Exception {
		manager.exportAccount(new ByteArrayOutputStream(), "hunter2", progress);
	}

	/**
	 * Stands in for the database layer: writes an archive of the shape
	 * {@code BACKUP TO} produces.
	 */
	private void expectSnapshot() throws Exception {
		context.checking(new Expectations() {{
			oneOf(accountManager).getDatabaseKey();
			will(returnValue(dbKey));
			oneOf(db).backupDatabase(new File(tempDir, "db.zip"));
			will(new CustomAction("writes an archive") {
				@Override
				public Object invoke(Invocation invocation) {
					writeArchive((File) invocation.getParameter(0), false);
					return null;
				}
			});
		}});
	}

	private void writeArchive(File zip, boolean extraEntry) {
		writeArchive(zip, false, extraEntry);
	}

	private void writeArchive(File zip, boolean entryBefore,
			boolean entryAfter) {
		try {
			ZipOutputStream out =
					new ZipOutputStream(new FileOutputStream(zip));
			if (entryBefore) writeExtraEntry(out, "db.blob.db");
			out.putNextEntry(new ZipEntry(BACKUP_DB_FILE_NAME));
			out.write(dbBytes);
			out.closeEntry();
			if (entryAfter) writeExtraEntry(out, "db.lob.db");
			out.close();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private void writeExtraEntry(ZipOutputStream out, String name)
			throws IOException {
		out.putNextEntry(new ZipEntry(name));
		out.write(getRandomBytes(64));
		out.closeEntry();
	}

	private byte[] readFile(File f) throws IOException {
		InputStream in = new FileInputStream(f);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			int read;
			while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
			return out.toByteArray();
		} finally {
			in.close();
		}
	}

	private int countFiles(File dir) {
		File[] children = dir.listFiles();
		if (children == null) return 0;
		int count = 0;
		for (File child : children) {
			count += child.isDirectory() ? countFiles(child) : 1;
		}
		return count;
	}

	private static class FailingOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			throw new IOException();
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			throw new IOException();
		}
	}
}
