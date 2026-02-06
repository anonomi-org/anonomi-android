package org.anonomi.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;

import org.anonomi.R;
import org.anonomi.android.util.SecurePrefsManager;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.PreferenceFragmentCompat;

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

				if (enableStealth) {
					showSetPasscodeTwiceDialog(newPasscode -> {
						SecurePrefsManager securePrefs = new SecurePrefsManager(requireContext());
						securePrefs.putEncrypted(PREF_KEY_CALCULATOR_PASSCODE, newPasscode);

						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
						prefs.edit().putBoolean(PREF_KEY_STEALTH_MODE, true).apply();

						stealthSwitch.setChecked(true);
						enableStealthMode();

						Toast.makeText(requireContext(), R.string.stealth_mode_enabled, Toast.LENGTH_SHORT).show();
					});
					return false; // we’ll enable only after successful set+confirm
				} else {
					// Optional cleanup: remove stored passcode when stealth mode is turned off
					SecurePrefsManager securePrefs = new SecurePrefsManager(requireContext());
					securePrefs.putEncrypted(PREF_KEY_CALCULATOR_PASSCODE, "");

					disableStealthMode();
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
					prefs.edit().putBoolean(PREF_KEY_STEALTH_MODE, false).apply();
					return true;
				}
			});
		}
	}

	private interface PasscodeCallback {
		void onPasscode(String passcode);
	}

	private void showSetPasscodeTwiceDialog(PasscodeCallback onConfirmed) {
		Context context = requireContext();

		final androidx.appcompat.widget.AppCompatEditText input1 =
				new androidx.appcompat.widget.AppCompatEditText(context);
		input1.setHint(R.string.set_passcode_hint_1);
		input1.setSingleLine(true);

		final androidx.appcompat.widget.AppCompatEditText input2 =
				new androidx.appcompat.widget.AppCompatEditText(context);
		input2.setHint(R.string.set_passcode_hint_2);
		input2.setSingleLine(true);

		// Prevent keyboard suggestions/autofill learning the passcode
		hardenPasscodeInput(input1);
		hardenPasscodeInput(input2);

		android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		int pad = (int) (16 * context.getResources().getDisplayMetrics().density);
		layout.setPadding(pad, pad, pad, pad);
		layout.addView(input1);
		layout.addView(input2);

		androidx.appcompat.app.AlertDialog dialog =
				new androidx.appcompat.app.AlertDialog.Builder(context)
						.setTitle(R.string.set_passcode_title)
						.setMessage(R.string.set_passcode_message)
						.setView(layout)
						.setPositiveButton(android.R.string.ok, null) // we override later
						.setNegativeButton(android.R.string.cancel, null)
						.setCancelable(false)
						.create();

		dialog.setOnShowListener(d -> {
			android.widget.Button ok = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
			ok.setOnClickListener(v -> {
				String p1 = input1.getText() == null ? "" : input1.getText().toString().trim();
				String p2 = input2.getText() == null ? "" : input2.getText().toString().trim();

				if (!isValidExpression(p1)) {
					Toast.makeText(context, R.string.passcode_invalid, Toast.LENGTH_SHORT).show();
					return;
				}
				if (!p1.equals(p2)) {
					Toast.makeText(context, R.string.passcode_confirm_failed, Toast.LENGTH_SHORT).show();
					return;
				}

				onConfirmed.onPasscode(p1);
				dialog.dismiss();
			});
		});

		dialog.show();
	}

	private static void hardenPasscodeInput(androidx.appcompat.widget.AppCompatEditText input) {
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		input.setAutofillHints((String) null);
		input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
		input.setLongClickable(false);
		input.setTextIsSelectable(false);
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