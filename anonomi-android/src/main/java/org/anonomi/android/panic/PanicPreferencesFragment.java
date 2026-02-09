package org.anonomi.android.panic;

import android.os.Bundle;

import org.anonomi.R;
import org.anonomi.android.util.SecurePrefsManager;

import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_SEQUENCE;

public class PanicPreferencesFragment extends PreferenceFragmentCompat {

	private static final String KEY_RECORD_SEQUENCE = "pref_key_record_sequence";
	private static final String KEY_PANIC_ACTION_LIST = "pref_key_panic_action";

	private Preference recordSequencePref;
	private ListPreference panicActionPref;
	private SecurePrefsManager securePrefs;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.panic_preferences);

		securePrefs = new SecurePrefsManager(requireContext());

		recordSequencePref = findPreference(KEY_RECORD_SEQUENCE);
		panicActionPref = findPreference(KEY_PANIC_ACTION_LIST);

		updateSequenceDisplay();

		if (recordSequencePref != null) {
			recordSequencePref.setOnPreferenceClickListener(pref -> {
				PanicSequenceRecordDialog dialog =
						new PanicSequenceRecordDialog();
				dialog.setCallback(steps -> {
					String serialized =
							PanicSequenceDetector.serializeSequence(steps);
					securePrefs.putEncrypted(PREF_KEY_PANIC_SEQUENCE,
							serialized);
					PanicSequenceDetector.getInstance()
							.loadSequence(requireContext());
					updateSequenceDisplay();
				});
				dialog.show(getParentFragmentManager(),
						"PanicSequenceRecordDialog");
				return true;
			});
		}

		if (panicActionPref != null) {
			// Load current action value from encrypted prefs
			String currentAction =
					securePrefs.getDecrypted(PREF_KEY_PANIC_ACTION);
			if (currentAction != null) {
				panicActionPref.setValue(currentAction);
			} else {
				panicActionPref.setValue(ACTION_SIGN_OUT);
			}
			updateActionSummary(panicActionPref.getValue());

			panicActionPref.setOnPreferenceChangeListener((pref, newValue) -> {
				String value = (String) newValue;
				securePrefs.putEncrypted(PREF_KEY_PANIC_ACTION, value);
				updateActionSummary(value);
				return true;
			});
		}
	}

	private void updateSequenceDisplay() {
		if (recordSequencePref == null) return;
		String raw = securePrefs.getDecrypted(PREF_KEY_PANIC_SEQUENCE);
		if (raw != null && !raw.isEmpty()) {
			List<PanicSequenceDetector.Step> steps =
					PanicSequenceDetector.deserializeSequence(raw);
			if (!steps.isEmpty()) {
				recordSequencePref.setSummary(
						PanicSequenceDetector.toDisplayString(steps));
				return;
			}
		}
		recordSequencePref.setSummary(R.string.panic_sequence_not_set);
	}

	private void updateActionSummary(String value) {
		if (panicActionPref == null || value == null) return;
		switch (value) {
			case ACTION_SIGN_OUT:
				panicActionPref.setSummary(
						R.string.panic_action_sign_out_summary);
				break;
			case ACTION_DELETE_ACCOUNT:
				panicActionPref.setSummary(
						R.string.panic_action_delete_summary);
				break;
			case ACTION_SHOW_DIALOG:
				panicActionPref.setSummary(
						R.string.panic_action_dialog_summary);
				break;
		}
	}
}
