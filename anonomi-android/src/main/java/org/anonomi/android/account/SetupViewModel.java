package org.anonomi.android.account;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;

import org.briarproject.android.dontkillmelib.DozeHelper;
import org.anonchatsecure.bramble.api.account.AccountBackupManager;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupError;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.util.IoUtils;
import org.anonomi.android.util.DocumentInfo;
import org.anonomi.android.viewmodel.LiveEvent;
import org.anonomi.android.viewmodel.MutableLiveEvent;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.lang.Boolean.TRUE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.android.account.SetupViewModel.RestorePhase.OPENING;
import static org.anonomi.android.account.SetupViewModel.RestorePhase.UNPACKING;
import static org.anonomi.android.account.SetupViewModel.State.AUTHOR_NAME;
import static org.anonomi.android.account.SetupViewModel.State.CREATED;
import static org.anonomi.android.account.SetupViewModel.State.DOZE;
import static org.anonomi.android.account.SetupViewModel.State.FAILED;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_CODE;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_CONFIRM;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_FAILED;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_INTRO;
import static org.anonomi.android.account.SetupViewModel.State.SET_PASSWORD;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class SetupViewModel extends AndroidViewModel {

	/**
	 * The screens of setup. The restore fork runs from
	 * {@link State#RESTORE_INTRO} and rejoins at {@link State#SET_PASSWORD}.
	 */
	enum State {
		AUTHOR_NAME, SET_PASSWORD, DOZE, CREATED, FAILED,
		RESTORE_INTRO, RESTORE_CODE, RESTORE_CONFIRM, RESTORE_FAILED
	}

	/**
	 * Why an account was not restored. Mostly a {@link BackupError} in terms
	 * of what the user can do about it, plus the ways the file or this phone
	 * can be the problem rather than the backup.
	 */
	enum RestoreFailure {
		NOT_A_BACKUP, WRONG_CODE_OR_DAMAGED, TRUNCATED, DAMAGED, TOO_NEW,
		UNSUPPORTED_FORMAT, TOO_OLD, NOT_ENOUGH_MEMORY, NOT_ENOUGH_SPACE,
		CANNOT_READ_FILE, COULD_NOT_RESTORE
	}

	/**
	 * Which part of reading the backup is running. Deriving the key from the
	 * recovery code takes over a second on a low-end phone and reports
	 * nothing, so it is shown as indeterminate.
	 */
	enum RestorePhase {OPENING, UNPACKING}

	static class RestoreProgress {

		final RestorePhase phase;
		final long done, total;

		RestoreProgress(RestorePhase phase, long done, long total) {
			this.phase = phase;
			this.done = done;
			this.total = total;
		}
	}

	private static final Logger LOG = getLogger(SetupActivity.class.getName());

	/**
	 * Where the database inside a backup is unpacked to, under the files
	 * directory so that deleting the account takes it away.
	 */
	private static final String RESTORE_DIR_NAME = "restore";

	@Nullable
	private String authorName, password;

	@Nullable
	private String verificationId;  // <-- Added field for verification ID

	private final MutableLiveEvent<State> state = new MutableLiveEvent<>();
	private final MutableLiveData<Boolean> isCreatingAccount = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> isReadingBackup =
			new MutableLiveData<>(false);
	private final MutableLiveData<RestoreProgress> restoreProgress =
			new MutableLiveData<>();

	private final AccountManager accountManager;
	private final AccountBackupManager backupManager;
	private final Executor ioExecutor;
	private final PasswordStrengthEstimator strengthEstimator;
	private final DozeHelper dozeHelper;

	/**
	 * Whether the flow is putting an old account back rather than making a
	 * new one. The password and doze screens are shared, so this is what
	 * decides what the last step does.
	 */
	private volatile boolean restoring = false;
	@Nullable
	private volatile Uri backupUri = null;
	/**
	 * What was read out of the backup, held between unpacking it and putting
	 * the account in place. It carries the database key, so it is dropped as
	 * soon as the account is restored.
	 */
	@Nullable
	private volatile BackupManifest manifest = null;
	@Nullable
	private volatile String backupName = null;
	@Nullable
	private volatile RestoreFailure restoreFailure = null;

	@Inject
	SetupViewModel(Application app,
			AccountManager accountManager,
			AccountBackupManager backupManager,
			@IoExecutor Executor ioExecutor,
			PasswordStrengthEstimator strengthEstimator,
			DozeHelper dozeHelper) {
		super(app);
		this.accountManager = accountManager;
		this.backupManager = backupManager;
		this.ioExecutor = ioExecutor;
		this.strengthEstimator = strengthEstimator;
		this.dozeHelper = dozeHelper;

		ioExecutor.execute(() -> {
			if (accountManager.accountExists()) {
				throw new AssertionError();
			} else {
				state.postEvent(AUTHOR_NAME);
			}
		});
	}

	@Override
	protected void onCleared() {
		// The unpacked database is left to one of the paths that removes it:
		// the start of the next read, leaving the restore flow, the end of a
		// restore, or the sweep the next launch runs when there is no
		// account. Deleting it here could race a restore that is still moving
		// it into place
		manifest = null;
		password = null;
	}

	LiveEvent<State> getState() {
		return state;
	}

	LiveData<Boolean> getIsCreatingAccount() {
		return isCreatingAccount;
	}

	void setAuthorName(String authorName) {
		// Making a new account gives up a restore that was started and left
		discardRestore();
		this.authorName = authorName;
		state.setEvent(SET_PASSWORD);
	}

	void setPassword(String password) {
		if (!restoring && authorName == null) throw new IllegalStateException();
		this.password = password;
		if (needToShowDozeFragment()) {
			state.setEvent(DOZE);
		} else {
			createOrRestoreAccount();
		}
	}

	float estimatePasswordStrength(String password) {
		return strengthEstimator.estimateStrength(password);
	}

	boolean needToShowDozeFragment() {
		return dozeHelper.needToShowDoNotKillMeFragment(getApplication());
	}

	void dozeExceptionConfirmed() {
		createOrRestoreAccount();
	}

	private void createOrRestoreAccount() {
		if (restoring) restoreAccount();
		else createAccount();
	}

	private void createAccount() {
		if (authorName == null) throw new IllegalStateException();
		if (password == null) throw new IllegalStateException();
		isCreatingAccount.setValue(true);
		ioExecutor.execute(() -> {
			if (accountManager.createAccount(authorName, password)) {
				// LOG.info("Created account");
				state.postEvent(CREATED);
			} else {
				// LOG.warning("Failed to create account");
				state.postEvent(FAILED);
			}
		});
	}

	// --- Restoring an account from a backup ---

	boolean isRestoring() {
		return restoring;
	}

	/**
	 * Whether work is running that the user should not be able to walk away
	 * from halfway.
	 */
	boolean isBusy() {
		return TRUE.equals(isReadingBackup.getValue()) ||
				TRUE.equals(isCreatingAccount.getValue());
	}

	LiveData<Boolean> getIsReadingBackup() {
		return isReadingBackup;
	}

	LiveData<RestoreProgress> getRestoreProgress() {
		return restoreProgress;
	}

	@Nullable
	RestoreFailure getRestoreFailure() {
		return restoreFailure;
	}

	/**
	 * What was read out of the backup, or null if none has been read yet.
	 */
	@Nullable
	BackupManifest getBackupManifest() {
		return manifest;
	}

	/**
	 * The name of the file the backup was read from, or null if none has been
	 * read yet.
	 */
	@Nullable
	String getBackupName() {
		return backupName;
	}

	@UiThread
	void startRestore() {
		clearRestore();
		restoring = true;
		state.setEvent(RESTORE_INTRO);
	}

	/**
	 * Goes back to the first screen, for a restore screen that the fragment
	 * manager brought back after the process was killed. Nothing of the
	 * restore is left in memory, so there is nothing for that screen to act
	 * on. See {@link RestoreFragment}.
	 */
	@UiThread
	void restartSetup() {
		state.setEvent(AUTHOR_NAME);
	}

	@UiThread
	void onBackupFileChosen(Uri uri) {
		backupUri = uri;
		// Whatever an earlier file said is not about this one
		manifest = null;
		backupName = null;
		state.setEvent(RESTORE_CODE);
	}

	@UiThread
	void onBackupConfirmed() {
		if (manifest == null) throw new IllegalStateException();
		state.setEvent(SET_PASSWORD);
	}

	/**
	 * Goes back a screen within the restore flow, since none of it is on the
	 * fragment back stack. Returns false if there was nothing to go back to.
	 */
	@UiThread
	boolean goBack() {
		if (!restoring) return false;
		// Swallowed rather than refused: leaving in the middle of unpacking a
		// backup or putting an account in place is not a way out
		if (isBusy()) return true;
		State current = state.getLastValue();
		if (current == RESTORE_INTRO) {
			discardRestore();
			state.setEvent(AUTHOR_NAME);
		} else if (current == RESTORE_CODE) {
			state.setEvent(RESTORE_INTRO);
		} else if (current == RESTORE_CONFIRM) {
			state.setEvent(RESTORE_CODE);
		} else if (current == SET_PASSWORD) {
			state.setEvent(RESTORE_CONFIRM);
		} else if (current == DOZE) {
			state.setEvent(SET_PASSWORD);
		} else if (current == RESTORE_FAILED) {
			afterFailure();
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Leaves the failure screen by the one way out that makes sense for the
	 * failure: a code that did not open the file can be typed again, and
	 * anything else is a reason to go back to the file.
	 */
	@UiThread
	void afterFailure() {
		RestoreFailure failure = restoreFailure;
		restoreFailure = null;
		if (failure == RestoreFailure.WRONG_CODE_OR_DAMAGED &&
				backupUri != null) {
			state.setEvent(RESTORE_CODE);
		} else {
			backupUri = null;
			manifest = null;
			backupName = null;
			state.setEvent(RESTORE_INTRO);
		}
	}

	/**
	 * Unpacks the chosen backup, so that a file or a code that will not do
	 * turns up before the user has chosen a password for it.
	 *
	 * @throws IllegalArgumentException if the code is not a recovery code
	 */
	@UiThread
	void readBackup(String typedCode) {
		Uri uri = backupUri;
		if (uri == null) throw new IllegalStateException();
		String code = RecoveryCode.normalise(typedCode);
		if (code == null) throw new IllegalArgumentException();
		restoreProgress.setValue(new RestoreProgress(OPENING, 0, 0));
		isReadingBackup.setValue(true);
		ioExecutor.execute(() -> read(uri, code));
	}

	@IoExecutor
	private void read(Uri uri, String code) {
		ContentResolver resolver = getApplication().getContentResolver();
		File dir = getRestoreDirectory();
		// Anything an earlier attempt left. Done on the way in rather than on
		// the way out, so that it cannot race the write below
		IoUtils.deleteFileOrDir(dir);
		File dbFile = new File(dir, BACKUP_DB_FILE_NAME);
		try {
			DocumentInfo info = DocumentInfo.query(resolver, uri);
			// The account inside is a little smaller than the file that holds
			// it, so the file's own size is a safe thing to ask room for. A
			// provider that will not say leaves this to fail as it happens
			if (info.size > 0 && getApplication().getFilesDir()
					.getUsableSpace() < info.size) {
				fail(RestoreFailure.NOT_ENOUGH_SPACE);
				return;
			}
			BackupProgressListener listener = (done, total) ->
					restoreProgress.postValue(
							new RestoreProgress(UNPACKING, done, total));
			BackupManifest m;
			InputStream in = resolver.openInputStream(uri);
			if (in == null) throw new IOException("No input stream for " + uri);
			try {
				m = backupManager.readBackup(in, code, dbFile, listener);
			} finally {
				in.close();
			}
			manifest = m;
			backupName = info.displayName;
			isReadingBackup.postValue(false);
			state.postEvent(RESTORE_CONFIRM);
		} catch (InvalidBackupException e) {
			logException(LOG, WARNING, e);
			fail(failureFor(e.getError()));
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			fail(RestoreFailure.CANNOT_READ_FILE);
		} catch (RuntimeException e) {
			// A provider that will not hand the file over arrives here as
			// SecurityException
			logException(LOG, WARNING, e);
			fail(RestoreFailure.CANNOT_READ_FILE);
		}
	}

	private void restoreAccount() {
		BackupManifest m = manifest;
		String password = this.password;
		if (m == null || password == null) throw new IllegalStateException();
		File dbFile = new File(getRestoreDirectory(), BACKUP_DB_FILE_NAME);
		isCreatingAccount.setValue(true);
		ioExecutor.execute(() -> {
			boolean restored;
			try {
				restored = accountManager
						.restoreAccount(dbFile, m.getDbKey(), password);
			} catch (RuntimeException e) {
				logException(LOG, WARNING, e);
				restored = false;
			}
			// Either the database has been moved into place or the account
			// manager has rolled back; nothing here is wanted afterwards
			IoUtils.deleteFileOrDir(getRestoreDirectory());
			if (restored) {
				clearRestore();
				state.postEvent(CREATED);
			} else {
				fail(RestoreFailure.COULD_NOT_RESTORE);
			}
		});
	}

	private void fail(RestoreFailure failure) {
		restoreFailure = failure;
		isReadingBackup.postValue(false);
		isCreatingAccount.postValue(false);
		state.postEvent(RESTORE_FAILED);
	}

	private static RestoreFailure failureFor(BackupError error) {
		switch (error) {
			case NOT_A_BACKUP:
				return RestoreFailure.NOT_A_BACKUP;
			case WRONG_CODE_OR_DAMAGED:
				return RestoreFailure.WRONG_CODE_OR_DAMAGED;
			case TRUNCATED:
				return RestoreFailure.TRUNCATED;
			case NOT_ENOUGH_MEMORY:
				return RestoreFailure.NOT_ENOUGH_MEMORY;
			case DATA_TOO_OLD:
				return RestoreFailure.TOO_OLD;
			// A format, key derivation function or cost this code does not
			// know is what a newer version's backup looks like, and also what
			// damage to those few bytes looks like: they are read before
			// anything has been authenticated
			case UNSUPPORTED_FORMAT:
				return RestoreFailure.UNSUPPORTED_FORMAT;
			// The schema version comes out of the authenticated manifest, so
			// this one really was written by a newer version
			case DATA_TOO_NEW:
				return RestoreFailure.TOO_NEW;
			// Anything else, including a reason added later, is a file that
			// cannot be used and nothing the user can act on
			default:
				return RestoreFailure.DAMAGED;
		}
	}

	/**
	 * Forgets the restore without touching the unpacked database, which is
	 * either about to be replaced or has already been moved into place.
	 */
	private void clearRestore() {
		restoring = false;
		backupUri = null;
		manifest = null;
		backupName = null;
		restoreFailure = null;
	}

	private void discardRestore() {
		if (!restoring) return;
		clearRestore();
		File dir = getRestoreDirectory();
		ioExecutor.execute(() -> IoUtils.deleteFileOrDir(dir));
	}

	private File getRestoreDirectory() {
		return new File(getApplication().getFilesDir(), RESTORE_DIR_NAME);
	}

	// --- Added getter and setter for verification ID ---

	@Nullable
	String getVerificationId() {
		return verificationId;
	}

	void setVerificationId(String id) {
		this.verificationId = id;
	}
}
