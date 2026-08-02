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
