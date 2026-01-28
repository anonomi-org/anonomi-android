package org.anonchatsecure.bramble.crypto;

import org.anonchatsecure.bramble.api.crypto.AgreementPrivateKey;
import org.anonchatsecure.bramble.api.crypto.AgreementPublicKey;
import org.anonchatsecure.bramble.api.crypto.KeyParser;
import org.anonchatsecure.bramble.api.crypto.PrivateKey;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class AgreementKeyParser implements KeyParser {

	@Override
	public PublicKey parsePublicKey(byte[] encodedKey)
			throws GeneralSecurityException {
		if (encodedKey.length != 32) throw new GeneralSecurityException();
		return new AgreementPublicKey(encodedKey);
	}

	@Override
	public PrivateKey parsePrivateKey(byte[] encodedKey)
			throws GeneralSecurityException {
		if (encodedKey.length != 32) throw new GeneralSecurityException();
		return new AgreementPrivateKey(clamp(encodedKey));
	}

	static byte[] clamp(byte[] b) {
		byte[] clamped = new byte[32];
		System.arraycopy(b, 0, clamped, 0, 32);
		clamped[0] &= 248;
		clamped[31] &= 127;
		clamped[31] |= 64;
		return clamped;
	}
}
