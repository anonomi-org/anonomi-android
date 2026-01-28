package org.anonomi.android.xmr;

import org.bouncycastle.crypto.digests.KeccakDigest;
import java.math.BigInteger;
import java.util.Arrays;

import android.util.Log;

public class CryptoUtils {

	public static final BigInteger L = new BigInteger(
			"7237005577332262213973186563042994240857116359379907606001950938285454250989");

	// Ed25519 constants
	private static final BigInteger P_MODULUS = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffed", 16); // 2^255 - 19
	private static final BigInteger D = new BigInteger("-121665").multiply(new BigInteger("121666").modInverse(P_MODULUS)).mod(P_MODULUS);
	private static final BigInteger I = new BigInteger("2").modPow(P_MODULUS.subtract(BigInteger.ONE).divide(new BigInteger("4")), P_MODULUS); // sqrt(-1)


	public static byte[] keccak256(byte[] data, int offset, int length) {
		KeccakDigest digest = new KeccakDigest(256);
		digest.update(data, offset, length);
		byte[] out = new byte[32];
		digest.doFinal(out, 0);
		return out;
	}

	public static byte[] hashToScalar(byte[] data) {
		byte[] hash = keccak256(data, 0, data.length);

		byte[] hBuffer = reverseBytes(hash);  // LE

		BigInteger s = new BigInteger(1, hBuffer);

		BigInteger reduced = s.mod(L);

		byte[] scalar = to32BytesLE(reduced);

		return scalar;
	}

	private static byte[] to32BytesLE(BigInteger val) {
		byte[] full = val.toByteArray();
		byte[] le = new byte[32];
		int copyLen = Math.min(full.length, 32);
		for (int i = 0; i < copyLen; i++) {
			le[i] = full[full.length - 1 - i];
		}
		return le;
	}

	public static byte[] scalarMultBase(byte[] scalarLE) {

		try {
			// Convert scalarLE to BigInteger (little-endian to big-endian)
			BigInteger scalar = new BigInteger(1, reverseBytes(scalarLE));

			// Get Ed25519 base point (standard Monero/Ed25519 base point)
			Point base = getEd25519BasePoint();

			// Do scalar multiplication manually
			Point result = scalarMultManual(base, scalar);

			// Compress point (to match Monero format)
			byte[] compressed = compressPoint(result);


			return compressed;
		} catch (Exception e) {
			Log.e("CryptoUtils", "Exception in scalarMultBasePureJava", e);
			return null;
		}
	}

	private static Point getEd25519BasePoint() {
		// Standard Ed25519 base point (from RFC 8032)
		String baseXHex = "216936d3cd6e53fec0a4e231fdd6dc5c692cc7609525a7b2c9562d608f25d51a";
		String baseYHex = "6666666666666666666666666666666666666666666666666666666666666658";

		BigInteger x = new BigInteger(baseXHex, 16);
		BigInteger y = new BigInteger(baseYHex, 16);

		return new Point(x, y);
	}


	public static Point scalarMultManual(Point P, BigInteger k) {

		Point result = null;  // Start with "infinity"
		Point addend = P;

		int numBits = k.bitLength();
		for (int i = 0; i < numBits; i++) {
			if (k.testBit(i)) {
				if (result == null) {
					result = addend;
				} else {
					result = pointAdd(result, addend);
				}
			}
			addend = pointAdd(addend, addend);  // Double
		}

		if (result == null) {
			// Return identity point (0,1) on Edwards curve
			return new Point(BigInteger.ZERO, BigInteger.ONE);
		}
		return result;
	}

