package org.anonomi.android.xmr;

import android.text.TextUtils;
import java.util.regex.Pattern;
import android.util.Log;


public class AnonMoneroUtils {

	// Basic Monero address pattern (can be made more specific)
	private static final Pattern MONERO_ADDRESS_PATTERN =
			Pattern.compile("^4[0-9AB][1-9A-HJ-NP-Za-km-z]{93}$");

	// Basic Monero private view key pattern (64 hexadecimal characters)
	private static final Pattern MONERO_PRIVATE_VIEW_KEY_PATTERN =
			Pattern.compile("^[0-9a-fA-F]{64}$");

	public static boolean isValidMoneroAddress(String address) {
		return !TextUtils.isEmpty(address) && MONERO_ADDRESS_PATTERN.matcher(address).matches();
	}

	public static boolean isValidMoneroPrivateKey(String privateKey) {
		return !TextUtils.isEmpty(privateKey) && MONERO_PRIVATE_VIEW_KEY_PATTERN.matcher(privateKey).matches();
	}

	public static MoneroDecodedAddress decodeAddress(String address) {
		byte[] decoded = MoneroBase58.decode(address);
		if (decoded.length != 69) { // 1 + 32 + 32 + 4 = 69 bytes
			throw new IllegalArgumentException("Invalid decoded address length: " + decoded.length);
		}

		byte[] publicSpendKey = new byte[32];
		byte[] publicViewKey = new byte[32];
		System.arraycopy(decoded, 1, publicSpendKey, 0, 32);
		System.arraycopy(decoded, 33, publicViewKey, 0, 32);
		Log.d("AnonMoneroUtils", "Decoded address length: " + decoded.length);
		Log.d("AnonMoneroUtils", "Public Spend Key: " + CryptoUtils.bytesToHex(publicSpendKey));
		Log.d("AnonMoneroUtils", "Public View Key: " + CryptoUtils.bytesToHex(publicViewKey));
		return new MoneroDecodedAddress(publicSpendKey, publicViewKey);
	}
}