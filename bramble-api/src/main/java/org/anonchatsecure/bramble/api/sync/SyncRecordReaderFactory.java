package org.anonchatsecure.bramble.api.sync;

import org.briarproject.nullsafety.NotNullByDefault;

import java.io.InputStream;

@NotNullByDefault
public interface SyncRecordReaderFactory {

	SyncRecordReader createRecordReader(InputStream in);
}
