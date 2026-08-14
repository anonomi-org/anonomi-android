package org.anonomi.android.util;

import org.junit.Test;

import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ENABLED;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_SEQUENCE;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_MINOR_INDEX;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_MONERO_RATE;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_PRIMARY_ADDRESS;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_PRIVATE_VIEW_KEY;
import static org.anonomi.android.settings.SecurityFragment.PREF_KEY_CALCULATOR_PASSCODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Which keys are allowed to have plaintext adopted, once, on upgrade.
 * <p>
 * These are compile-time String constants, so referring to them here inlines
 * the literals and does not load any Android class.
 */
public class SecurePrefsLegacyKeysTest {

	/**
	 * These keys are not shared with any {@code Preference}, so they have
	 * only ever held ciphertext. Plaintext found in them was planted, and
	 * adopting it would let whoever planted it choose the stealth-mode
	 * passcode or silently replace the panic sequence.
	 */
	@Test
	public void secretsWrittenOnlyByThisClassAreNeverAdoptedFromPlaintext() {
		assertFalse("A planted plaintext stealth passcode would be trusted",
				SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
						.contains(PREF_KEY_CALCULATOR_PASSCODE));
		assertFalse("A planted plaintext panic sequence would be trusted",
				SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
						.contains(PREF_KEY_PANIC_SEQUENCE));
		// Marked persistent="false" in panic_preferences.xml since before
		// this change, so it never held plaintext either.
		assertFalse(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_PANIC_ENABLED));
	}

	/**
	 * These four share a key with a {@code Preference}, which persisted the
	 * value in plaintext over our ciphertext. Dropping them from the list
	 * would lose the user's Monero settings on upgrade and, for the panic
	 * action, turn a configured sign-out into an unreadable value that now
	 * resolves to deleting the account.
	 */
	@Test
	public void keysThePreferenceFrameworkOverwroteAreAdoptedOnce() {
		assertTrue(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_PRIMARY_ADDRESS));
		assertTrue(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_PRIVATE_VIEW_KEY));
		assertTrue(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_MINOR_INDEX));
		assertTrue(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_PANIC_ACTION));
	}

	/**
	 * The list is spelled out in SecurePrefsManager rather than importing the
	 * UI packages, so it can drift. This is what catches that.
	 */
	@Test
	public void theListIsExactlyThoseFourKeys() {
		assertEquals(4, SecurePrefsManager.LEGACY_PLAINTEXT_KEYS.size());
	}

	/**
	 * SecurePrefsManager spells this one out too, so that it can move the value
	 * into the disguise file without importing the UI package. A name that
	 * drifted would move nothing and leave the passcode where it was.
	 */
	@Test
	public void theKeyMovedToTheDisguiseFileIsTheOneTheUiWrites() {
		assertEquals(PREF_KEY_CALCULATOR_PASSCODE,
				SecurePrefsManager.PREF_KEY_CALCULATOR_PASSCODE);
	}

	/**
	 * The Monero rate is never encrypted - RequestXmrActivity reads it
	 * straight from SharedPreferences - so it must stay out of this.
	 */
	@Test
	public void thePlaintextMoneroRateIsNotInvolved() {
		assertFalse(SecurePrefsManager.LEGACY_PLAINTEXT_KEYS
				.contains(PREF_KEY_MONERO_RATE));
	}
}
