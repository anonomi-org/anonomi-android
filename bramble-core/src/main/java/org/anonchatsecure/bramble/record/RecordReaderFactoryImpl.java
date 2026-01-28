package org.anonchatsecure.bramble.record;

import org.anonchatsecure.bramble.api.record.RecordReader;
import org.anonchatsecure.bramble.api.record.RecordReaderFactory;

import java.io.InputStream;

class RecordReaderFactoryImpl implements RecordReaderFactory {

	@Override
	public RecordReader createRecordReader(InputStream in) {
		return new RecordReaderImpl(in);
	}
}
