package org.anonomi.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores small settings encrypted with an AES-256-GCM key held in the Android
 * Keystore, in the default {@link SharedPreferences} file.
 * <p>
 * Reads return a {@link SecureValue} rather than a nullable string, so callers
 * can tell "never set" from "could not be read". See {@link SecureValue} for
 * why that distinction is load-bearing.
 */
public class SecurePrefsManager {

	private static final String AES_MODE = "AES/GCM/NoPadding";
	private static final String KEYSTORE_ALIAS = "AnonChatSecurePrefsKey";
	private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

	private static final SecurePrefsCodec.Base64Codec ANDROID_BASE64 =
			new SecurePrefsCodec.Base64Codec() {

				@Override
				public String encode(byte[] bytes) {
					return Base64.encodeToString(bytes, Base64.NO_WRAP);
				}

				@Override
				public byte[] decode(String encoded) {
					return Base64.decode(encoded, Base64.NO_WRAP);
				}
			};

	private final SharedPreferences prefs;
	private final KeyStore keyStore;
	private final SecurePrefsCodec codec;

	public SecurePrefsManager(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		codec = new SecurePrefsCodec(ANDROID_BASE64);
		try {
			keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
			keyStore.load(null);

			// Check if key exists, generate if missing
			if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
				KeyGenerator keyGenerator = KeyGenerator.getInstance(
						KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
				);
				keyGenerator.init(new KeyGenParameterSpec.Builder(
								KEYSTORE_ALIAS,
								KeyProperties.PURPOSE_ENCRYPT |
										KeyProperties.PURPOSE_DECRYPT
						)
								.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
								.setEncryptionPaddings(
										KeyProperties.ENCRYPTION_PADDING_NONE)
								.setKeySize(256)
								.build()
				);
				keyGenerator.generateKey();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize SecurePrefsManager", e);
		}
	}

	public void putEncrypted(String keyName, String value) {
		try {
			Cipher cipher = Cipher.getInstance(AES_MODE);
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
			byte[] iv = cipher.getIV();
			byte[] encrypted =
					cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

			prefs.edit()
					.putString(keyName, codec.encode(iv, encrypted))
					.apply();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads one entry, distinguishing absent from unreadable. Never returns
	 * null.
	 */
	public SecureValue read(String keyName) {
		String stored;
		try {
			stored = prefs.getString(keyName, null);
		} catch (ClassCastException e) {
			// The androidx preference framework shares this preferences file,
			// so a key can hold a value of another type written by a
			// Preference with the same key.
			return SecureValue.unreadable("stored value is not a string");
		}
		if (stored == null) return SecureValue.absent();

		SecurePrefsCodec.Record record;
		try {
			record = codec.decode(stored);
		} catch (SecurePrefsCodec.MalformedRecordException e) {
			// TODO: this trusts any value that is not in our record format,
			// which means anything that can write to the preferences file can
			// choose what we return. Preserved here only so that extracting
			// the codec changes no behaviour; bounded in the following
			// commit.
			return SecureValue.present(stored);
		}
		try {
			return SecureValue.present(decrypt(record));
		} catch (GeneralSecurityException | RuntimeException e) {
			return SecureValue.unreadable("decryption failed");
		}
	}

	private String decrypt(SecurePrefsCodec.Record record)
			throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance(AES_MODE);
		GCMParameterSpec spec = new GCMParameterSpec(
				SecurePrefsCodec.GCM_TAG_LENGTH_BITS, record.getIv());
		cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);
		byte[] decrypted = cipher.doFinal(record.getCiphertext());
		return new String(decrypted, StandardCharsets.UTF_8);
	}

	private SecretKey getSecretKey() throws GeneralSecurityException {
		KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
		if (!(entry instanceof KeyStore.SecretKeyEntry)) {
			throw new UnrecoverableKeyException(
					"No secret key for alias " + KEYSTORE_ALIAS);
		}
		return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
	}
}
