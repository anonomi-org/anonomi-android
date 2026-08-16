package org.anonchatsecure.bramble.api.db;

public interface DatabaseSchema {

	/**
	 * The database schema version used by the current code. Data using a
	 * newer schema causes a {@link DataTooNewException}.
	 */
	int CODE_SCHEMA_VERSION = 52;

	/**
	 * The oldest schema version that can be migrated to
	 * {@link #CODE_SCHEMA_VERSION}. Data using an older schema causes a
	 * {@link DataTooOldException}.
	 */
	int MIN_SCHEMA_VERSION = 38;
}
