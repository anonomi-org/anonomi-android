package org.anonomi.android.util;

import org.anonomi.android.util.SecurePrefsCodec.MalformedRecordException;
import org.anonomi.android.util.SecurePrefsCodec.Record;
import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Runs on a plain JVM: the codec holds no key material and never opens the
 * Keystore, which is the whole reason it was split out of
 * {@link SecurePrefsManager}.
 * <p>
 * Base64 here is {@code java.util.Base64} rather than the framework's, which
 * is a stub in unit tests. The two agree on unwrapped Base64 and both signal
 * bad input with {@link IllegalArgumentException}, which is all the codec
 * relies on.
 */
public class SecurePrefsCodecTest {

	private static final SecurePrefsCodec.Base64Codec JVM_BASE64 =
			new SecurePrefsCodec.Base64Codec() {

				@Override
				public String encode(byte[] bytes) {
					return Base64.getEncoder().encodeToString(bytes);
				}

				@Override
				public byte[] decode(String encoded) {
					return Base64.getDecoder().decode(encoded);
				}
			};

	private final SecurePrefsCodec codec = new SecurePrefsCodec(JVM_BASE64);

	private static byte[] bytes(int length, int firstByte) {
		byte[] b = new byte[length];
		for (int i = 0; i < length; i++) b[i] = (byte) (firstByte + i);
		return b;
	}

	private static final byte[] IV = bytes(12, 1);
	private static final byte[] CIPHERTEXT = bytes(40, 100);

	@Test
	public void roundTrip() throws Exception {
		Record decoded = codec.decode(codec.encode(IV, CIPHERTEXT));
		assertArrayEquals(IV, decoded.getIv());
		assertArrayEquals(CIPHERTEXT, decoded.getCiphertext());
	}

	@Test
	public void encodesAsTwoBase64FieldsSeparatedByAColon() {
		String encoded = codec.encode(IV, CIPHERTEXT);
		String[] parts = encoded.split(":");
		assertEquals(2, parts.length);
		assertArrayEquals(IV, Base64.getDecoder().decode(parts[0]));
		assertArrayEquals(CIPHERTEXT, Base64.getDecoder().decode(parts[1]));
	}

	/**
	 * A value with no colon was previously returned to the caller verbatim,
	 * as though it had been decrypted, so anything that could write to the
	 * preferences file could choose what came back - including the
	 * stealth-mode passcode that unlocks the app.
	 */
	@Test
	public void rejectsColonLessInput() {
		assertMalformed("1+1");
		assertMalformed("sign_out");
		assertMalformed("delete_account");
	}

	@Test
	public void rejectsMalformedBase64() {
		assertMalformed("not!valid!base64:" +
				Base64.getEncoder().encodeToString(CIPHERTEXT));
		assertMalformed(Base64.getEncoder().encodeToString(IV) +
				":not!valid!base64");
	}

	@Test
	public void rejectsWrongPartCount() {
		String iv = Base64.getEncoder().encodeToString(IV);
		String ciphertext = Base64.getEncoder().encodeToString(CIPHERTEXT);
		assertMalformed(iv + ":" + ciphertext + ":" + ciphertext);
	}

	@Test
	public void rejectsEmptyFields() {
		String iv = Base64.getEncoder().encodeToString(IV);
		String ciphertext = Base64.getEncoder().encodeToString(CIPHERTEXT);
		assertMalformed(iv + ":");
		assertMalformed(":" + ciphertext);
		assertMalformed(":");
	}

	/**
	 * GCM output always carries the 16-byte tag, so anything shorter did not
	 * come from us however well-formed it looks.
	 */
	@Test
	public void rejectsCiphertextShorterThanTheGcmTag() {
		String iv = Base64.getEncoder().encodeToString(IV);
		String tooShort = Base64.getEncoder().encodeToString(bytes(15, 0));
		assertMalformed(iv + ":" + tooShort);
	}

	@Test
	public void acceptsCiphertextExactlyTheLengthOfTheGcmTag()
			throws Exception {
		byte[] tagOnly = bytes(16, 0);
		Record decoded = codec.decode(codec.encode(IV, tagOnly));
		assertArrayEquals(tagOnly, decoded.getCiphertext());
	}

	@Test
	public void rejectsNullAndEmpty() {
		assertMalformed(null);
		assertMalformed("");
	}

	/**
	 * The message is surfaced in the UI and may reach logs.
	 */
	@Test
	public void rejectionMessageDoesNotEchoTheStoredValue() {
		try {
			codec.decode("hunter2");
			fail("expected MalformedRecordException");
		} catch (MalformedRecordException e) {
			org.junit.Assert.assertFalse(e.getMessage().contains("hunter2"));
		}
	}

	private void assertMalformed(String stored) {
		try {
			codec.decode(stored);
			fail("expected MalformedRecordException for: " + stored);
		} catch (MalformedRecordException expected) {
			// what we want
		}
	}
}
