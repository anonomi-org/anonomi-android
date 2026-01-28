package org.anonomi.android.xmr;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.util.Log;
import java.math.BigInteger;
import java.io.ByteArrayOutputStream;



public class SubaddressGenerator {

	public static byte[] generateSubaddressPublicSpendKey(byte[] publicSpendKey, byte[] privateViewKey, int major, int minor) {

		byte[] prefix = new byte[] { 'S', 'u', 'b', 'A', 'd', 'd', 'r', 0x00 };
		byte[] leMajor = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(major).array();
		byte[] leMinor = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(minor).array();

		byte[] data = new byte[prefix.length + privateViewKey.length + 4 + 4];
		System.arraycopy(prefix, 0, data, 0, prefix.length);
		System.arraycopy(privateViewKey, 0, data, prefix.length, privateViewKey.length);
		System.arraycopy(leMajor, 0, data, prefix.length + privateViewKey.length, 4);
		System.arraycopy(leMinor, 0, data, prefix.length + privateViewKey.length + 4, 4);

		byte[] m = CryptoUtils.hashToScalar(data);

		// 🚀 Compute m * G
		byte[] mGCompressed = CryptoUtils.scalarMultBase(m);

		byte[] D = null;
		try {
			// ✅ Decompress with YOUR OWN decompressPoint
			CryptoUtils.Point mGPointCustom = CryptoUtils.decompressPoint(mGCompressed);
			CryptoUtils.Point BPointCustom = CryptoUtils.decompressPoint(publicSpendKey);

			// ✅ Add points manually using YOUR pointAdd()
			CryptoUtils.Point DPointCustom = CryptoUtils.pointAdd(mGPointCustom, BPointCustom);

			// ✅ Compress the result using YOUR compressPoint()
			D = CryptoUtils.compressPoint(DPointCustom);

		} catch (Exception e) {
			Log.e("SubAddrGen", "pointAdd failed!", e);
		}

		return D;
	}

	public static byte[] encodeVarint(int value) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			if ((value & ~0x7F) == 0) {
				baos.write(value);
				break;
			} else {
				baos.write((value & 0x7F) | 0x80);
				value >>>= 7;
			}
		}
		return baos.toByteArray();
	}

	public static byte[] generateSubaddressPublicViewKey(
			byte[] subPubSpendKey,
			byte[] privateViewKey
	) {
		byte[] C = null;
		try {
			// ✅ Decompress D using your own decompressPoint
			CryptoUtils.Point DPointCustom = CryptoUtils.decompressPoint(subPubSpendKey);

			// ✅ Parse scalar b (privateViewKey) into BigInteger
			BigInteger bScalar = new BigInteger(1, CryptoUtils.reverseBytes(privateViewKey));

			// ✅ Manually scalar multiply (use your own scalarMultManual)
			CryptoUtils.Point CPointCustom = CryptoUtils.scalarMultManual(DPointCustom, bScalar);

			// ✅ Compress the result using your own compressPoint
			C = CryptoUtils.compressPoint(CPointCustom);

		} catch (Exception e) {
			Log.e("SubAddrGen", "scalarMultKey failed!", e);
		}

		return C;
	}
}