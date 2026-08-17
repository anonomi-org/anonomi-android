package org.anonchatsecure.bramble.api.account;

import org.briarproject.nullsafety.NotNullByDefault;

/**
 * Reports how much of the database has been copied. Called on the thread
 * doing the work, often enough to drive a progress bar and rarely enough not
 * to flood it.
 */
@NotNullByDefault
public interface BackupProgressListener {

	void onBackupProgress(long done, long total);
}
