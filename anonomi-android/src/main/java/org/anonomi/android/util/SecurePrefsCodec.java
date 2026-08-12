package org.anonomi.android.util;

import androidx.annotation.Nullable;

/**
 * The on-disk record format used by {@link SecurePrefsManager}:
 * {@code base64(iv) + ":" + base64(ciphertext)}.
 * <p>
 * This is only the format - parsing it, validating it, and writing it back.
 * It holds no key material and never talks to the Keystore, so unlike
 * {@link SecurePrefsManager} it can be exercised by a plain JUnit test with no
 * device and no Robolectric. That is the point of the split: the format rules
 * below are the ones that decide whether a planted value is trusted, so they
 * are the ones that need tests.
 * <p>
 * Base64 is injected via {@link Base64Codec} because {@code android.util.Base64}
 * is a stubbed framework class in unit tests. Both the framework
 * implementation and {@code java.util.Base64} signal bad input by throwing
 * {@link IllegalArgumentException}, which is the only behaviour this class
 * relies on.
 * <p>
 * They are not equally strict, and the tests run against the JVM one, so be
 * clear about what that does and does not cover. The checks that keep a
 * planted value from being trusted are string operations - one separator, two
 * non-empty fields, and in particular the rejection of a value with no
 * separator at all - and those behave identically whichever decoder is
 * underneath. Base64 strictness is not load-bearing: {@code android.util.Base64}
 * skips line breaks and is looser about padding, so it will accept a few
 * records the JVM decoder rejects, and each one then has to survive GCM tag
 * verification. Authenticity rests on the tag, not on the encoding.
 */
public final class SecurePrefsCodec {

	/** AES-GCM authentication tag length in bits, as written by this codec. */
	public static final int GCM_TAG_LENGTH_BITS = 128;

	/**
	 * A GCM record always carries at least the authentication tag, so any
	 * shorter ciphertext cannot have come from us.
	 */
	private static final int MIN_CIPHERTEXT_BYTES = GCM_TAG_LENGTH_BITS / 8;

	private static final char SEPARATOR = ':';

	/**
	 * Base64 without line wrapping, in whichever implementation the caller has
	 * available.
	 */
	public interface Base64Codec {

		String encode(byte[] bytes);

		/**
		 * @throws IllegalArgumentException if {@code encoded} is not valid
		 * unwrapped Base64.
		 */
		byte[] decode(String encoded);
	}

	/**
	 * A stored record that parsed. It has not been decrypted and is not known
	 * to be authentic - only GCM can decide that.
	 */
	public static final class Record {

		private final byte[] iv;
		private final byte[] ciphertext;

		Record(byte[] iv, byte[] ciphertext) {
			this.iv = iv;
			this.ciphertext = ciphertext;
		}

		public byte[] getIv() {
			return iv;
		}

		public byte[] getCiphertext() {
			return ciphertext;
		}
	}

	/**
	 * Thrown when stored data is not a record this codec produced. Its message
	 * never contains any part of the stored data.
	 */
	public static final class MalformedRecordException extends Exception {

		MalformedRecordException(String message) {
			super(message);
		}
	}

	private final Base64Codec base64;

	public SecurePrefsCodec(Base64Codec base64) {
		this.base64 = base64;
	}

	public String encode(byte[] iv, byte[] ciphertext) {
		return base64.encode(iv) + SEPARATOR + base64.encode(ciphertext);
	}

	/**
	 * Parses a stored record.
	 * <p>
	 * Anything that is not exactly two non-empty Base64 fields separated by a
	 * single colon is rejected. In particular a value with no colon is
	 * rejected rather than passed through as though it were already
	 * plaintext: returning it would mean anything able to write to the
	 * preferences file could choose the value this class hands back, which is
	 * the guarantee the class exists to provide.
	 * <p>
	 * The IV length is deliberately not checked. GCM IV length is a property
	 * of the provider that wrote the record, and a record we wrote must stay
	 * readable; an IV of the wrong length simply fails to decrypt.
	 *
	 * @throws MalformedRecordException if {@code stored} is not a record this
	 * codec produced.
	 */
	public Record decode(@Nullable String stored)
			throws MalformedRecordException {
		if (stored == null) {
			throw new MalformedRecordException("record is null");
		}
		int separator = stored.indexOf(SEPARATOR);
		if (separator < 0) {
			throw new MalformedRecordException(
					"record has no separator, so it was not written encrypted");
		}
		if (stored.indexOf(SEPARATOR, separator + 1) >= 0) {
			throw new MalformedRecordException(
					"record has more than two fields");
		}
		String ivPart = stored.substring(0, separator);
		String ciphertextPart = stored.substring(separator + 1);
		if (ivPart.isEmpty() || ciphertextPart.isEmpty()) {
			throw new MalformedRecordException("record has an empty field");
		}

		byte[] iv;
		byte[] ciphertext;
		try {
			iv = base64.decode(ivPart);
			ciphertext = base64.decode(ciphertextPart);
		} catch (IllegalArgumentException e) {
			throw new MalformedRecordException("record is not valid base64");
		}
		if (iv.length == 0) {
			throw new MalformedRecordException("record has an empty iv");
		}
		if (ciphertext.length < MIN_CIPHERTEXT_BYTES) {
			throw new MalformedRecordException(
					"record is shorter than a GCM tag");
		}
		return new Record(iv, ciphertext);
	}
}
