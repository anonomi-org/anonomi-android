package org.anonchatsecure.bramble.api.crypto;

import org.anonchatsecure.bramble.api.transport.StreamContext;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.OutputStream;

@NotNullByDefault
public interface StreamEncrypterFactory {

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a transport stream.
	 */
	StreamEncrypter createStreamEncrypter(OutputStream out, StreamContext ctx);

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a contact exchange
	 * stream.
	 */
	StreamEncrypter createContactExchangeStreamEncrypter(OutputStream out,
			SecretKey headerKey);

	/**
	 * Creates a {@link StreamEncrypter} for encrypting a log stream.
	 */
	StreamEncrypter createLogStreamEncrypter(OutputStream out,
			SecretKey headerKey);
}
