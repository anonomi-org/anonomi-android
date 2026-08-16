package org.anonchatsecure.bramble.api.account;

import org.anonchatsecure.bramble.api.db.DbException;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@NotNullByDefault
public interface AccountBackupManager {

	/**
	 * Writes an encrypted backup of the open account to the given stream and
	 * returns the manifest it wrote. The stream is not closed.
	 * <p>
	 * A snapshot of the database is taken first, which blocks writers for as
	 * long as the copy takes.
	 *
	 * @throws IllegalArgumentException if the code is not a recovery code
	 * @throws IllegalStateException if the account is not unlocked
	 * @throws IOException if there is not enough room for the snapshot, as
	 * well as for the usual reasons
	 */
	BackupManifest exportAccount(OutputStream out, String recoveryCode,
			BackupProgressListener listener) throws DbException, IOException;

	/**
	 * Reads a backup from the given stream, writing the database it holds to
	 * the given file, and returns the manifest. The stream is not closed. The
	 * file is deleted if the backup turns out not to be readable.
	 * <p>
	 * The database is still encrypted with the key in the manifest, so the
	 * key never reaches the disk.
	 *
	 * @throws IllegalArgumentException if the code is not a recovery code
	 */
	BackupManifest readBackup(InputStream in, String recoveryCode, File dbFile,
			BackupProgressListener listener)
			throws IOException, InvalidBackupException;

	/**
	 * Reads a backup from the given stream without keeping the database, and
	 * returns the manifest. Used to check that a backup that has just been
	 * written can be read back. The stream is not closed.
	 *
	 * @throws IllegalArgumentException if the code is not a recovery code
	 */
	BackupManifest verifyBackup(InputStream in, String recoveryCode,
			BackupProgressListener listener)
			throws IOException, InvalidBackupException;
}
