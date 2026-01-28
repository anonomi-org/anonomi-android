package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.api.sync.GroupFactory;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.api.sync.SyncRecordReaderFactory;
import org.anonchatsecure.bramble.api.sync.SyncRecordWriterFactory;
import org.anonchatsecure.bramble.api.sync.SyncSessionFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SyncModule {

	@Provides
	GroupFactory provideGroupFactory(GroupFactoryImpl groupFactory) {
		return groupFactory;
	}

	@Provides
	MessageFactory provideMessageFactory(MessageFactoryImpl messageFactory) {
		return messageFactory;
	}

	@Provides
	SyncRecordReaderFactory provideRecordReaderFactory(
			SyncRecordReaderFactoryImpl recordReaderFactory) {
		return recordReaderFactory;
	}

	@Provides
	SyncRecordWriterFactory provideRecordWriterFactory(
			SyncRecordWriterFactoryImpl recordWriterFactory) {
		return recordWriterFactory;
	}

	@Provides
	@Singleton
	SyncSessionFactory provideSyncSessionFactory(
			SyncSessionFactoryImpl syncSessionFactory) {
		return syncSessionFactory;
	}
}
