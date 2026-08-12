package org.anonomi.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	private static final String TAG = "SecurePrefsManager";
	private static final String AES_MODE = "AES/GCM/NoPadding";
	// Existing data is encrypted under this alias; renaming it orphans that data.
	private static final String KEYSTORE_ALIAS = "AnonChatSecurePrefsKey";
	private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

	/**
	 * Set once the values in {@link #LEGACY_PLAINTEXT_KEYS} have been
	 * re-encrypted. Written through {@link #putEncrypted}, so corrupting it
	 * cannot re-open the adoption window: an unreadable flag is treated as
	 * "already migrated", which is the direction that adopts nothing.
	 * <p>
	 * Earlier builds stored this as a boolean. Reading one now raises
	 * {@link ClassCastException} inside {@link #read}, which surfaces as
	 * unreadable and so skips the migration - correct, because those installs
	 * have already run it.
	 */
	private static final String PREF_KEY_LEGACY_MIGRATED =
			"secure_prefs_plaintext_migrated";

	/**
	 * Keys whose encrypted value the androidx preference framework used to
	 * overwrite with plaintext, and which therefore have to be adopted once
	 * before this class can start rejecting plaintext.
	 * <p>
	 * These keys are shared with {@code Preference}s of the same name, and
	 * {@code androidx.preference} writes to the same file this class does
	 * ({@code PreferenceManager.getDefaultSharedPreferences}, i.e.
	 * {@code <package>_preferences}). On a settings change the framework calls
	 * the change listener - which encrypts the value - and only then calls
	 * {@code setText}/{@code setValue}, which persists the value in plaintext
	 * over the top of the ciphertext. So on any install built before this
	 * change these four keys hold plaintext, and reading them as unreadable
	 * would silently lose the user's Monero settings and, worse, turn a
	 * configured "sign out" into an unreadable panic action.
	 * <p>
	 * The preferences involved are now marked {@code persistent="false"}, so
	 * nothing writes plaintext to them any more.
	 * <p>
	 * Deliberately absent: {@code pref_key_set_calculator_passcode} and
	 * {@code pref_key_panic_sequence}. No {@code Preference} shares those
	 * keys, so they have only ever held ciphertext, and plaintext found in
	 * them was planted rather than written by us. Those are exactly the
	 * values an attacker would want to choose, so they are never adopted.
	 * <p>
	 * The names are duplicated rather than imported to keep this package from
	 * depending on the UI packages; {@code SecurePrefsLegacyKeysTest} fails if
	 * they drift from the constants they mirror.
	 */
	static final Set<String> LEGACY_PLAINTEXT_KEYS;

	static {
		Set<String> keys = new HashSet<>();
		keys.add("pref_key_primary_address");
		keys.add("pref_key_private_view_key");
		keys.add("pref_key_minor_index_key");
		keys.add("pref_key_panic_action");
		LEGACY_PLAINTEXT_KEYS = Collections.unmodifiableSet(keys);
	}

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
		migrateLegacyPlaintext();
	}

	/**
	 * Re-encrypts, once, the values that used to be stored in plaintext by
	 * the preference framework. See {@link #LEGACY_PLAINTEXT_KEYS}.
	 * <p>
	 * This is trust on first use, and it cannot be made watertight. Adoption
	 * has to trust something on the one run where it happens, and deleting
	 * the flag returns us to that run. Corrupting the flag no longer does,
	 * because unreadable means "already migrated".
	 * <p>
	 * So state the residual risk plainly rather than explaining it away.
	 * Something able to write this app's preferences file - which needs root
	 * or a sandbox compromise, as {@code allowBackup="false"} - can delete
	 * the flag, plant plaintext, and have it adopted and encrypted into a
	 * value that afterwards looks authentic. That is worse than the same
	 * attacker simply destroying the ciphertext, because destruction is now
	 * visible: it reads as unreadable, panic fails strong, and settings says
	 * so. Adoption is silent. The window is one run per install and covers
	 * only these four keys. The stealth passcode and the panic sequence are
	 * not among them - but pref_key_panic_action is, and planting sign_out
	 * there downgrades a configured deletion into the weakest action while
	 * afterwards looking authentic, which makes it the most valuable thing
	 * an adversary could put in this set.
	 */
	private void migrateLegacyPlaintext() {
		try {
			// Absent is the only state that means "not yet migrated".
			if (!read(PREF_KEY_LEGACY_MIGRATED).isAbsent()) return;
			for (String key : LEGACY_PLAINTEXT_KEYS) {
				String stored;
				try {
					stored = prefs.getString(key, null);
				} catch (ClassCastException e) {
					// Unreachable for these four: every Preference sharing
					// one of these keys stores a String. If it ever happens,
					// the value is not ours to adopt and the key will read as
					// unreadable, so close the window anyway rather than
					// leave it open for the other three.
					Log.w(TAG, "Skipping non-string legacy value for " + key);
					continue;
				}
				if (stored == null) continue;
				try {
					codec.decode(stored);
					continue; // already encrypted, nothing to do
				} catch (SecurePrefsCodec.MalformedRecordException expected) {
					// plaintext left by the preference framework
				}
				putEncrypted(key, stored);
				Log.i(TAG, "Re-encrypted legacy plaintext value for " + key);
			}
			putEncrypted(PREF_KEY_LEGACY_MIGRATED, "1");
		} catch (RuntimeException e) {
			// Leave the flag unset so this is retried, rather than losing
			// settings because the Keystore was briefly unavailable.
			Log.w(TAG, "Could not migrate legacy plaintext settings", e);
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
			return SecureValue.unreadable(e.getMessage());
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
