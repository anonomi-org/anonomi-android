package org.anonomi.android.backup;

import android.app.Application;
import android.net.Uri;

import org.anonchatsecure.bramble.api.account.AccountBackupManager;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.DecryptionException;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.test.ImmediateExecutor;
import org.anonomi.android.backup.BackupViewModel.State;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.SecureRandom;
import java.util.TimeZone;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;

import static org.anonchatsecure.bramble.api.account.BackupConstants.RECOVERY_CODE_DIGITS;
import static org.anonchatsecure.bramble.api.crypto.DecryptionResult.INVALID_PASSWORD;
import static org.anonchatsecure.bramble.api.crypto.DecryptionResult.SUCCESS;
import static org.anonomi.android.viewmodel.LiveEventTestUtil.getOrAwaitValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 24) // Must be >= minSdkVersion or the manifest fails to parse
public class BackupViewModelTest {

	@Rule
	public final InstantTaskExecutorRule testRule =
			new InstantTaskExecutorRule();

	private static final String PASSWORD = "some password";

	@Mock
	private AccountManager accountManager;
	@Mock
	private AccountBackupManager backupManager;
	@Mock
	private CryptoComponent crypto;
	@Mock
	private Clock clock;

	private BackupViewModel viewModel;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		when(crypto.getSecureRandom()).thenReturn(new SecureRandom());
		Application app = ApplicationProvider.getApplicationContext();
		viewModel = new BackupViewModel(app, accountManager, backupManager,
				crypto, new ImmediateExecutor(), clock);
	}

	@Test
	public void testStartsWithoutACode() {
		assertEquals(State.PASSWORD, viewModel.getState().getValue());
		assertNull(viewModel.getRecoveryCode());
	}

	@Test
	public void testCorrectPasswordGeneratesACode() throws Exception {
		viewModel.checkPassword(PASSWORD);

		assertEquals(SUCCESS, getOrAwaitValue(viewModel.getPasswordResult()));
		assertEquals(State.SHOW_CODE, viewModel.getState().getValue());
		String code = viewModel.getRecoveryCode();
		assertNotNull(code);
		assertEquals(RECOVERY_CODE_DIGITS, code.length());
		assertEquals(code, RecoveryCode.normalise(code));
	}

	@Test
	public void testWrongPasswordGeneratesNoCode() throws Exception {
		doThrow(new DecryptionException(INVALID_PASSWORD))
				.when(accountManager).verifyPassword(anyString());

		viewModel.checkPassword(PASSWORD);

		assertEquals(INVALID_PASSWORD,
				getOrAwaitValue(viewModel.getPasswordResult()));
		assertEquals(State.PASSWORD, viewModel.getState().getValue());
		assertNull(viewModel.getRecoveryCode());
	}

	@Test
	public void testCodeIsConfirmedWithOrWithoutTheSpacesItIsShownIn() {
		viewModel.checkPassword(PASSWORD);
		String code = viewModel.getRecoveryCode();
		assertNotNull(code);

		assertTrue(viewModel.isCodeConfirmed(code));
		assertTrue(viewModel.isCodeConfirmed(RecoveryCode.format(code)));
		assertFalse(viewModel.isCodeConfirmed(code.substring(1)));
		assertFalse(viewModel.isCodeConfirmed(""));
	}

	@Test
	public void testNothingIsConfirmedWithoutACode() {
		assertFalse(viewModel.isCodeConfirmed("1".repeat(RECOVERY_CODE_DIGITS)));
	}

	@Test
	public void testRestartThrowsTheCodeAway() {
		viewModel.checkPassword(PASSWORD);
		assertNotNull(viewModel.getRecoveryCode());

		viewModel.restart();

		assertNull(viewModel.getRecoveryCode());
		assertEquals(State.PASSWORD, viewModel.getState().getValue());
	}

	@Test
	public void testFileNameSaysNothingAboutTheApp() {
		// The name carries the day the backup was made in the user's own time
		// zone, so the test has to fix one to know what to expect
		TimeZone previous = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		try {
			// 2026-08-16T12:00:00Z
			when(clock.currentTimeMillis()).thenReturn(1786881600000L);
			assertEquals("backup-20260816.bin", viewModel.getFileName());
		} finally {
			TimeZone.setDefault(previous);
		}
	}

	@Test
	public void testExportWithoutACodeIsRefused() {
		Uri uri = Uri.parse("content://com.android.externalstorage.documents"
				+ "/document/primary%3Abackup-20260816.bin");
		try {
			viewModel.exportTo(uri);
			throw new AssertionError("Expected IllegalStateException");
		} catch (IllegalStateException expected) {
			// The code is the only thing that protects the file, so there is
			// nothing sensible to write without one
		}
	}

	@Test
	public void testOnlyLocalProvidersCountAsStayingOnTheDevice() {
		assertTrue(BackupViewModel.isLocalDestination(Uri.parse(
				"content://com.android.externalstorage.documents/document/"
						+ "primary%3Abackup.bin")));
		assertTrue(BackupViewModel.isLocalDestination(Uri.parse(
				"content://com.android.providers.downloads.documents/document/"
						+ "42")));
		// Anything we do not recognise may put the file online
		assertFalse(BackupViewModel.isLocalDestination(Uri.parse(
				"content://com.google.android.apps.docs.storage/document/1")));
		assertFalse(BackupViewModel.isLocalDestination(
				Uri.parse("file:///sdcard/backup.bin")));
	}
}
