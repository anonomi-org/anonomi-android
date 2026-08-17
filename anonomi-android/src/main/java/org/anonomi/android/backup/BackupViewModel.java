package org.anonomi.android.backup;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.anonchatsecure.bramble.api.account.AccountBackupManager;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupError;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.NotEnoughSpaceException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.DecryptionException;
import org.anonchatsecure.bramble.api.crypto.DecryptionResult;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonomi.android.util.DocumentInfo;
import org.anonomi.android.viewmodel.LiveEvent;
import org.anonomi.android.viewmodel.MutableLiveEvent;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.UiThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.Arrays.asList;
import static java.util.Locale.US;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

@NotNullByDefault
public class BackupViewModel extends AndroidViewModel {

	private static final Logger LOG =
			getLogger(BackupViewModel.class.getName());

	/**
	 * The providers whose documents stay on this device. There is no way to
	 * ask a provider whether it syncs somewhere online, so this is an
	 * allowlist rather than a blocklist: an unrecognised destination is
	 * warned about.
	 */
	private static final Set<String> LOCAL_AUTHORITIES =
			Collections.unmodifiableSet(new HashSet<>(asList(
					"com.android.externalstorage.documents",
					"com.android.providers.downloads.documents")));

	enum State {PASSWORD, SHOW_CODE, CONFIRM_CODE, RUNNING, DONE, FAILED}

	/**
	 * Which part of the work is running. The snapshot reports no progress of
	 * its own, so it is shown as indeterminate.
	 */
	enum Phase {SNAPSHOT, WRITING, CHECKING}

	enum Failure {
		/**
		 * There was not enough room for a copy of the database.
		 */
		NOT_ENOUGH_SPACE,
		/**
		 * The backup could not be written where the user chose.
		 */
		WRITE_FAILED,
		/**
		 * The backup was written but could not be read back, so it would not
		 * have restored.
		 */
		UNREADABLE,
		/**
		 * The account was locked or signed out while the backup was running.
		 */
		SIGNED_OUT
	}

	static class Progress {

		final Phase phase;
		final long done, total;

		Progress(Phase phase, long done, long total) {
			this.phase = phase;
			this.done = done;
			this.total = total;
		}
	}

	static class Result {

		final String displayName;
		final boolean onThisDevice;
		/**
		 * Whether the file that could not be used is still where it was
		 * written. Only meaningful for a failure.
		 */
		final boolean fileLeftBehind;

		Result(String displayName, boolean onThisDevice,
				boolean fileLeftBehind) {
			this.displayName = displayName;
			this.onThisDevice = onThisDevice;
			this.fileLeftBehind = fileLeftBehind;
		}
	}

	private final AccountManager accountManager;
	private final AccountBackupManager backupManager;
	private final CryptoComponent crypto;
	private final Executor ioExecutor;
	private final Clock clock;

	private final MutableLiveData<State> state =
			new MutableLiveData<>(State.PASSWORD);
	private final MutableLiveData<Progress> progress = new MutableLiveData<>();
	private final MutableLiveEvent<DecryptionResult> passwordResult =
			new MutableLiveEvent<>();

	@Nullable
	private volatile Result result = null;
	@Nullable
	private volatile Failure failure = null;
	/**
	 * Held in memory for as long as the flow lasts and never written down.
	 * Losing it means starting again, which is the trade this feature makes.
	 */
	@Nullable
	private volatile String recoveryCode = null;

	@Inject
	BackupViewModel(Application app, AccountManager accountManager,
			AccountBackupManager backupManager, CryptoComponent crypto,
			@IoExecutor Executor ioExecutor, Clock clock) {
		super(app);
		this.accountManager = accountManager;
		this.backupManager = backupManager;
		this.crypto = crypto;
		this.ioExecutor = ioExecutor;
		this.clock = clock;
	}

	@Override
	protected void onCleared() {
		recoveryCode = null;
	}

	LiveData<State> getState() {
		return state;
	}

	LiveData<Progress> getProgress() {
		return progress;
	}

	LiveEvent<DecryptionResult> getPasswordResult() {
		return passwordResult;
	}

	@Nullable
	Result getResult() {
		return result;
	}

	@Nullable
	Failure getFailure() {
		return failure;
	}

