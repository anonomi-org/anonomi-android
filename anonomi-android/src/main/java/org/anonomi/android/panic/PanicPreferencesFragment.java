package org.anonomi.android.panic;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import org.anonomi.R;
import org.anonomi.android.util.SecurePrefsManager;

import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ENABLED;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_SEQUENCE;

public class PanicPreferencesFragment extends PreferenceFragmentCompat {

	private static final String KEY_PANIC_ENABLED = "pref_key_panic_enabled";
	private static final String KEY_RECORD_SEQUENCE = "pref_key_record_sequence";
	private static final String KEY_PANIC_ACTION_LIST = "pref_key_panic_action";

	private SwitchPreferenceCompat enabledPref;
	private Preference recordSequencePref;
	private ListPreference panicActionPref;
	private SecurePrefsManager securePrefs;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.panic_preferences);

		securePrefs = new SecurePrefsManager(requireContext());

		enabledPref = findPreference(KEY_PANIC_ENABLED);
		recordSequencePref = findPreference(KEY_RECORD_SEQUENCE);
		panicActionPref = findPreference(KEY_PANIC_ACTION_LIST);

		if (enabledPref != null) {
			String enabledStr = securePrefs.getDecrypted(PREF_KEY_PANIC_ENABLED);
			boolean isEnabled = enabledStr == null || "true".equals(enabledStr);
			enabledPref.setChecked(isEnabled);
			updateDependentPrefs(isEnabled);

			enabledPref.setOnPreferenceChangeListener((pref, newValue) -> {
				boolean val = (Boolean) newValue;
				if (val && isPanicConflictWithPtt()) {
					Toast.makeText(requireContext(),
							R.string.panic_conflict_ptt,
							Toast.LENGTH_LONG).show();
					return false;
				}
				securePrefs.putEncrypted(PREF_KEY_PANIC_ENABLED,
						String.valueOf(val));
				updateDependentPrefs(val);
				PanicSequenceDetector.getInstance()
						.loadSequence(requireContext());
				return true;
			});
		}

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
					// Check conflict with PTT button
					if (!steps.isEmpty()
							&& isPanicFirstStepConflict(steps)) {
						securePrefs.putEncrypted(PREF_KEY_PANIC_ENABLED,
								"false");
						if (enabledPref != null) {
							enabledPref.setChecked(false);
						}
						updateDependentPrefs(false);
						Toast.makeText(requireContext(),
								R.string.panic_conflict_ptt,
								Toast.LENGTH_LONG).show();
					}
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

	private void updateDependentPrefs(boolean enabled) {
		if (recordSequencePref != null) {
			recordSequencePref.setEnabled(enabled);
		}
		if (panicActionPref != null) {
			panicActionPref.setEnabled(enabled);
		}
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

	private int getPttKeyCode() {
		SharedPreferences prefs =
				PreferenceManager.getDefaultSharedPreferences(
						requireContext());
		String value = prefs.getString("pref_key_ptt_button", "volume_up");
		return "volume_down".equals(value)
				? KeyEvent.KEYCODE_VOLUME_DOWN
				: KeyEvent.KEYCODE_VOLUME_UP;
	}

	private boolean isPanicConflictWithPtt() {
		String raw = securePrefs.getDecrypted(PREF_KEY_PANIC_SEQUENCE);
		if (raw == null || raw.isEmpty()) return false;

		List<PanicSequenceDetector.Step> steps =
				PanicSequenceDetector.deserializeSequence(raw);
		if (steps.isEmpty()) return false;

		return steps.get(0).button == getPttKeyCode();
	}

	private boolean isPanicFirstStepConflict(
			List<PanicSequenceDetector.Step> steps) {
		if (steps.isEmpty()) return false;
		return steps.get(0).button == getPttKeyCode();
	}
}
