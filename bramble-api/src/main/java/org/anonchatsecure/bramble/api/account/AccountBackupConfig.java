package org.anonchatsecure.bramble.api.account;

import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;

@NotNullByDefault
public interface AccountBackupConfig {

	/**
	 * A directory for the temporary copy of the database taken during an
	 * export. Its contents are deleted when the export finishes, whether or
	 * not it succeeded.
	 */
	File getBackupTempDirectory();

	/**
	 * The version code of the app writing the backup, recorded in the
	 * manifest so a restore can tell the user where the backup came from.
	 */
	long getAppVersionCode();

	/**
	 * The version name of the app writing the backup.
	 */
	String getAppVersionName();
}
