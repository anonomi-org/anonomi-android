package org.anonomi.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;

public class SecurePrefsManager {

	private static final String ENCRYPTION_KEY_PREF = "encryption_key";
	private static final String AES_MODE = "AES/GCM/NoPadding";
	private static final String KEYSTORE_ALIAS = "AnonChatSecurePrefsKey";
	private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
	private final SharedPreferences prefs;
	private final KeyStore keyStore;
	private final Context context;

	public SecurePrefsManager(Context context) {
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
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
								KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
						)
								.setBlockModes(KeyProperties.BLOCK_MODE_GCM)
								.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
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
			SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null)).getSecretKey();
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] iv = cipher.getIV();
			byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

			String data = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
					Base64.encodeToString(encrypted, Base64.NO_WRAP);

			prefs.edit().putString(keyName, data).apply();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getDecrypted(String keyName) {
		try {
			String data = prefs.getString(keyName, null);
			if (data == null) return null;

			if (!data.contains(":")) {
				// Handle the case where the data is not encrypted
				// You might want to return the data as is, or handle it differently
				// depending on your application's logic.
				return data; // Or perhaps log a warning and return null
			}

			String[] parts = data.split(":");
			if (parts.length != 2) {
				// Handle unexpected format (e.g., multiple colons)
				//Log.w("SecurePrefsManager", "Unexpected data format for key: " + keyName + ", data: " + data);
				return null;
			}

			byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
			byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

			Cipher cipher = Cipher.getInstance(AES_MODE);
			GCMParameterSpec spec = new GCMParameterSpec(128, iv);
			SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null)).getSecretKey();
			cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

			byte[] decrypted = cipher.doFinal(encrypted);
			return new String(decrypted, StandardCharsets.UTF_8);

		} catch (Exception e) {
			//Log.e("SecurePrefsManager", "Error decrypting preference " + keyName, e);
			return null;
		}
	}

	private byte[] generateRandomKey() {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			SecretKey secretKey = keyGen.generateKey();
			return secretKey.getEncoded();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}