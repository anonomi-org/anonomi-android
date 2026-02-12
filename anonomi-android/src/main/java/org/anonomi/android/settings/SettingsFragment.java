package org.anonomi.android.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import org.anonomi.R;
import org.anonomi.android.mailbox.MailboxActivity;
import org.anonomi.android.panic.PanicSequenceDetector;
import org.anonomi.android.util.SecurePrefsManager;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.List;

import javax.inject.Inject;


import androidx.activity.result.contract.ActivityResultContracts;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import static java.util.Objects.requireNonNull;
import static org.anonomi.android.AppModule.getAndroidComponent;
import static org.anonomi.android.TestingConstants.IS_DEBUG_BUILD;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SettingsFragment extends PreferenceFragmentCompat {

	public static final String SETTINGS_NAMESPACE = "android-ui";

	private static final String PREF_KEY_AVATAR = "pref_key_avatar";
	private static final String PREF_KEY_SHARE_LINK = "pref_key_share_app_link";
	private static final String PREF_KEY_FEEDBACK = "pref_key_send_feedback";
	private static final String PREF_KEY_DEV = "pref_key_dev";
	private static final String PREF_KEY_EXPLODE = "pref_key_explode";
	private static final String PREF_KEY_MAILBOX = "pref_key_mailbox";
	private static final String PREF_KEY_PTT_BUTTON = "pref_key_ptt_button";

	private static final String DOWNLOAD_URL = "https://anon/download/";

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private SettingsViewModel viewModel;
	private AvatarPreference prefAvatar;

	private final ActivityResultLauncher<String> contentLauncher =
			registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		getAndroidComponent(context).inject(this);
		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
				.get(SettingsViewModel.class);
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.settings);

		prefAvatar = requireNonNull(findPreference(PREF_KEY_AVATAR));
		if (viewModel.shouldEnableProfilePictures()) {
			prefAvatar.setOnPreferenceClickListener(preference -> {
				contentLauncher.launch("image/*");
				return true;
			});
		} else {
			prefAvatar.setVisible(false);
		}

		Preference prefMailbox = requireNonNull(findPreference(PREF_KEY_MAILBOX));
		prefMailbox.setOnPreferenceClickListener(preference -> {
			Intent i = new Intent(requireContext(), MailboxActivity.class);
			startActivity(i);
			return true;
		});

		Preference prefMonero = findPreference("pref_key_monero_settings");
		if (prefMonero != null) {
			prefMonero.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(requireContext(), MoneroSettingsActivity.class);
				startActivity(intent);
				return true;
			});
		}

		// Hide the Share App Link preference
		Preference prefShareLink = findPreference(PREF_KEY_SHARE_LINK);
		if (prefShareLink != null) {
			prefShareLink.setVisible(false);
		}

		// Hide the Send Feedback preference
		Preference prefFeedback = findPreference(PREF_KEY_FEEDBACK);
		if (prefFeedback != null) {
			prefFeedback.setVisible(false);
		}

		// PTT button preference
		ListPreference pttPref = findPreference(PREF_KEY_PTT_BUTTON);
		if (pttPref != null) {
			updatePttSummary(pttPref, pttPref.getValue());
			pttPref.setOnPreferenceChangeListener((pref, newValue) -> {
				String value = (String) newValue;
				if (isPttConflictWithPanic(value)) {
					Toast.makeText(requireContext(),
							R.string.ptt_conflict_message,
							Toast.LENGTH_LONG).show();
					return false;
				}
				updatePttSummary((ListPreference) pref, value);
				return true;
			});
		}

		// Developer-only crash option
		Preference explode = requireNonNull(findPreference(PREF_KEY_EXPLODE));
		if (IS_DEBUG_BUILD) {
			explode.setOnPreferenceClickListener(preference -> {
				throw new RuntimeException("Boom!");
			});
		} else {
			PreferenceGroup dev = requireNonNull(findPreference(PREF_KEY_DEV));
			dev.setVisible(false);
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewModel.getOwnIdentityInfo().observe(getViewLifecycleOwner(),
				us -> prefAvatar.setOwnIdentityInfo(us));
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.settings_button);
	}

	private void onImageSelected(@Nullable Uri uri) {
		if (uri == null) return;
		DialogFragment dialog = ConfirmAvatarDialogFragment.newInstance(uri);
		dialog.show(getParentFragmentManager(), ConfirmAvatarDialogFragment.TAG);
	}

	private void updatePttSummary(ListPreference pref, String value) {
		if ("volume_down".equals(value)) {
			pref.setSummary(R.string.ptt_button_volume_down);
		} else {
			pref.setSummary(R.string.ptt_button_volume_up);
		}
	}

	private boolean isPttConflictWithPanic(String pttValue) {
		SecurePrefsManager securePrefs =
				new SecurePrefsManager(requireContext());
		String enabledStr = securePrefs.getDecrypted(
				PanicSequenceDetector.PREF_KEY_PANIC_ENABLED);
		boolean panicEnabled =
				enabledStr == null || "true".equals(enabledStr);
		if (!panicEnabled) return false;

		String raw = securePrefs.getDecrypted(
				PanicSequenceDetector.PREF_KEY_PANIC_SEQUENCE);
		if (raw == null || raw.isEmpty()) return false;

		List<PanicSequenceDetector.Step> steps =
				PanicSequenceDetector.deserializeSequence(raw);
		if (steps.isEmpty()) return false;

		int firstButton = steps.get(0).button;
		int pttKeyCode = "volume_down".equals(pttValue)
				? KeyEvent.KEYCODE_VOLUME_DOWN
				: KeyEvent.KEYCODE_VOLUME_UP;
		return firstButton == pttKeyCode;
	}
}