	/**
	 * Returns the code the flow is using, or null if this view model was
	 * created after it was generated, in which case the flow has to start
	 * again.
	 */
	@Nullable
	String getRecoveryCode() {
		return recoveryCode;
	}

	/**
	 * Checks the account password before generating a code. A backup outlives
	 * the unlocked screen it was made from, so an unlocked phone in someone
	 * else's hands must not be enough to walk away with the account.
	 */
	@UiThread
	void checkPassword(String password) {
		ioExecutor.execute(() -> {
			try {
				accountManager.verifyPassword(password);
				recoveryCode = RecoveryCode.generate(crypto.getSecureRandom());
				state.postValue(State.SHOW_CODE);
				passwordResult.postEvent(DecryptionResult.SUCCESS);
			} catch (DecryptionException e) {
				passwordResult.postEvent(e.getDecryptionResult());
			}
		});
	}

	@UiThread
	void onCodeWrittenDown() {
		state.setValue(State.CONFIRM_CODE);
	}

	@UiThread
	void showCodeAgain() {
		state.setValue(State.SHOW_CODE);
	}

	/**
	 * Returns true if the given text is the code that was generated. Compared
	 * after normalising, so the spaces it is shown in do not have to be typed.
	 */
	boolean isCodeConfirmed(String typed) {
		String code = recoveryCode;
		if (code == null) return false;
		return code.equals(RecoveryCode.normalise(typed));
	}

	/**
	 * The name offered to the file picker. Deliberately says nothing about
	 * which app wrote the file or what is in it.
	 */
	String getFileName() {
		String day = new SimpleDateFormat("yyyyMMdd", US)
				.format(new Date(clock.currentTimeMillis()));
		return "backup-" + day + ".bin";
	}

	/**
	 * Returns true if a document from this provider stays on the device. See
	 * {@link #LOCAL_AUTHORITIES}.
	 */
	static boolean isLocalDestination(Uri uri) {
		return LOCAL_AUTHORITIES.contains(uri.getAuthority());
	}

	@UiThread
	void exportTo(Uri uri) {
		String code = recoveryCode;
		if (code == null) throw new IllegalStateException();
		state.setValue(State.RUNNING);
		progress.setValue(new Progress(Phase.SNAPSHOT, 0, 0));
		ioExecutor.execute(() -> export(uri, code));
	}

	@IoExecutor
	private void export(Uri uri, String code) {
		ContentResolver resolver = getApplication().getContentResolver();
		// No persistable permission is taken. Nothing reaches the backup
		// again after this export: it is the user's to move and the user's to
		// delete. A lasting grant would also record the file's path in the
		// system's own list, outside anything of ours that is encrypted
		//
		// Asked for before anything is written: every outcome names the file,
		// and whether it was empty when we were handed it decides whether it
		// is ours to delete afterwards
		DocumentInfo doc = DocumentInfo.query(resolver, uri);
		try {
			BackupManifest m = write(resolver, uri, code);
			verify(resolver, uri, code, m);
			result = new Result(doc.displayName, isLocalDestination(uri), false);
			record(m.getCreated());
			state.postValue(State.DONE);
		} catch (NotEnoughSpaceException e) {
			logException(LOG, WARNING, e);
			fail(resolver, uri, doc, Failure.NOT_ENOUGH_SPACE);
		} catch (InvalidBackupException e) {
			logException(LOG, WARNING, e);
			fail(resolver, uri, doc, Failure.UNREADABLE);
		} catch (DbException e) {
			// Locking or signing out closes the database under us, which
			// arrives here rather than as the IllegalStateException below
			logException(LOG, WARNING, e);
			fail(resolver, uri, doc, accountManager.hasDatabaseKey() ?
					Failure.WRITE_FAILED : Failure.SIGNED_OUT);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			fail(resolver, uri, doc, Failure.WRITE_FAILED);
		} catch (RuntimeException e) {
			// Caught rather than allowed to escape, so that the unusable file
			// is still taken away. The account being locked under us arrives
			// here as IllegalStateException
			logException(LOG, WARNING, e);
			fail(resolver, uri, doc, accountManager.hasDatabaseKey() ?
					Failure.WRITE_FAILED : Failure.SIGNED_OUT);
		}
	}

