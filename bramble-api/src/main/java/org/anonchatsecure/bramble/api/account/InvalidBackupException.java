package org.anonchatsecure.bramble.api.account;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public class InvalidBackupException extends Exception {

	private final BackupError error;

	public InvalidBackupException(BackupError error) {
		this.error = error;
	}

	public BackupError getError() {
		return error;
	}
}
