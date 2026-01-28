package org.anonomi.android.settings;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import org.anonomi.android.util.SecurePrefsManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.EditTextPreference;

import static java.util.Objects.requireNonNull;
import static org.anonomi.android.AppModule.getAndroidComponent;
import static org.anonomi.android.settings.SettingsActivity.enableAndPersist;
import static org.anonomi.android.util.UiUtils.hasScreenLock;
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SecurityFragment extends PreferenceFragmentCompat {

	public static final String PREF_SCREEN_LOCK = "pref_key_lock";
	public static final String PREF_SCREEN_LOCK_TIMEOUT = "pref_key_lock_timeout";
	public static final String PREF_KEY_STEALTH_MODE = "pref_key_stealth_mode";

	public static final String PREF_KEY_CALCULATOR_PASSCODE = "pref_key_set_calculator_passcode";

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private SettingsViewModel viewModel;
	private SwitchPreferenceCompat screenLock;
	private ListPreference screenLockTimeout;

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		getAndroidComponent(context).inject(this);
		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
				.get(SettingsViewModel.class);
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.settings_security);
		getPreferenceManager().setPreferenceDataStore(viewModel.settingsStore);

		screenLock = findPreference(PREF_SCREEN_LOCK);
		screenLockTimeout = requireNonNull(findPreference(PREF_SCREEN_LOCK_TIMEOUT));

		screenLockTimeout.setSummaryProvider(preference -> {
			CharSequence timeout = screenLockTimeout.getValue();
			String never = getString(R.string.pref_lock_timeout_value_never);
			if (timeout.equals(never)) {
				return getString(R.string.pref_lock_timeout_never_summary);
			} else {
				return getString(R.string.pref_lock_timeout_summary, screenLockTimeout.getEntry());
			}
		});

		// Set up stealth mode switch listener
		SwitchPreferenceCompat stealthSwitch = findPreference(PREF_KEY_STEALTH_MODE);
		if (stealthSwitch != null) {
			stealthSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
				boolean enableStealth = (Boolean) newValue;

				SecurePrefsManager securePrefs = new SecurePrefsManager(requireContext());
				String decryptedPasscode = securePrefs.getDecrypted(PREF_KEY_CALCULATOR_PASSCODE);

				if (enableStealth) {
					if (decryptedPasscode == null || decryptedPasscode.trim().isEmpty()) {
						Toast.makeText(requireContext(), R.string.no_passcode_set_summary, Toast.LENGTH_SHORT).show();
						return false;  // block enabling stealth mode
					} else {
						showPasscodeDialog(decryptedPasscode, () -> {
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
							prefs.edit().putBoolean(PREF_KEY_STEALTH_MODE, true).apply();
							stealthSwitch.setChecked(true);
							enableStealthMode();
						});
						return false;  // we’ll handle enabling after dialog
					}
				} else {
					disableStealthMode();
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
					prefs.edit().putBoolean(PREF_KEY_STEALTH_MODE, false).apply();
					return true;
				}

//				// Save preference immediately
//				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
//				prefs.edit().putBoolean(PREF_KEY_STEALTH_MODE, enableStealth).apply();
//				return true;
			});
		}

		EditTextPreference passcodePref = findPreference(PREF_KEY_CALCULATOR_PASSCODE);
		if (passcodePref != null) {
			SecurePrefsManager securePrefs = new SecurePrefsManager(requireContext());
			String decryptedPasscode = securePrefs.getDecrypted(PREF_KEY_CALCULATOR_PASSCODE);
			if (decryptedPasscode != null) {
				passcodePref.setText(decryptedPasscode);  // prefill value
			}

			// Add the hint text above the input field
			passcodePref.setDialogMessage(R.string.set_calculator_passcode_summary);

			// Let the SummaryProvider automatically show the current value or fallback
			passcodePref.setSummaryProvider(preference -> {
				String value = ((EditTextPreference) preference).getText();
				if (value == null || value.trim().isEmpty()) {
					return getString(R.string.no_passcode_set_summary);
				} else {
					return getString(R.string.current_passcode_summary, value);
				}
			});

			passcodePref.setPersistent(false);  // we handle storage ourselves

			passcodePref.setOnPreferenceChangeListener((preference, newValue) -> {
				String expression = (String) newValue;
				if (isValidExpression(expression)) {
					securePrefs.putEncrypted(PREF_KEY_CALCULATOR_PASSCODE, expression);
					Toast.makeText(requireContext(), R.string.passcode_saved_success, Toast.LENGTH_SHORT).show();
					return true;  // accept change, updates summary automatically
				} else {
					Toast.makeText(requireContext(), R.string.passcode_invalid, Toast.LENGTH_SHORT).show();
					return false;  // reject change
				}
			});
		}
	}

	private void showPasscodeDialog(String passcode, Runnable onOk) {
		Context context = requireContext();

		int accentColor = context.getResources().getColor(R.color.colorAccent, null);

		android.widget.TextView passcodeView = new android.widget.TextView(context);
		passcodeView.setText(passcode);
		passcodeView.setTextSize(24);
		passcodeView.setTextColor(accentColor);
		passcodeView.setPadding(40, 40, 40, 40);
		passcodeView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

		new androidx.appcompat.app.AlertDialog.Builder(context)
				.setTitle(R.string.show_passcode_dialog_title)
				.setView(passcodeView)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					onOk.run();
					Toast.makeText(context, R.string.stealth_mode_enabled, Toast.LENGTH_SHORT).show();
				})
				.setCancelable(false)
				.show();
	}

	private boolean isValidExpression(String expr) {
		if (expr == null) return false;

		String cleaned = expr.replaceAll("\\s+", "");

		// Must contain at least one operator
		if (!cleaned.matches(".*[+\\-*/%].*")) {
			return false;
		}

		// Must only contain valid characters (digits, operators, parentheses, dot)
		if (!cleaned.matches("[0-9+\\-*/%().]+")) {
			return false;
		}

		// Optionally: require at least two numbers (basic check)
		String[] numbers = cleaned.split("[+\\-*/%]");
		int numberCount = 0;
		for (String part : numbers) {
			if (!part.isEmpty()) numberCount++;
		}
		if (numberCount < 2) {
			return false;  // e.g., rejects just "2+" or "2"
		}

		return true;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// timeout depends on screenLock and gets disabled automatically
		LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
		viewModel.getScreenLockTimeout().observe(lifecycleOwner, value -> {
			screenLockTimeout.setValue(value);
			enableAndPersist(screenLockTimeout);
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.security_settings_title);
		checkScreenLock();
	}

	private void checkScreenLock() {
		LifecycleOwner lifecycleOwner = getViewLifecycleOwner();
		viewModel.getScreenLockEnabled().removeObservers(lifecycleOwner);
		if (hasScreenLock(requireActivity())) {
			viewModel.getScreenLockEnabled().observe(lifecycleOwner, on -> {
				screenLock.setChecked(on);
				enableAndPersist(screenLock);
			});
			screenLock.setSummary(R.string.pref_lock_summary);
		} else {
			screenLock.setEnabled(false);
			screenLock.setPersistent(false);
			screenLock.setChecked(false);
			screenLock.setSummary(R.string.pref_lock_disabled_summary);
		}
	}

	private void setStateIfNeeded(PackageManager pm, ComponentName cn, int state) {
		if (pm.getComponentEnabledSetting(cn) != state) {
			pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP);
		}
	}

	private ComponentName splash() {
		return new ComponentName(requireContext().getPackageName(),
				"org.anonomi.android.splash.SplashScreenActivity");
	}

	private ComponentName calcAlias() {
		return new ComponentName(requireContext().getPackageName(),
				"org.anonomi.android.splash.CalculatorAlias");
	}

	private void enableStealthMode() {
		PackageManager pm = requireContext().getPackageManager();

		// 1) Enable the new launcher FIRST
		setStateIfNeeded(pm, calcAlias(), PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

		// 2) Disable the old launcher AFTER a tick (prevents task teardown on A15)
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
				setStateIfNeeded(pm, splash(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
		);
	}

	private void disableStealthMode() {
		PackageManager pm = requireContext().getPackageManager();

		// 1) Enable the normal launcher FIRST
		setStateIfNeeded(pm, splash(), PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

		// 2) Then disable the calculator alias
		new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
				setStateIfNeeded(pm, calcAlias(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
		);
	}

}