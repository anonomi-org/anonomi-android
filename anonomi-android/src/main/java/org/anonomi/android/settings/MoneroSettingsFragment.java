package org.anonomi.android.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;
import org.anonomi.android.util.SecurePrefsManager;
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
			String decryptedAddress = securePrefs.getDecrypted(PREF_KEY_PRIMARY_ADDRESS);
			if (decryptedAddress != null) {
				addressPref.setText(decryptedAddress);
			}
			addressPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

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
			String decryptedViewKey = securePrefs.getDecrypted(PREF_KEY_PRIVATE_VIEW_KEY);
			if (decryptedViewKey != null) {
				viewKeyPref.setText(decryptedViewKey);
			}
			viewKeyPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

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
			String decryptedMinorIndex = securePrefs.getDecrypted(PREF_KEY_MINOR_INDEX);
			if (decryptedMinorIndex != null) {
				minorPref.setText(decryptedMinorIndex);
			}
			minorPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

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
	private boolean isValidMinorIndex(String input) {
		if (input == null || input.isEmpty()) {
			return false;
		}
		try {
			int index = Integer.parseInt(input);
			return index >= 0; // Minor index should be a non-negative integer
		} catch (NumberFormatException e) {
			return false; // Not a valid integer
		}
	}

}