package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.api.record.RecordWriter;
import org.anonchatsecure.bramble.api.record.RecordWriterFactory;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.sync.SyncRecordWriter;
import org.anonchatsecure.bramble.api.sync.SyncRecordWriterFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.OutputStream;

import javax.inject.Inject;

@NotNullByDefault
class SyncRecordWriterFactoryImpl implements SyncRecordWriterFactory {

	private final MessageFactory messageFactory;
	private final RecordWriterFactory recordWriterFactory;

	@Inject
	SyncRecordWriterFactoryImpl(MessageFactory messageFactory,
			RecordWriterFactory recordWriterFactory) {
		this.messageFactory = messageFactory;
		this.recordWriterFactory = recordWriterFactory;
	}

	@Override
	public SyncRecordWriter createRecordWriter(OutputStream out) {
		RecordWriter writer = recordWriterFactory.createRecordWriter(out);
		return new SyncRecordWriterImpl(messageFactory, writer);
	}
}
