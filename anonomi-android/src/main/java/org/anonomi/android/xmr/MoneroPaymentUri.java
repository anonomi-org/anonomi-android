package org.anonomi.android.xmr;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Nullable;

/**
 * Builds the {@code monero:} URI a wallet reads out of a payment request,
 * whether the request travelled as its own message or as text.
 */
public class MoneroPaymentUri {

	public static String build(String subaddress,
			@Nullable Long atomicAmount, @Nullable String description) {
		StringBuilder uri = new StringBuilder("monero:").append(subaddress);
		boolean hasQuery = false;
		if (atomicAmount != null) {
			uri.append("?tx_amount=")
					.append(AnonMoneroUtils.atomicUnitsToXmr(atomicAmount));
			hasQuery = true;
		}
		if (description != null && !description.isEmpty()) {
			uri.append(hasQuery ? '&' : '?')
					.append("tx_description=")
					.append(encode(description));
		}
		return uri.toString();
	}

	/**
	 * Percent-encodes a value for a query string. URLEncoder writes a space
	 * as '+', which means a space only to a form decoder, so it is put back
	 * as the escape a URI actually defines.
	 */
	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}
}
