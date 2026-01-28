package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.api.record.RecordReader;
import org.anonchatsecure.bramble.api.record.RecordReaderFactory;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.sync.SyncRecordReader;
import org.anonchatsecure.bramble.api.sync.SyncRecordReaderFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.InputStream;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class SyncRecordReaderFactoryImpl implements SyncRecordReaderFactory {

	private final MessageFactory messageFactory;
	private final RecordReaderFactory recordReaderFactory;

	@Inject
	SyncRecordReaderFactoryImpl(MessageFactory messageFactory,
			RecordReaderFactory recordReaderFactory) {
		this.messageFactory = messageFactory;
		this.recordReaderFactory = recordReaderFactory;
	}

	@Override
	public SyncRecordReader createRecordReader(InputStream in) {
		RecordReader reader = recordReaderFactory.createRecordReader(in);
		return new SyncRecordReaderImpl(messageFactory, reader);
	}
}
