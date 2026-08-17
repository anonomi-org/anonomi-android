package org.anonomi.android.account;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.anonchatsecure.bramble.api.account.AccountBackupManager;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupError;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.test.ImmediateExecutor;
import org.anonomi.android.account.SetupViewModel.RestoreFailure;
import org.anonomi.android.account.SetupViewModel.RestorePhase;
import org.anonomi.android.account.SetupViewModel.RestoreProgress;
import org.anonomi.android.account.SetupViewModel.State;
import org.briarproject.android.dontkillmelib.DozeHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;

import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 24) // Must be >= minSdkVersion or the manifest fails to parse
public class SetupRestoreViewModelTest {

	@Rule
	public final InstantTaskExecutorRule testRule =
			new InstantTaskExecutorRule();

	private static final String AUTHORITY =
			"com.android.providers.downloads.documents";
	private static final Uri URI =
			Uri.parse("content://" + AUTHORITY + "/document/42");
	private static final String FILE_NAME = "backup-20260817.bin";
	private static final String CODE = "123456789012345678901234567890";
	private static final String PASSWORD = "some password";

	@Mock
	private AccountManager accountManager;
	@Mock
	private AccountBackupManager backupManager;
	@Mock
	private PasswordStrengthEstimator strengthEstimator;
	@Mock
	private DozeHelper dozeHelper;

