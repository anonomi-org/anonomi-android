package org.anonomi.android.xmr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Subaddress derivation vectors from dan-da/subaddress-derive-xmr, which
 * derives them via monero-project's own libraries.
 * <p>
 * The vector is self-corroborating: PRIMARY_ADDRESS and PUBLIC_SPEND_KEY are
 * published independently of each other, and
 * {@link #testPrimaryAddressDecodesToPublishedSpendKey()} checks that our
 * Base58 and address decoding agree that the former contains the latter.
 */
public class AnonMoneroUtilsTest {

	private static final String PRIVATE_VIEW_KEY =
			"25d014a444fb7a1e6836c680d3ec1b6eed628a29c3c85e0379fb89f53c4c610a";
	private static final String PUBLIC_SPEND_KEY =
			"dce90ff7304d8b648bfbac69624b4c6562340c5c748a8a6d2c84bad3b76fe974";
	private static final String PRIMARY_ADDRESS =
			"49zf2PF7nLSHpRwWcPG8ePHxYnR6eFmYuKG8Akpq5vFALTzZzMdv3kC36fCSP3Uf" +
					"FdMrY51QAs5NGiGuwXK6YMa3Nk7549x";
	private static final String SUBADDRESS_0_1 =
			"87i7kA61fNvMboXiYWHVygPAggKJPETFqLXXcdH4mQTrECvrTxZMtt6e6owj1k8j" +
					"UVjNR11eBuBMWHFBtxAwEVcm9dcSUxr";
	private static final String SUBADDRESS_0_2 =
			"8A9XmWsATrhfedtNhTMNKELwfCwMVAk2iVTdUJdFRb2AC4tV4VeBjsCLYR9cSQTw" +
					"nvLo4MAuQFMLP6Si4xp6t6BS788db3t";

	@Test
	public void testAcceptsDerivedSubaddresses() {
		assertTrue(AnonMoneroUtils.isValidMoneroSubaddress(SUBADDRESS_0_1));
		assertTrue(AnonMoneroUtils.isValidMoneroSubaddress(SUBADDRESS_0_2));
	}

	/**
	 * The two checks are not interchangeable: a primary address and a
	 * subaddress carry different network bytes, so each is invalid to the
	 * other's check.
	 */
	@Test
	public void testSubaddressCheckRejectsAPrimaryAddress() {
		assertFalse(AnonMoneroUtils.isValidMoneroSubaddress(PRIMARY_ADDRESS));
		assertFalse(AnonMoneroUtils.isValidMoneroAddress(SUBADDRESS_0_1));
	}

	@Test
	public void testSubaddressCheckRejectsAlteredAddress() {
		// Change one character, which the checksum has to catch
		String altered = SUBADDRESS_0_1.substring(0, 10) + "X" +
				SUBADDRESS_0_1.substring(11);
		assertFalse(AnonMoneroUtils.isValidMoneroSubaddress(altered));
		assertFalse(AnonMoneroUtils.isValidMoneroSubaddress(""));
		assertFalse(AnonMoneroUtils.isValidMoneroSubaddress(null));
		assertFalse(AnonMoneroUtils.isValidMoneroSubaddress("not an address"));
	}

	/**
	 * The amounts a double would get wrong. 0.1 has no exact binary
	 * representation, and 12 decimal places is the finest Monero counts.
	 */
	@Test
	public void testConvertsDecimalAmountsExactly() {
		assertEquals(100_000_000_000L,
				AnonMoneroUtils.xmrToAtomicUnits("0.1"));
		assertEquals(1L, AnonMoneroUtils.xmrToAtomicUnits("0.000000000001"));
		assertEquals(0L, AnonMoneroUtils.xmrToAtomicUnits("0"));
		assertEquals(1_000_000_000_000L,
				AnonMoneroUtils.xmrToAtomicUnits("1"));
		assertEquals(123_456_789_012L,
				AnonMoneroUtils.xmrToAtomicUnits("0.123456789012"));
		// The comma the amount field accepts as a decimal separator
		assertEquals(500_000_000_000L,
				AnonMoneroUtils.xmrToAtomicUnits("0,5"));
		assertEquals(2_500_000_000_000L,
				AnonMoneroUtils.xmrToAtomicUnits(" 2.5 "));
	}

	@Test
	public void testRoundTripsAmounts() {
		assertEquals("0.1", AnonMoneroUtils.atomicUnitsToXmr(
				AnonMoneroUtils.xmrToAtomicUnits("0.1")));
		assertEquals("0.000000000001", AnonMoneroUtils.atomicUnitsToXmr(1L));
		assertEquals("0", AnonMoneroUtils.atomicUnitsToXmr(0L));
		assertEquals("1", AnonMoneroUtils.atomicUnitsToXmr(
				1_000_000_000_000L));
		assertEquals("2.5", AnonMoneroUtils.atomicUnitsToXmr(
				2_500_000_000_000L));
	}

	@Test
	public void testRejectsUnrepresentableAmounts() {
		String[] invalid = {
				"-1",              // negative
				"0.0000000000001", // finer than an atomic unit
				"10000000",        // more atomic units than a long holds
				"",
				"abc",
				null
		};
		for (String s : invalid) {
			try {
				AnonMoneroUtils.xmrToAtomicUnits(s);
				fail("accepted " + s);
			} catch (NumberFormatException expected) {
				// Expected
			}
		}
	}

	@Test
	public void testPrimaryAddressDecodesToPublishedSpendKey() {
		MoneroDecodedAddress decoded =
				AnonMoneroUtils.decodeAddress(PRIMARY_ADDRESS);
		assertEquals(PUBLIC_SPEND_KEY,
				CryptoUtils.bytesToHex(decoded.getPublicSpendKey()));
	}

	@Test
	public void testDerivesKnownSubaddresses() {
		byte[] spendKey = AnonMoneroUtils.hexToBytes(PUBLIC_SPEND_KEY);
		byte[] viewKey = AnonMoneroUtils.hexToBytes(PRIVATE_VIEW_KEY);
		assertEquals(SUBADDRESS_0_1,
				AnonMoneroUtils.buildSubaddress(spendKey, viewKey, 0, 1));
		assertEquals(SUBADDRESS_0_2,
				AnonMoneroUtils.buildSubaddress(spendKey, viewKey, 0, 2));
	}

	/**
	 * The keys we derive from must come from the address the user pasted, so
	 * derive from the decoded address rather than the published spend key.
	 */
	@Test
	public void testDerivesFromDecodedPrimaryAddress() {
		MoneroDecodedAddress decoded =
				AnonMoneroUtils.decodeAddress(PRIMARY_ADDRESS);
		byte[] viewKey = AnonMoneroUtils.hexToBytes(PRIVATE_VIEW_KEY);
		assertEquals(SUBADDRESS_0_1, AnonMoneroUtils.buildSubaddress(
				decoded.getPublicSpendKey(), viewKey, 0, 1));
	}

	@Test
	public void testSubaddressesAreDistinctPerIndex() {
		byte[] spendKey = AnonMoneroUtils.hexToBytes(PUBLIC_SPEND_KEY);
		byte[] viewKey = AnonMoneroUtils.hexToBytes(PRIVATE_VIEW_KEY);
		String first = AnonMoneroUtils.buildSubaddress(spendKey, viewKey, 0, 1);
		String second = AnonMoneroUtils.buildSubaddress(spendKey, viewKey, 0, 2);
		assertFalse("Reusing an index would link payments to one recipient",
				first.equals(second));
	}

	@Test
	public void testAcceptsValidPrimaryAddress() {
		assertTrue(AnonMoneroUtils.isValidMoneroAddress(PRIMARY_ADDRESS));
	}

	@Test
	public void testRejectsAddressWithCorruptedCharacter() {
		// Swap one character in the body; the checksum must catch it
		char[] chars = PRIMARY_ADDRESS.toCharArray();
		chars[20] = chars[20] == 'A' ? 'B' : 'A';
		assertFalse(AnonMoneroUtils.isValidMoneroAddress(new String(chars)));
	}

	@Test
	public void testRejectsMalformedAddresses() {
		assertFalse(AnonMoneroUtils.isValidMoneroAddress(null));
		assertFalse(AnonMoneroUtils.isValidMoneroAddress(""));
		// A subaddress is not a valid input; we derive from primary addresses
		assertFalse(AnonMoneroUtils.isValidMoneroAddress(SUBADDRESS_0_1));
	}

	@Test
	public void testDecodeRejectsCorruptedChecksum() {
		char[] chars = PRIMARY_ADDRESS.toCharArray();
		chars[chars.length - 1] = chars[chars.length - 1] == 'x' ? 'y' : 'x';
		try {
			AnonMoneroUtils.decodeAddress(new String(chars));
			fail("Expected a checksum mismatch");
		} catch (IllegalArgumentException expected) {
			// Expected
		}
	}

	@Test
	public void testValidatesPrivateViewKeys() {
		assertTrue(AnonMoneroUtils.isValidMoneroPrivateKey(PRIVATE_VIEW_KEY));
		assertFalse(AnonMoneroUtils.isValidMoneroPrivateKey(null));
		assertFalse(AnonMoneroUtils.isValidMoneroPrivateKey(""));
		// 63 characters, one short
		assertFalse(AnonMoneroUtils.isValidMoneroPrivateKey(
				PRIVATE_VIEW_KEY.substring(1)));
		assertFalse(AnonMoneroUtils.isValidMoneroPrivateKey(
				PRIVATE_VIEW_KEY.replace('2', 'z')));
	}

	@Test
	public void testHexToBytesRoundTrip() {
		byte[] bytes = AnonMoneroUtils.hexToBytes(PUBLIC_SPEND_KEY);
		assertEquals(32, bytes.length);
		assertEquals(PUBLIC_SPEND_KEY, CryptoUtils.bytesToHex(bytes));
	}
}
