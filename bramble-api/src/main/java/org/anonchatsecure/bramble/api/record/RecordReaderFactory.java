package org.anonchatsecure.bramble.api.record;

import java.io.InputStream;

public interface RecordReaderFactory {

	RecordReader createRecordReader(InputStream in);
}
