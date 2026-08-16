package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.AccountBackupConfig;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.db.Metadata;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.Identity;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.anonchatsecure.bramble.db.TestDatabaseComponents;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.bramble.test.TestDatabaseConfig;
import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.account.BackupError.WRONG_CODE_OR_DAMAGED;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Backs up a real database and restores it into an account that has never
 * seen it: the snapshot, the container and the restore all have to agree for
 * the messages to come back.
 */
public class AccountBackupIntegrationTest extends BrambleMockTestCase {

	private static final String CODE = "123456789012345678901234567890";
	private static final String WRONG_CODE = "999999999999999999999999999999";
	private static final String PASSWORD = "not the recovery code";

	private final AccountManager sourceAccountManager =
			context.mock(AccountManager.class, "sourceAccountManager");
	private final IdentityManager identityManager =
			context.mock(IdentityManager.class);

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

	private final SecretKey dbKey = getSecretKey();
	private final File testDir = getTestDirectory();
	private final File sourceDir = new File(testDir, "source");
	private final File restoreDir = new File(testDir, "restore");
	private final File tempDir = new File(testDir, "cache");
	private final BackupProgressListener progress = (done, total) -> {
	};

	private final Group group = getGroup(getClientId(), 123);
	private final GroupId groupId = group.getId();
	private final Author author = getAuthor();
	private final Identity identity = getIdentity();
	private final Message message = getMessage(groupId);
	private final MessageId messageId = message.getId();

	public AccountBackupIntegrationTest() {
		BackupContainerTestComponent component =
				DaggerBackupContainerTestComponent.builder().build();
		component.inject(this);
	}

	@Before
	public void setUp() {
		assertTrue(testDir.mkdirs());
		context.checking(new Expectations() {{
			allowing(sourceAccountManager).getDatabaseKey();
			will(returnValue(dbKey));
		}});
	}

	@After
	public void tearDown() {
		deleteTestDirectory(testDir);
	}

	@Test
	public void testAccountSurvivesABackupAndRestore() throws Exception {
		byte[] container = backUpAnAccountWithOneMessage();

		DatabaseConfig restoredConfig = new TestDatabaseConfig(restoreDir);
		File restoredDb = new File(testDir, "incoming.mv.db");
		BackupManifest m = createManager(restoredConfig).readBackup(
				new ByteArrayInputStream(container), CODE, restoredDb,
				progress);
		assertEquals(CODE_SCHEMA_VERSION, m.getSchemaVersion());
		assertArrayEquals(dbKey.getBytes(), m.getDbKey().getBytes());

		// The restored database is still encrypted, so the account is only
		// usable with the key that came out of the backup
		AccountManagerImpl accountManager = new AccountManagerImpl(
				restoredConfig, crypto, identityManager);
		assertTrue(accountManager.restoreAccount(restoredDb, m.getDbKey(),
				PASSWORD));
		accountManager.signIn(PASSWORD);
		SecretKey signedInKey = accountManager.getDatabaseKey();
		assertArrayEquals(dbKey.getBytes(), signedInKey.getBytes());

		DatabaseComponent db = TestDatabaseComponents.create(restoredConfig);
		db.open(signedInKey, null);
		try {
			db.transaction(true, txn -> {
				assertTrue(db.containsIdentity(txn, identity.getId()));
				assertTrue(db.containsGroup(txn, groupId));
				assertArrayEquals(message.getBody(),
						db.getMessage(txn, messageId).getBody());
			});
		} finally {
			db.close();
		}
	}

	@Test
	public void testBackupOfARealDatabaseNeedsTheRightCode() throws Exception {
		byte[] container = backUpAnAccountWithOneMessage();
		try {
			createManager(new TestDatabaseConfig(restoreDir)).verifyBackup(
					new ByteArrayInputStream(container), WRONG_CODE, progress);
			fail();
		} catch (InvalidBackupException e) {
			assertEquals(WRONG_CODE_OR_DAMAGED, e.getError());
		}
	}

	private byte[] backUpAnAccountWithOneMessage() throws Exception {
		DatabaseConfig config = new TestDatabaseConfig(sourceDir);
		DatabaseComponent db = TestDatabaseComponents.create(config);
		db.open(dbKey, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			db.transaction(false, txn -> {
				db.addIdentity(txn, identity);
				db.addContact(txn, author, identity.getId(), null, true);
				db.addGroup(txn, group);
				db.addLocalMessage(txn, message, new Metadata(), true, false);
			});
			BackupManifest m = createManager(db, config)
					.exportAccount(out, CODE, progress);
			assertEquals(CODE_SCHEMA_VERSION, m.getSchemaVersion());
			assertTrue(m.getDbLength() > 0);
		} finally {
			db.close();
		}
		return out.toByteArray();
	}

	private AccountBackupManagerImpl createManager(DatabaseConfig config) {
		return createManager(null, config);
	}

	private AccountBackupManagerImpl createManager(DatabaseComponent db,
			DatabaseConfig config) {
		AccountBackupConfig backupConfig = new AccountBackupConfig() {

			@Override
			public File getBackupTempDirectory() {
				return tempDir;
			}

			@Override
			public long getAppVersionCode() {
				return 10600;
			}

			@Override
			public String getAppVersionName() {
				return "1.6.0";
			}
		};
		return new AccountBackupManagerImpl(db, config, sourceAccountManager,
				backupConfig, new SystemClock(), crypto, streamWriterFactory,
				streamReaderFactory, bdfWriterFactory, bdfReaderFactory);
	}
}