	private Application app;
	private SetupViewModel viewModel;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		app = ApplicationProvider.getApplicationContext();
		viewModel = new SetupViewModel(app, accountManager, backupManager,
				new ImmediateExecutor(), strengthEstimator, dozeHelper);
	}

	@Test
	public void testStartsByAskingForAName() {
		assertEquals(State.AUTHOR_NAME, viewModel.getState().getLastValue());
		assertFalse(viewModel.isRestoring());
	}

	@Test
	public void testChoosingAFileAsksForTheCode() {
		viewModel.startRestore();
		assertTrue(viewModel.isRestoring());
		assertEquals(State.RESTORE_INTRO, viewModel.getState().getLastValue());

		viewModel.onBackupFileChosen(URI);

		assertEquals(State.RESTORE_CODE, viewModel.getState().getLastValue());
	}

	@Test
	public void testReadingABackupUnpacksItAndReportsProgress()
			throws Exception {
		BackupManifest m = manifest();
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenAnswer(invocation -> {
					BackupProgressListener listener = invocation.getArgument(3);
					listener.onBackupProgress(0, 100);
					listener.onBackupProgress(100, 100);
					return m;
				});

		startRestoreAndRead();

		assertEquals(State.RESTORE_CONFIRM,
				viewModel.getState().getLastValue());
		assertEquals(m, viewModel.getBackupManifest());
		assertEquals(FILE_NAME, viewModel.getBackupName());
		assertNull(viewModel.getRestoreFailure());
		assertFalse(viewModel.getIsReadingBackup().getValue());
		RestoreProgress p = viewModel.getRestoreProgress().getValue();
		assertNotNull(p);
		assertEquals(RestorePhase.UNPACKING, p.phase);
		assertEquals(100, p.done);
		assertEquals(100, p.total);
	}

	@Test
	public void testTheDatabaseIsUnpackedUnderTheFilesDirectory()
			throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenReturn(manifest());

		startRestoreAndRead();

		ArgumentCaptor<File> file = ArgumentCaptor.forClass(File.class);
		verify(backupManager)
				.readBackup(any(), anyString(), file.capture(), any());
		File expected = new File(new File(app.getFilesDir(), "restore"),
				BACKUP_DB_FILE_NAME);
		assertEquals(expected, file.getValue());
	}

	@Test
	public void testAWrongCodeSendsTheUserBackToTheCode() throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenThrow(new InvalidBackupException(
						BackupError.WRONG_CODE_OR_DAMAGED));

		startRestoreAndRead();

		assertEquals(State.RESTORE_FAILED, viewModel.getState().getLastValue());
		assertEquals(RestoreFailure.WRONG_CODE_OR_DAMAGED,
				viewModel.getRestoreFailure());
		assertFalse(viewModel.getIsReadingBackup().getValue());

		viewModel.afterFailure();

		// The file was fine as far as anyone can tell, so it is kept
		assertEquals(State.RESTORE_CODE, viewModel.getState().getLastValue());
		assertNull(viewModel.getRestoreFailure());
	}

	@Test
	public void testADamagedFileSendsTheUserBackToTheFile() throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenThrow(new InvalidBackupException(BackupError.CORRUPT));

		startRestoreAndRead();

		assertEquals(RestoreFailure.DAMAGED, viewModel.getRestoreFailure());

		viewModel.afterFailure();

		assertEquals(State.RESTORE_INTRO, viewModel.getState().getLastValue());
		// Nothing of the file it could not read is kept
		assertNull(viewModel.getBackupManifest());
		assertNull(viewModel.getBackupName());
	}

	@Test
	public void testEveryReasonABackupCanBeRefusedIsExplained()
			throws Exception {
		assertFailureFor(BackupError.NOT_A_BACKUP,
				RestoreFailure.NOT_A_BACKUP);
		assertFailureFor(BackupError.UNSUPPORTED_FORMAT,
				RestoreFailure.TOO_NEW);
		assertFailureFor(BackupError.WRONG_CODE_OR_DAMAGED,
				RestoreFailure.WRONG_CODE_OR_DAMAGED);
		assertFailureFor(BackupError.TRUNCATED, RestoreFailure.TRUNCATED);
		assertFailureFor(BackupError.NOT_ENOUGH_MEMORY,
				RestoreFailure.NOT_ENOUGH_MEMORY);
		assertFailureFor(BackupError.DATA_TOO_NEW, RestoreFailure.TOO_NEW);
		assertFailureFor(BackupError.DATA_TOO_OLD, RestoreFailure.TOO_OLD);
		assertFailureFor(BackupError.CORRUPT, RestoreFailure.DAMAGED);
	}

	@Test
	public void testAFileThatCannotBeReadIsNotABadBackup() throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenThrow(new IOException());

		startRestoreAndRead();

		// Nothing is known about the backup, so it is not blamed
		assertEquals(RestoreFailure.CANNOT_READ_FILE,
				viewModel.getRestoreFailure());
	}

	@Test
	public void testABackupTooBigForThePhoneIsRefusedBeforeReading()
			throws Exception {
		givenTheFileCanBeRead();
		registerProvider(Long.MAX_VALUE);

		startRestoreAndRead();

		assertEquals(RestoreFailure.NOT_ENOUGH_SPACE,
				viewModel.getRestoreFailure());
		verify(backupManager, never())
				.readBackup(any(), anyString(), any(), any());
	}

	@Test
	public void testACodeThatIsNotACodeIsRefused() {
		viewModel.startRestore();
		viewModel.onBackupFileChosen(URI);
		try {
			viewModel.readBackup("not a code");
			throw new AssertionError("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			// Nothing can be derived from it, so there is nothing to try
		}
	}

	@Test
	public void testThePasswordRestoresRatherThanCreates() throws Exception {
		BackupManifest m = manifest();
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenReturn(m);
		when(accountManager.restoreAccount(any(), any(), anyString()))
				.thenReturn(true);
		startRestoreAndRead();
		viewModel.onBackupConfirmed();

		viewModel.setPassword(PASSWORD);

		File expected = new File(new File(app.getFilesDir(), "restore"),
				BACKUP_DB_FILE_NAME);
		verify(accountManager).restoreAccount(expected, m.getDbKey(), PASSWORD);
		verify(accountManager, never()).createAccount(anyString(), anyString());
		assertEquals(State.CREATED, viewModel.getState().getLastValue());
		// The key came in on the manifest, so it is not kept afterwards
		assertNull(viewModel.getBackupManifest());
		assertFalse(viewModel.isRestoring());
	}

	@Test
	public void testAnAccountThatCannotBePutInPlaceIsReported()
			throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenReturn(manifest());
		when(accountManager.restoreAccount(any(), any(), anyString()))
				.thenReturn(false);
		startRestoreAndRead();
		viewModel.onBackupConfirmed();

		viewModel.setPassword(PASSWORD);

		assertEquals(State.RESTORE_FAILED, viewModel.getState().getLastValue());
		assertEquals(RestoreFailure.COULD_NOT_RESTORE,
				viewModel.getRestoreFailure());
		assertFalse(viewModel.getIsCreatingAccount().getValue());
	}

	@Test
	public void testLeavingTheRestoreGoesBackToMakingAnAccount() {
		viewModel.startRestore();

		assertTrue(viewModel.goBack());

		assertFalse(viewModel.isRestoring());
		assertEquals(State.AUTHOR_NAME, viewModel.getState().getLastValue());

		// The password screen must now make an account rather than restore one
		viewModel.setAuthorName("a name");
		viewModel.setPassword(PASSWORD);

		verify(accountManager).createAccount("a name", PASSWORD);
	}

	@Test
	public void testTheRestoreFlowWalksBackwardsOneScreenAtATime()
			throws Exception {
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenReturn(manifest());
		startRestoreAndRead();
		viewModel.onBackupConfirmed();
		assertEquals(State.SET_PASSWORD, viewModel.getState().getLastValue());

		assertTrue(viewModel.goBack());
		assertEquals(State.RESTORE_CONFIRM,
				viewModel.getState().getLastValue());
		assertTrue(viewModel.goBack());
		assertEquals(State.RESTORE_CODE, viewModel.getState().getLastValue());
		assertTrue(viewModel.goBack());
		assertEquals(State.RESTORE_INTRO, viewModel.getState().getLastValue());
		assertTrue(viewModel.goBack());
		assertEquals(State.AUTHOR_NAME, viewModel.getState().getLastValue());
		// Out of the fork, so the activity's own back handling takes over
		assertFalse(viewModel.goBack());
	}

	@Test
	public void testARestoreScreenWithNothingBehindItGoesBackToTheStart() {
		// What a restore fragment finds when the fragment manager brings it
		// back after the process died: no restore, and nothing to act on
		assertFalse(viewModel.isRestoring());

		viewModel.restartSetup();

		assertEquals(State.AUTHOR_NAME, viewModel.getState().getLastValue());
	}

	@Test
	public void testReadingWithoutAFileIsRefused() {
		try {
			viewModel.readBackup(CODE);
			throw new AssertionError("Expected IllegalStateException");
		} catch (IllegalStateException expected) {
			// There is nothing to read, so there is nothing to do but refuse
		}
	}

	@Test
	public void testBackIsLeftAloneWhenMakingANewAccount() {
		// The create path keeps its fragments on the back stack, so the view
		// model must not take back presses away from it
		viewModel.setAuthorName("a name");
		assertFalse(viewModel.goBack());
	}

	private void assertFailureFor(BackupError error, RestoreFailure expected)
			throws Exception {
		setUp();
		givenTheFileCanBeRead();
		when(backupManager.readBackup(any(), anyString(), any(), any()))
				.thenThrow(new InvalidBackupException(error));

		startRestoreAndRead();

		assertEquals(error.name(), expected, viewModel.getRestoreFailure());
	}

	private void startRestoreAndRead() {
		viewModel.startRestore();
		viewModel.onBackupFileChosen(URI);
		viewModel.readBackup(CODE);
	}

	/**
	 * Puts something behind the URI for the resolver to open, and a provider
	 * that will say what it is called and how big it is.
	 */
	private void givenTheFileCanBeRead() {
		ShadowContentResolver shadow =
				Shadows.shadowOf(app.getContentResolver());
		shadow.registerInputStream(URI, new ByteArrayInputStream(new byte[0]));
		registerProvider(1024);
	}

	private void registerProvider(long size) {
		ShadowContentResolver.registerProviderInternal(AUTHORITY,
				new FakeDocumentProvider(size));
	}

	private BackupManifest manifest() {
		return new BackupManifest(BACKUP_FORMAT_VERSION, 1, "1.5.0", 52,
				1786968000000L, new SecretKey(new byte[SecretKey.LENGTH]),
				BACKUP_DB_FILE_NAME, 1024, new byte[32]);
	}

	/**
	 * Answers the two questions {@code DocumentInfo} asks about a document.
	 */
	private static class FakeDocumentProvider extends ContentProvider {

		private final long size;

		private FakeDocumentProvider(long size) {
			this.size = size;
		}

		@Override
		public boolean onCreate() {
			return true;
		}

		@Override
		public Cursor query(Uri uri, @Nullable String[] projection,
				@Nullable String selection, @Nullable String[] selectionArgs,
				@Nullable String sortOrder) {
			MatrixCursor c = new MatrixCursor(new String[] {
					OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
			c.addRow(new Object[] {FILE_NAME, size});
			return c;
		}

		@Nullable
		@Override
		public String getType(Uri uri) {
			return "application/octet-stream";
		}

		@Nullable
		@Override
		public Uri insert(Uri uri, @Nullable ContentValues values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int delete(Uri uri, @Nullable String selection,
				@Nullable String[] selectionArgs) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int update(Uri uri, @Nullable ContentValues values,
				@Nullable String selection, @Nullable String[] selectionArgs) {
			throw new UnsupportedOperationException();
		}
	}
}
