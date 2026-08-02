package org.anonomi.android.xmr;

import java.util.regex.Pattern;

public class AnonMoneroUtils {

	// Basic Monero address pattern (can be made more specific)
	private static final Pattern MONERO_ADDRESS_PATTERN =
			Pattern.compile("^4[0-9AB][1-9A-HJ-NP-Za-km-z]{93}$");

	// Basic Monero private view key pattern (64 hexadecimal characters)
	private static final Pattern MONERO_PRIVATE_VIEW_KEY_PATTERN =
			Pattern.compile("^[0-9a-fA-F]{64}$");

	/**
	 * Network byte of a mainnet subaddress (42 decimal). Primary addresses use
	 * 18 and integrated addresses 19, neither of which we generate.
	 */
	private static final byte SUBADDRESS_NETWORK_BYTE = 0x2a;

	/** Network byte (1) + public spend key (32) + public view key (32). */
	private static final int ADDRESS_BODY_LENGTH = 65;
	/** Leading bytes of the body's Keccak-256 hash appended to an address. */
	private static final int CHECKSUM_LENGTH = 4;
	private static final int ADDRESS_LENGTH =
			ADDRESS_BODY_LENGTH + CHECKSUM_LENGTH;
	private static final int KEY_LENGTH = 32;

	/**
	 * Returns true if the string is a well-formed mainnet primary address whose
	 * checksum matches. The checksum is what catches a mistyped or truncated
	 * address before we derive subaddresses from its keys.
	 */
	public static boolean isValidMoneroAddress(String address) {
		if (address == null || address.isEmpty()) return false;
		if (!MONERO_ADDRESS_PATTERN.matcher(address).matches()) return false;
		try {
			decodeAddress(address);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	public static boolean isValidMoneroPrivateKey(String privateKey) {
		return privateKey != null && !privateKey.isEmpty() &&
				MONERO_PRIVATE_VIEW_KEY_PATTERN.matcher(privateKey).matches();
	}

	/**
	 * Decodes an address and verifies its checksum.
	 *
	 * @throws IllegalArgumentException if the address is the wrong length or
	 * its checksum does not match the body.
	 */
	public static MoneroDecodedAddress decodeAddress(String address) {
		byte[] decoded = MoneroBase58.decode(address);
		if (decoded.length != ADDRESS_LENGTH) {
			throw new IllegalArgumentException(
					"Invalid decoded address length: " + decoded.length);
		}
		verifyChecksum(decoded);

		byte[] publicSpendKey = new byte[KEY_LENGTH];
		byte[] publicViewKey = new byte[KEY_LENGTH];
		System.arraycopy(decoded, 1, publicSpendKey, 0, KEY_LENGTH);
		System.arraycopy(decoded, 1 + KEY_LENGTH, publicViewKey, 0, KEY_LENGTH);
		return new MoneroDecodedAddress(publicSpendKey, publicViewKey);
	}

	/**
	 * Derives the mainnet subaddress at (major, minor) for the wallet holding
	 * publicSpendKey and privateViewKey.
	 *
	 * @param privateViewKey the wallet's private view key, 32 bytes
	 * little-endian as produced by {@link #hexToBytes}.
	 * @throws IllegalStateException if the underlying curve arithmetic fails.
	 */
	public static String buildSubaddress(byte[] publicSpendKey,
			byte[] privateViewKey, int major, int minor) {
		byte[] subSpendKey = SubaddressGenerator.generateSubaddressPublicSpendKey(
				publicSpendKey, privateViewKey, major, minor);
		if (subSpendKey == null) {
			throw new IllegalStateException(
					"Could not derive subaddress public spend key");
		}
		byte[] subViewKey = SubaddressGenerator.generateSubaddressPublicViewKey(
				subSpendKey, privateViewKey);
		if (subViewKey == null) {
			throw new IllegalStateException(
					"Could not derive subaddress public view key");
		}

		byte[] address = new byte[ADDRESS_LENGTH];
		address[0] = SUBADDRESS_NETWORK_BYTE;
		System.arraycopy(subSpendKey, 0, address, 1, KEY_LENGTH);
		System.arraycopy(subViewKey, 0, address, 1 + KEY_LENGTH, KEY_LENGTH);
		byte[] checksum = CryptoUtils.keccak256(address, 0,
				ADDRESS_BODY_LENGTH);
		System.arraycopy(checksum, 0, address, ADDRESS_BODY_LENGTH,
				CHECKSUM_LENGTH);
		return MoneroBase58.encode(address);
	}

	public static byte[] hexToBytes(String s) {
		int len = s.length();
		if (len % 2 != 0) {
			throw new IllegalArgumentException("Odd-length hex string");
		}
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			int hi = Character.digit(s.charAt(i), 16);
			int lo = Character.digit(s.charAt(i + 1), 16);
			if (hi < 0 || lo < 0) {
				throw new IllegalArgumentException("Invalid hex string");
			}
			data[i / 2] = (byte) ((hi << 4) + lo);
		}
		return data;
	}

	private static void verifyChecksum(byte[] address) {
		byte[] expected = CryptoUtils.keccak256(address, 0,
				ADDRESS_BODY_LENGTH);
		for (int i = 0; i < CHECKSUM_LENGTH; i++) {
			if (expected[i] != address[ADDRESS_BODY_LENGTH + i]) {
				throw new IllegalArgumentException("Address checksum mismatch");
			}
		}
	}
}
