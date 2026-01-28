package org.anonomi.android.xmr;

public class MoneroDecodedAddress {
	private final byte[] publicSpendKey;
	private final byte[] publicViewKey;

	public MoneroDecodedAddress(byte[] publicSpendKey, byte[] publicViewKey) {
		this.publicSpendKey = publicSpendKey;
		this.publicViewKey = publicViewKey;
	}

	public byte[] getPublicSpendKey() {
		return publicSpendKey;
	}

	public byte[] getPublicViewKey() {
		return publicViewKey;
	}
}