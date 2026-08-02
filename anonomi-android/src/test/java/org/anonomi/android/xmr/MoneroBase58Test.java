package org.anonomi.android.xmr;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Block-level vectors taken verbatim from monero-project's own
 * tests/unit_tests/base58.cpp. Monero's Base58 is block-based rather than
 * the bitcoin-style stream encoding, so an implementation can look correct
 * on short inputs and still be wrong at block boundaries.
 */
public class MoneroBase58Test {

	@Test
	public void testEncodesMoneroProjectVectors() {
		assertEquals("11", MoneroBase58.encode(bytes(0x00)));
		assertEquals("5Q", MoneroBase58.encode(bytes(0xFF)));
		assertEquals("LUv", MoneroBase58.encode(bytes(0xFF, 0xFF)));
		assertEquals("2UzHL", MoneroBase58.encode(bytes(0xFF, 0xFF, 0xFF)));
		assertEquals("7YXq9G",
				MoneroBase58.encode(bytes(0xFF, 0xFF, 0xFF, 0xFF)));
		assertEquals("jpXCZedGfVQ", MoneroBase58.encode(
				bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF)));
	}

	@Test
	public void testDecodesMoneroProjectVectors() {
		assertArrayEquals(bytes(0x00), MoneroBase58.decode("11"));
		assertArrayEquals(bytes(0xFF), MoneroBase58.decode("5Q"));
		assertArrayEquals(bytes(0xFF, 0xFF), MoneroBase58.decode("LUv"));
		assertArrayEquals(bytes(0xFF, 0xFF, 0xFF),
				MoneroBase58.decode("2UzHL"));
		assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF),
				MoneroBase58.decode("7YXq9G"));
		assertArrayEquals(
				bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF),
				MoneroBase58.decode("jpXCZedGfVQ"));
	}

	/**
	 * 69 bytes is the address length, which is not a multiple of the 8-byte
	 * block size, so every address exercises the partial trailing block.
	 */
	@Test
	public void testRoundTripsAtAddressLength() {
		Random random = new Random(0);
		for (int i = 0; i < 200; i++) {
			byte[] data = new byte[69];
			random.nextBytes(data);
			assertArrayEquals(data,
					MoneroBase58.decode(MoneroBase58.encode(data)));
		}
	}

	@Test
	public void testRoundTripsAcrossBlockBoundaries() {
		Random random = new Random(1);
		for (int length = 1; length <= 24; length++) {
			byte[] data = new byte[length];
			random.nextBytes(data);
			assertArrayEquals("Failed at length " + length, data,
					MoneroBase58.decode(MoneroBase58.encode(data)));
		}
	}

	@Test
	public void testPreservesLeadingZeroBytes() {
		byte[] data = new byte[69];
		data[68] = 0x01;
		assertArrayEquals(data,
				MoneroBase58.decode(MoneroBase58.encode(data)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRejectsCharacterOutsideAlphabet() {
		// '0', 'O', 'I' and 'l' are excluded from the Base58 alphabet
		MoneroBase58.decode("110");
	}

	private static byte[] bytes(int... values) {
		byte[] out = new byte[values.length];
		for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
		return out;
	}
}
