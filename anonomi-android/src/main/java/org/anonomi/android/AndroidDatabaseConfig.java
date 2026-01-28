package org.anonomi.android;

import org.anonchatsecure.bramble.api.crypto.KeyStrengthener;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;

import javax.annotation.Nullable;

@NotNullByDefault
class AndroidDatabaseConfig implements DatabaseConfig {

	private final File dbDir, keyDir;
	@Nullable
	private final KeyStrengthener keyStrengthener;

	AndroidDatabaseConfig(File dbDir, File keyDir,
			@Nullable KeyStrengthener keyStrengthener) {
		this.dbDir = dbDir;
		this.keyDir = keyDir;
		this.keyStrengthener = keyStrengthener;
	}

	@Override
	public File getDatabaseDirectory() {
		return dbDir;
	}

	@Override
	public File getDatabaseKeyDirectory() {
		return keyDir;
	}

	@Nullable
	@Override
	public KeyStrengthener getKeyStrengthener() {
		return keyStrengthener;
	}
}
