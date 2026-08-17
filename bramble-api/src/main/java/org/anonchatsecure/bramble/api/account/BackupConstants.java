package org.anonchatsecure.bramble.api.account;

public interface BackupConstants {

	/**
	 * The magic number at the start of a backup file. The trailing CR, LF,
	 * EOF and LF bytes detect a file that has been mangled by a text-mode
	 * transfer.
	 */
	long BACKUP_MAGIC = 0x41424B310D0A1A0AL;

	/**
	 * The container format version written by the current code.
	 */
	int BACKUP_FORMAT_VERSION = 1;

	/**
	 * Identifier for scrypt with r=8, p=1 and cost 2^logCost.
	 */
	int KDF_SCRYPT = 0;

	/**
	 * The scrypt block size, parameter r. Scrypt needs
	 * 128 * 2^logCost * r bytes.
	 */
	int KDF_BLOCK_SIZE = 8;

	/**
	 * The cost exponent used for new backups. 2^14 needs 16 MiB, and a reader
	 * needs twice that. A backup is restored onto whatever phone is to hand,
	 * which may be smaller than the one that wrote it, so the cost is kept
	 * within reach of a small heap: the recovery code is thirty random digits,
	 * not a memorised secret, so stretching it harder buys very little.
	 */
	int BACKUP_LOG_COST = 14;

	/**
	 * The lowest cost exponent a backup may declare.
	 */
	int MIN_BACKUP_LOG_COST = 14;

	/**
	 * The highest cost exponent a backup may declare. Scrypt needs
	 * 128 * 2^logCost * r bytes, so this bound is 64 MiB; the cost is read
	 * from the file before anything is authenticated, and a phone's heap is
	 * a few hundred MiB.
	 */
	int MAX_BACKUP_LOG_COST = 16;

	/**
	 * The length of the key derivation salt in bytes.
	 */
	int BACKUP_SALT_BYTES = 32;

	/**
	 * The length of the plaintext header in bytes: the magic number, format
	 * version, KDF identifier, cost exponent and salt.
	 */
	int BACKUP_HEADER_BYTES = 8 + 1 + 1 + 1 + BACKUP_SALT_BYTES;

	/**
	 * The offset of the format version within the plaintext header.
	 */
	int BACKUP_FORMAT_VERSION_OFFSET = 8;

	/**
	 * The offset of the KDF identifier within the plaintext header.
	 */
	int BACKUP_KDF_OFFSET = 9;

	/**
	 * The offset of the cost exponent within the plaintext header.
	 */
	int BACKUP_LOG_COST_OFFSET = 10;

	/**
	 * The offset of the salt within the plaintext header.
	 */
	int BACKUP_SALT_OFFSET = 11;

	/**
	 * The maximum length of the manifest in bytes.
	 */
	int MAX_MANIFEST_BYTES = 4096;

	/**
	 * How much of the database is copied between calls to a
	 * {@link BackupProgressListener}.
	 */
	int BACKUP_PROGRESS_INTERVAL_BYTES = 1024 * 1024;

	/**
	 * Label for deriving the header key from the recovery code. The format
	 * version is part of the label, so a backup written by a later format
	 * version cannot be read with a key derived for this one.
	 */
	String BACKUP_HEADER_LABEL =
			"org.anonomi.backup/v" + BACKUP_FORMAT_VERSION + "/HEADER";

	String MANIFEST_FORMAT_VERSION = "formatVersion";

	String MANIFEST_APP_VERSION_CODE = "appVersionCode";

	String MANIFEST_APP_VERSION_NAME = "appVersionName";

	String MANIFEST_SCHEMA_VERSION = "schemaVersion";

	String MANIFEST_CREATED = "created";

	String MANIFEST_DB_KEY = "dbKey";

	String MANIFEST_DB_FILE_NAME = "dbFileName";

	String MANIFEST_DB_LENGTH = "dbLength";

	String MANIFEST_DB_SHA_256 = "dbSha256";

	/**
	 * The manifest key under which a copy of the plaintext header is stored,
	 * so that the reader can check it against the header it parsed.
	 */
	String MANIFEST_HEADER = "header";

	/**
	 * The length of the SHA-256 hash of the database file in bytes.
	 */
	int DB_SHA_256_BYTES = 32;

	/**
	 * The number of digits in a recovery code.
	 */
	int RECOVERY_CODE_DIGITS = 30;

	/**
	 * The number of digits in each group when a recovery code is displayed.
	 */
	int RECOVERY_CODE_GROUP_SIZE = 5;

	/**
	 * The name of the database file inside a backup.
	 */
	String BACKUP_DB_FILE_NAME = "db.mv.db";
}
