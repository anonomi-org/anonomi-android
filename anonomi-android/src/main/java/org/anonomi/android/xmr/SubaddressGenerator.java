package org.anonomi.android.xmr;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Monero subaddress key derivation.
 * <p>
 * Failures propagate rather than returning null: a caller that silently got a
 * null key here would go on to build an address the recipient cannot spend.
 */
public class SubaddressGenerator {

	private static final byte[] SUBADDRESS_PREFIX =
			{'S', 'u', 'b', 'A', 'd', 'd', 'r', 0x00};

	/**
	 * D = B + m*G, where m = Hs("SubAddr" || 0 || a || major || minor).
	 *
	 * @param publicSpendKey the wallet's public spend key B, 32 bytes
	 * @param privateViewKey the wallet's private view key a, 32 bytes
	 *                       little-endian
	 */
	public static byte[] generateSubaddressPublicSpendKey(byte[] publicSpendKey,
			byte[] privateViewKey, int major, int minor) {
		byte[] data = new byte[SUBADDRESS_PREFIX.length + privateViewKey.length
				+ 8];
		int offset = 0;
		System.arraycopy(SUBADDRESS_PREFIX, 0, data, offset,
				SUBADDRESS_PREFIX.length);
		offset += SUBADDRESS_PREFIX.length;
		System.arraycopy(privateViewKey, 0, data, offset,
				privateViewKey.length);
		offset += privateViewKey.length;
		System.arraycopy(toLittleEndian(major), 0, data, offset, 4);
		offset += 4;
		System.arraycopy(toLittleEndian(minor), 0, data, offset, 4);

		byte[] m = CryptoUtils.hashToScalar(data);
		CryptoUtils.Point mG =
				CryptoUtils.decompressPoint(CryptoUtils.scalarMultBase(m));
		CryptoUtils.Point b = CryptoUtils.decompressPoint(publicSpendKey);
		return CryptoUtils.compressPoint(CryptoUtils.pointAdd(mG, b));
	}

	/**
	 * C = a*D, the subaddress public view key.
	 *
	 * @param subPubSpendKey the subaddress public spend key D, 32 bytes
	 * @param privateViewKey the wallet's private view key a, 32 bytes
	 *                       little-endian
	 */
	public static byte[] generateSubaddressPublicViewKey(byte[] subPubSpendKey,
			byte[] privateViewKey) {
		CryptoUtils.Point d = CryptoUtils.decompressPoint(subPubSpendKey);
		BigInteger a =
				new BigInteger(1, CryptoUtils.reverseBytes(privateViewKey));
		return CryptoUtils.compressPoint(CryptoUtils.scalarMultManual(d, a));
	}

	private static byte[] toLittleEndian(int value) {
		return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
				.putInt(value).array();
	}
}
