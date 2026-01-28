package org.anonchatsecure.bramble.record;

import org.anonchatsecure.bramble.api.record.RecordWriter;
import org.anonchatsecure.bramble.api.record.RecordWriterFactory;

import java.io.OutputStream;

class RecordWriterFactoryImpl implements RecordWriterFactory {

	@Override
	public RecordWriter createRecordWriter(OutputStream out) {
		return new RecordWriterImpl(out);
	}
}