	public static byte[] scalarMultKey(byte[] scalarLE, byte[] publicKey) {

		try {
			// Decompress the publicKey into a Point (manual decompress)
			Point pubPoint = decompressPoint(publicKey);

			// Convert scalarLE to BigInteger (little-endian to big-endian)
			BigInteger scalar = new BigInteger(1, reverseBytes(scalarLE));

			// Perform scalar multiplication manually
			Point resultPoint = scalarMultManual(pubPoint, scalar);

			// Compress result back to 32 bytes
			byte[] compressed = compressPoint(resultPoint);

			return compressed;
		} catch (Exception e) {
			Log.e("CryptoUtils", "Exception in scalarMultKey", e);
			return null;
		}
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) sb.append(String.format("%02x", b));
		return sb.toString();
	}



	public static class Point {
		public BigInteger x, y;
		public Point(BigInteger x, BigInteger y) {
			this.x = x;
			this.y = y;
		}
	}

	private static BigInteger sqrtMod(BigInteger a) {
		BigInteger exp = P_MODULUS.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8));
		BigInteger x = a.modPow(exp, P_MODULUS);
		if (x.multiply(x).subtract(a).mod(P_MODULUS).equals(BigInteger.ZERO)) {
			return x;
		}
		return x.multiply(I).mod(P_MODULUS);
	}

	public static Point decompressPoint(byte[] compressed) {
		if (compressed.length != 32)
			throw new IllegalArgumentException("Compressed point must be 32 bytes");

		byte[] yBytes = Arrays.copyOf(compressed, 32);
		boolean xShouldBeNegative = (compressed[31] & 0x80) != 0;
		yBytes[31] &= 0x7F;

		// ✅ Fix endian: Monero compressed points are LittleEndian,
		// but BigInteger expects BigEndian
		byte[] yBytesReversed = reverseBytes(yBytes);
		BigInteger y = new BigInteger(1, yBytesReversed);

		BigInteger y2 = y.multiply(y).mod(P_MODULUS);
		BigInteger u = y2.subtract(BigInteger.ONE).mod(P_MODULUS);
		BigInteger v = D.multiply(y2).add(BigInteger.ONE).mod(P_MODULUS);

		BigInteger invV = v.modInverse(P_MODULUS);

		BigInteger x = sqrtMod(u.multiply(invV).mod(P_MODULUS));

		boolean xIsOdd = x.testBit(0);
		if (xIsOdd != xShouldBeNegative) {
			x = P_MODULUS.subtract(x);
		}

		// ✅ NEW: recompress as LittleEndian before returning (for full round-trip check)
		byte[] recompressed = compressPoint(new Point(x, y));

		return new Point(x, y);
	}

	public static byte[] recompressPoint(BigInteger x, BigInteger y) {
		byte[] yBytes = to32Bytes(y);
		byte[] yLE = reverseBytes(yBytes);  // ✅ Monero format is little-endian
		if (x.testBit(0)) {
			yLE[31] |= 0x80;
		}
		return yLE;
	}

	public static byte[] to32Bytes(BigInteger num) {
		byte[] tmp = num.toByteArray();
		byte[] out = new byte[32];
		if (tmp.length == 33 && tmp[0] == 0x00) {
			System.arraycopy(tmp, 1, out, 0, 32);
		} else if (tmp.length <= 32) {
			System.arraycopy(tmp, 0, out, 32 - tmp.length, tmp.length);
		} else {
			throw new IllegalArgumentException("Number too large!");
		}
		return out;
	}



	public static byte[] reverseBytes(byte[] input) {
		byte[] reversed = new byte[input.length];
		for (int i = 0; i < input.length; i++) {
			reversed[i] = input[input.length - 1 - i];
		}
		return reversed;
	}

	// ✅ POINT ADD: Manual Ed25519 point addition

	public static Point pointAdd(Point p1, Point p2) {
		BigInteger x1 = p1.x, y1 = p1.y;
		BigInteger x2 = p2.x, y2 = p2.y;

		BigInteger x1y2 = x1.multiply(y2).mod(P_MODULUS);
		BigInteger x2y1 = x2.multiply(y1).mod(P_MODULUS);
		BigInteger y1y2 = y1.multiply(y2).mod(P_MODULUS);
		BigInteger x1x2 = x1.multiply(x2).mod(P_MODULUS);
		BigInteger dx1x2y1y2 = D.multiply(x1x2).multiply(y1y2).mod(P_MODULUS);

		BigInteger x3 = (x1y2.add(x2y1))
				.multiply(BigInteger.ONE.add(dx1x2y1y2).modInverse(P_MODULUS))
				.mod(P_MODULUS);

		BigInteger y3 = (y1y2.add(x1x2))
				.multiply(BigInteger.ONE.subtract(dx1x2y1y2).modInverse(P_MODULUS))
				.mod(P_MODULUS);

		return new Point(x3, y3);
	}

	// ✅ COMPRESS POINT: Manual Ed25519 point compression
	public static byte[] compressPoint(Point pt) {
		// Get Y in 32-byte BigEndian
		byte[] yBytesBE = to32Bytes(pt.y);

		// ✅ Reverse to LittleEndian for Monero format
		byte[] yBytesLE = reverseBytes(yBytesBE);

		// Set sign bit for X parity (on the last byte of LE array)
		if (pt.x.testBit(0)) {
			yBytesLE[31] |= 0x80;
		}

		return yBytesLE;
	}
}