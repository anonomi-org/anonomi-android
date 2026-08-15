package org.anonomi.android.xmr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MoneroPaymentUriTest {

	private static final String SUBADDRESS =
			"87i7kA61fNvMboXiYWHVygPAggKJPETFqLXXcdH4mQTrECvrTxZMtt6e6owj1k8j" +
					"UVjNR11eBuBMWHFBtxAwEVcm9dcSUxr";

	@Test
	public void testBuildsAddressOnlyUri() {
		assertEquals("monero:" + SUBADDRESS,
				MoneroPaymentUri.build(SUBADDRESS, null, null));
		assertEquals("monero:" + SUBADDRESS,
				MoneroPaymentUri.build(SUBADDRESS, null, ""));
	}

	/**
	 * The amount goes into the URI as XMR, since that is what a wallet
	 * reads, even though it travels between us as atomic units.
	 */
	@Test
	public void testWritesTheAmountAsXmr() {
		assertEquals("monero:" + SUBADDRESS + "?tx_amount=0.1",
				MoneroPaymentUri.build(SUBADDRESS, 100_000_000_000L, null));
		assertEquals("monero:" + SUBADDRESS + "?tx_amount=2.5",
				MoneroPaymentUri.build(SUBADDRESS, 2_500_000_000_000L, null));
	}

	@Test
	public void testEscapesTheDescription() {
		assertEquals("monero:" + SUBADDRESS +
						"?tx_amount=0.1&tx_description=Payment%20for%20service",
				MoneroPaymentUri.build(SUBADDRESS, 100_000_000_000L,
						"Payment for service"));
		// A space is the escape a URI defines, not the one a form decoder
		// would read
		assertEquals("monero:" + SUBADDRESS + "?tx_description=a%20%26%20b",
				MoneroPaymentUri.build(SUBADDRESS, null, "a & b"));
	}
}
