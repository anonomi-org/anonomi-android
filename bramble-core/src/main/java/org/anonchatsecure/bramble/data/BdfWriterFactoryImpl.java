package org.anonchatsecure.bramble.data;

import org.anonchatsecure.bramble.api.data.BdfWriter;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.OutputStream;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class BdfWriterFactoryImpl implements BdfWriterFactory {

	@Override
	public BdfWriter createWriter(OutputStream out) {
		return new BdfWriterImpl(out);
	}
}
