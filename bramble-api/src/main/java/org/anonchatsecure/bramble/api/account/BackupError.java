package org.anonchatsecure.bramble.api.account;

/**
 * The reason a backup could not be read.
 */
public enum BackupError {

	/**
	 * The file does not start with the backup magic number.
	 */
	NOT_A_BACKUP,

	/**
	 * The file declares a format version, key derivation function or cost
	 * parameter this code cannot use.
	 */
	UNSUPPORTED_FORMAT,

	/**
	 * The header key did not authenticate the stream. Either the recovery
	 * code is wrong or the start of the file has been altered; the two are
	 * indistinguishable.
	 */
	WRONG_CODE_OR_DAMAGED,

	/**
	 * The file ends before the end of the backup.
	 */
	TRUNCATED,

	/**
	 * The backup was written by a version of the app whose database schema
	 * this code cannot open.
	 */
	DATA_TOO_NEW,

	/**
	 * The backup holds a database schema too old to be migrated.
	 */
	DATA_TOO_OLD,

	/**
	 * The contents of the file are inconsistent or have been altered after
	 * the header.
	 */
	CORRUPT
}
