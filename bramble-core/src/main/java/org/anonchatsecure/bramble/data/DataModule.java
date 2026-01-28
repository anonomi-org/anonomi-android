package org.anonchatsecure.bramble.data;

import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.data.MetadataParser;

import dagger.Module;
import dagger.Provides;

@Module
public class DataModule {

	@Provides
	BdfReaderFactory provideBdfReaderFactory() {
		return new BdfReaderFactoryImpl();
	}

	@Provides
	BdfWriterFactory provideBdfWriterFactory() {
		return new BdfWriterFactoryImpl();
	}

	@Provides
	MetadataParser provideMetaDataParser(BdfReaderFactory bdfReaderFactory) {
		return new MetadataParserImpl(bdfReaderFactory);
	}

	@Provides
	MetadataEncoder provideMetaDataEncoder(BdfWriterFactory bdfWriterFactory) {
		return new MetadataEncoderImpl(bdfWriterFactory);
	}

}
