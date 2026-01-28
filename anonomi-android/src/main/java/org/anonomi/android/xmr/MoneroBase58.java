package org.anonomi.android.xmr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoneroBase58 {

	private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	private static final BigInteger BASE = BigInteger.valueOf(58);
	private static final int FULL_BLOCK_SIZE = 8;
	private static final int FULL_ENCODED_BLOCK_SIZE = 11;

	public static String encode(byte[] data) {
		List<String> result = new ArrayList<>();
		int fullBlockCount = data.length / FULL_BLOCK_SIZE;
		int lastBlockSize = data.length % FULL_BLOCK_SIZE;
		for (int i = 0; i < fullBlockCount; i++) {
			byte[] block = Arrays.copyOfRange(data, i * FULL_BLOCK_SIZE, (i + 1) * FULL_BLOCK_SIZE);
			result.add(encodeBlock(block, FULL_ENCODED_BLOCK_SIZE));
		}
		if (lastBlockSize > 0) {
			byte[] block = Arrays.copyOfRange(data, fullBlockCount * FULL_BLOCK_SIZE, data.length);
			int encodedSize = getEncodedBlockSize(lastBlockSize);
			result.add(encodeBlock(block, encodedSize));
		}
		return String.join("", result);
	}

	public static byte[] decode(String encoded) {
		List<Byte> result = new ArrayList<>();
		int fullBlockCount = encoded.length() / FULL_ENCODED_BLOCK_SIZE;
		int lastBlockSize = encoded.length() % FULL_ENCODED_BLOCK_SIZE;
		for (int i = 0; i < fullBlockCount; i++) {
			String block = encoded.substring(i * FULL_ENCODED_BLOCK_SIZE, (i + 1) * FULL_ENCODED_BLOCK_SIZE);
			byte[] decodedBlock = decodeBlock(block, FULL_BLOCK_SIZE);
			for (byte b : decodedBlock) result.add(b);
		}
		if (lastBlockSize > 0) {
			String block = encoded.substring(fullBlockCount * FULL_ENCODED_BLOCK_SIZE);
			int decodedSize = getDecodedBlockSize(lastBlockSize);
			byte[] decodedBlock = decodeBlock(block, decodedSize);
			for (byte b : decodedBlock) result.add(b);
		}
		byte[] out = new byte[result.size()];
		for (int i = 0; i < out.length; i++) out[i] = result.get(i);
		return out;
	}

	private static String encodeBlock(byte[] data, int encodedSize) {
		BigInteger num = new BigInteger(1, data);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < encodedSize; i++) {
			BigInteger[] divRem = num.divideAndRemainder(BASE);
			sb.append(ALPHABET.charAt(divRem[1].intValue()));
			num = divRem[0];
		}
		return sb.reverse().toString();
	}

	private static byte[] decodeBlock(String data, int decodedSize) {
		BigInteger num = BigInteger.ZERO;
		for (char c : data.toCharArray()) {
			int digit = ALPHABET.indexOf(c);
			if (digit < 0) throw new IllegalArgumentException("Invalid Base58 character: " + c);
			num = num.multiply(BASE).add(BigInteger.valueOf(digit));
		}
		byte[] full = num.toByteArray();
		byte[] result = new byte[decodedSize];
		int start = full.length > decodedSize ? full.length - decodedSize : 0;
		int length = Math.min(full.length, decodedSize);
		System.arraycopy(full, start, result, decodedSize - length, length);
		return result;
	}

	private static int getEncodedBlockSize(int decodedSize) {
		switch (decodedSize) {
			case 1: return 2;
			case 2: return 3;
			case 3: return 5;
			case 4: return 6;
			case 5: return 7;
			case 6: return 9;
			case 7: return 10;
			case 8: return 11;
			default: throw new IllegalArgumentException("Invalid decoded block size: " + decodedSize);
		}
	}

	private static int getDecodedBlockSize(int encodedSize) {
		switch (encodedSize) {
			case 2: return 1;
			case 3: return 2;
			case 5: return 3;
			case 6: return 4;
			case 7: return 5;
			case 9: return 6;
			case 10: return 7;
			case 11: return 8;
			default: throw new IllegalArgumentException("Invalid encoded block size: " + encodedSize);
		}
	}
}