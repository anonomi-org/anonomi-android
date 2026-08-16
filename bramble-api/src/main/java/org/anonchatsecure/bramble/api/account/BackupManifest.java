package org.anonchatsecure.bramble.api.account;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * The plaintext description of a backup, stored inside its encrypted stream.
 */
@Immutable
@NotNullByDefault
public class BackupManifest {

	private final int formatVersion, schemaVersion;
	private final long appVersionCode, created, dbLength;
	private final String appVersionName, dbFileName;
	private final SecretKey dbKey;
	private final byte[] dbSha256;

	public BackupManifest(int formatVersion, long appVersionCode,
			String appVersionName, int schemaVersion, long created,
			SecretKey dbKey, String dbFileName, long dbLength,
			byte[] dbSha256) {
		this.formatVersion = formatVersion;
		this.appVersionCode = appVersionCode;
		this.appVersionName = appVersionName;
		this.schemaVersion = schemaVersion;
		this.created = created;
		this.dbKey = dbKey;
		this.dbFileName = dbFileName;
		this.dbLength = dbLength;
		this.dbSha256 = dbSha256;
	}

	public int getFormatVersion() {
		return formatVersion;
	}

	public long getAppVersionCode() {
		return appVersionCode;
	}

	public String getAppVersionName() {
		return appVersionName;
	}

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public long getCreated() {
		return created;
	}

	public SecretKey getDbKey() {
		return dbKey;
	}

	public String getDbFileName() {
		return dbFileName;
	}

	public long getDbLength() {
		return dbLength;
	}

	public byte[] getDbSha256() {
		return dbSha256;
	}
}