	/**
	 * Notes when the backup was written, so settings can say how old it is.
	 * Failing is survivable and the backup is not: the Keystore can refuse
	 * this - after the screen lock has changed, for instance - and a backup
	 * that has been written and read back must not be thrown away because the
	 * note about it could not be stored.
	 */
	@IoExecutor
	private void record(long created) {
		try {
			LastBackup.save(getApplication(), created);
		} catch (RuntimeException e) {
			logException(LOG, WARNING, e);
		}
	}

	@IoExecutor
	private BackupManifest write(ContentResolver resolver, Uri uri, String code)
			throws DbException, IOException {
		BackupProgressListener listener = (done, total) ->
				progress.postValue(new Progress(Phase.WRITING, done, total));
		OutputStream out = openTruncating(resolver, uri);
		if (out == null) throw new IOException("No output stream for " + uri);
		try {
			BackupManifest m = backupManager.exportAccount(out, code, listener);
			out.flush();
			return m;
		} finally {
			out.close();
		}
	}

	/**
	 * Opens the document for writing, asking for it to be truncated first so
	 * that a shorter backup written over a longer one cannot leave the tail
	 * of the old one behind. Not every provider offers that mode, and a
	 * backup written to a file the picker has just created is worth more than
	 * the guarantee, so plain write is accepted as a fallback.
	 */
	@IoExecutor
	@Nullable
	private OutputStream openTruncating(ContentResolver resolver, Uri uri)
			throws IOException {
		try {
			return resolver.openOutputStream(uri, "wt");
		} catch (IOException | IllegalArgumentException |
				UnsupportedOperationException e) {
			logException(LOG, WARNING, e);
			return resolver.openOutputStream(uri, "w");
		}
	}

	/**
	 * Reads the backup back and checks it against what was written. A backup
	 * that cannot be restored is worse than none, and writes to removable
	 * storage can fail without saying so.
	 */
	@IoExecutor
	private void verify(ContentResolver resolver, Uri uri, String code,
			BackupManifest written)
			throws IOException, InvalidBackupException {
		progress.postValue(
				new Progress(Phase.CHECKING, 0, written.getDbLength()));
		BackupProgressListener listener = (done, total) ->
				progress.postValue(new Progress(Phase.CHECKING, done, total));
		BackupManifest read;
		InputStream in = resolver.openInputStream(uri);
		if (in == null) throw new IOException("No input stream for " + uri);
		try {
			read = backupManager.verifyBackup(in, code, listener);
		} finally {
			in.close();
		}
		if (read.getDbLength() != written.getDbLength() ||
				!Arrays.equals(read.getDbSha256(), written.getDbSha256())) {
			throw new InvalidBackupException(BackupError.CORRUPT);
		}
	}

	@IoExecutor
	private void fail(ContentResolver resolver, Uri uri, DocumentInfo doc,
			Failure reason) {
		// A half-written file that looks like a backup is the trap this
		// feature exists to avoid, so take it away - but only if the picker
		// made it for us. One that already held something was chosen rather
		// than created, and it could be the last backup. A provider that will
		// not say how big it is counts as not ours
		boolean leftBehind = true;
		if (doc.size == 0) leftBehind = !deleteQuietly(resolver, uri);
		failure = reason;
		result = new Result(doc.displayName, isLocalDestination(uri),
				leftBehind);
		state.postValue(State.FAILED);
	}

	/**
	 * Removes a document that was created for a backup that is not going to
	 * be written after all, so that choosing a different destination does not
	 * leave an empty file behind at the first one.
	 */
	@UiThread
	void discardDocument(Uri uri) {
		ioExecutor.execute(() -> {
			ContentResolver resolver = getApplication().getContentResolver();
			if (DocumentInfo.query(resolver, uri).size == 0) {
				deleteQuietly(resolver, uri);
			}
		});
	}

	@IoExecutor
	private boolean deleteQuietly(ContentResolver resolver, Uri uri) {
		try {
			return DocumentsContract.deleteDocument(resolver, uri);
		} catch (Exception e) {
			// Nothing else to try; the user is told the file may still be there
			logException(LOG, WARNING, e);
			return false;
		}
	}

	/**
	 * Starts again from the password, throwing away the code. Used when the
	 * flow is resumed after this view model was recreated and the code is
	 * gone.
	 */
	@UiThread
	void restart() {
		recoveryCode = null;
		result = null;
		failure = null;
		state.setValue(State.PASSWORD);
	}
}
