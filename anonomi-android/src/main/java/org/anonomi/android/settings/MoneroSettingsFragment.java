package org.anonomi.android.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;
import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.anonomi.android.xmr.AnonMoneroUtils;
import android.widget.Toast;


@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class MoneroSettingsFragment extends PreferenceFragmentCompat {

	public static final String PREF_KEY_PRIMARY_ADDRESS = "pref_key_primary_address";
	public static final String PREF_KEY_PRIVATE_VIEW_KEY = "pref_key_private_view_key";
	public static final String PREF_KEY_MONERO_RATE = "pref_key_monero_rate";

	public static final String PREF_KEY_MINOR_INDEX = "pref_key_minor_index_key";


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences_monero, rootKey);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		requireActivity().setTitle(R.string.monero_settings_title);

		SecurePrefsManager securePrefs = new SecurePrefsManager(requireContext());
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

		EditTextPreference addressPref = findPreference(PREF_KEY_PRIMARY_ADDRESS);
		EditTextPreference viewKeyPref = findPreference(PREF_KEY_PRIVATE_VIEW_KEY);
		EditTextPreference ratePref = findPreference(PREF_KEY_MONERO_RATE);
		EditTextPreference minorPref = findPreference(PREF_KEY_MINOR_INDEX);

		if (addressPref != null) {
			SecureValue address = securePrefs.read(PREF_KEY_PRIMARY_ADDRESS);
			addressPref.setSummaryProvider(new SecureSummaryProvider(
					false, address.isUnreadable()));
			if (address.isPresent()) {
				addressPref.setText(address.get());
			}

			addressPref.setOnPreferenceChangeListener((preference, newValue) -> {
				String newAddress = (String) newValue;
				if (AnonMoneroUtils.isValidMoneroAddress(newAddress)) {
					securePrefs.putEncrypted(PREF_KEY_PRIMARY_ADDRESS, newAddress);
					return true; // Accept change
				} else {
					Toast.makeText(requireContext(), R.string.invalid_monero_address, Toast.LENGTH_SHORT).show();
					return false; // Reject change
				}
			});
		}

		if (viewKeyPref != null) {
			SecureValue viewKey = securePrefs.read(PREF_KEY_PRIVATE_VIEW_KEY);
			// Set the provider before the text, so the private view key is
			// never the summary even briefly.
			viewKeyPref.setSummaryProvider(new SecureSummaryProvider(
					true, viewKey.isUnreadable()));
			if (viewKey.isPresent()) {
				viewKeyPref.setText(viewKey.get());
			}

			viewKeyPref.setOnPreferenceChangeListener((preference, newValue) -> {
				String newViewKey = (String) newValue;
				if (AnonMoneroUtils.isValidMoneroPrivateKey(newViewKey)) {
					securePrefs.putEncrypted(PREF_KEY_PRIVATE_VIEW_KEY, newViewKey);
					return true;
				} else {
					Toast.makeText(requireContext(), R.string.invalid_monero_private_view_key, Toast.LENGTH_SHORT).show();
					return false; // Reject change
				}
			});
		}

		if (minorPref != null) {
			SecureValue minorIndex = securePrefs.read(PREF_KEY_MINOR_INDEX);
			minorPref.setSummaryProvider(new SecureSummaryProvider(
					false, minorIndex.isUnreadable()));
			if (minorIndex.isPresent()) {
				minorPref.setText(minorIndex.get());
			}

			minorPref.setOnPreferenceChangeListener((preference, newValue) -> {
				String newMinorIndex = (String) newValue;
				if (isValidMinorIndex(newMinorIndex)) {
					securePrefs.putEncrypted(PREF_KEY_MINOR_INDEX, newMinorIndex);
					return true;
				} else {
					Toast.makeText(requireContext(), R.string.invalid_monero_minor_index_key, Toast.LENGTH_SHORT).show();
					return false; // Reject change
				}
			});
		}

		if (ratePref != null) {
			ratePref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
			// You might want to add validation for the Monero rate as well
			ratePref.setOnPreferenceChangeListener((preference, newValue) -> {
				// Add validation for the rate if needed (e.g., is it a valid number?)
				return true;
			});
		}
	}
	/**
	 * Summary line for a value backed by {@link SecurePrefsManager}.
	 * <p>
	 * Replaces {@link EditTextPreference.SimpleSummaryProvider} for two
	 * reasons. It renders the value itself, which for the private view key
	 * means printing the key on the settings screen where anyone looking at
	 * the device - or any screenshot - picks it up. And it has only two
	 * answers, so a value that could not be decrypted comes out as "Not
	 * set", which is the same conflation of unreadable with unset that this
	 * branch exists to remove. Here it matters twice over: told the minor
	 * index is unset, someone re-enters a low one and hands two contacts the
	 * same subaddress, which is exactly what RequestXmrActivity now refuses
	 * to do on their behalf.
	 */
	private static class SecureSummaryProvider
			implements Preference.SummaryProvider<EditTextPreference> {

		private final boolean secret;
		private final boolean unreadable;

		/**
		 * @param secret whether showing the value would disclose one.
		 * @param unreadable whether the stored value failed to decrypt. Only
		 * consulted while the preference has no text: once the user enters a
		 * value the stale read no longer describes what is stored.
		 */
		SecureSummaryProvider(boolean secret, boolean unreadable) {
			this.secret = secret;
			this.unreadable = unreadable;
		}

		@Override
		public CharSequence provideSummary(@NonNull EditTextPreference pref) {
			String text = pref.getText();
			if (text == null || text.isEmpty()) {
				return pref.getContext().getString(unreadable
						? R.string.monero_value_unreadable
						: R.string.pref_value_not_set);
			}
			return secret
					? pref.getContext().getString(R.string.pref_value_set)
					: text;
		}
	}

	private boolean isValidMinorIndex(String input) {
		if (input == null || input.isEmpty()) {
			return false;
		}
		try {
			int index = Integer.parseInt(input);
			// Non-negative, and low enough to still increment.
			return index >= 0 && index < Integer.MAX_VALUE;
		} catch (NumberFormatException e) {
			return false; // Not a valid integer
		}
	}

}