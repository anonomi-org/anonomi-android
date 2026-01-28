package org.anonomi.android.conversation;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.fragment.BaseFragment;
import org.anonomi.android.widget.OnboardingFullDialogFragment;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import static java.util.logging.Level.INFO;
import static org.anonomi.android.conversation.ConversationActivity.CONTACT_ID;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ConversationSettingsDialog extends DialogFragment {

	private static final Logger LOG = Logger.getLogger(ConversationSettingsDialog.class.getName());

	private static final long ONE_MINUTE = 60000;
	private static final long FIVE_MINUTES = 5 * ONE_MINUTE;
	private static final long THIRTY_MINUTES = 30 * ONE_MINUTE;
	private static final long ONE_HOUR = 60 * ONE_MINUTE;
	private static final long ONE_DAY = 24 * ONE_HOUR;
	private static final long THREE_DAYS = 3 * ONE_DAY;
	private static final long SEVEN_DAYS = 7 * ONE_DAY;

	private static final long[] DURATIONS = new long[]{
			ONE_MINUTE, FIVE_MINUTES, THIRTY_MINUTES, ONE_HOUR,
			ONE_DAY, THREE_DAYS, SEVEN_DAYS
	};

	public static final String TAG = "ConversationSettingsDialog";

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private ConversationViewModel viewModel;

	static ConversationSettingsDialog newInstance(ContactId contactId) {
		Bundle args = new Bundle();
		args.putInt(CONTACT_ID, contactId.getInt());
		ConversationSettingsDialog dialog = new ConversationSettingsDialog();
		dialog.setArguments(args);
		return dialog;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		injectFragment(((BaseFragment.BaseFragmentListener) context).getActivityComponent());
	}

	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(ConversationViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AnonFullScreenDialogTheme);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_conversation_settings, container, false);

		Bundle args = requireArguments();
		int id = args.getInt(CONTACT_ID, -1);
		if (id == -1) throw new IllegalStateException();
		ContactId contactId = new ContactId(id);

		viewModel.setContactId(contactId);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		SwitchCompat switchDisappearingMessages = view.findViewById(R.id.switchDisappearingMessages);
		switchDisappearingMessages.setOnCheckedChangeListener((button, value) ->
				viewModel.setAutoDeleteTimerEnabled(value));

		TextView textDurationLabel = view.findViewById(R.id.textDurationLabel);
		Spinner spinner = view.findViewById(R.id.spinnerDisappearingDuration);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
				R.array.auto_delete_durations, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setEnabled(false);

		// Used to prevent triggering listener when setting selection programmatically
		final boolean[] skipSelection = {false};

		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
				if (skipSelection[0]) {
					LOG.warning("[UI] Skipping onItemSelected caused by programmatic setSelection");
					skipSelection[0] = false;
					return;
				}
				long duration = DURATIONS[position];
				if (LOG.isLoggable(INFO)) {
					LOG.info("[UI] User selected duration: " + duration);
				}
				viewModel.setAutoDeleteTimer(duration);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		viewModel.getAutoDeleteTimer().observe(getViewLifecycleOwner(), timer -> {
			if (LOG.isLoggable(INFO)) {
				LOG.info("[UI] Received auto-delete timer: " + timer);
			}

			boolean enabled = timer != NO_AUTO_DELETE_TIMER;
			switchDisappearingMessages.setChecked(enabled);
			switchDisappearingMessages.setEnabled(true);
			spinner.setEnabled(enabled);
			textDurationLabel.setVisibility(enabled ? View.VISIBLE : View.GONE);
			spinner.setVisibility(enabled ? View.VISIBLE : View.GONE);

			int index = -1;
			for (int i = 0; i < DURATIONS.length; i++) {
				if (DURATIONS[i] == timer) {
					index = i;
					break;
				}
			}
			if (index == -1) {
				LOG.warning("[UI] Timer value " + timer + " not found in predefined durations.");
				long closest = DURATIONS[0];
				long diff = Math.abs(timer - closest);
				for (long d : DURATIONS) {
					long newDiff = Math.abs(timer - d);
					if (newDiff < diff) {
						closest = d;
						diff = newDiff;
					}
				}
				for (int i = 0; i < DURATIONS.length; i++) {
					if (DURATIONS[i] == closest) {
						index = i;
						break;
					}
				}
			}

			if (index != -1) {
				skipSelection[0] = true;
				spinner.setSelection(index);
			}
		});

		Button buttonLearnMore = view.findViewById(R.id.buttonLearnMore);
		buttonLearnMore.setOnClickListener(e -> showLearnMoreDialog());

		return view;
	}

	private void showLearnMoreDialog() {
		OnboardingFullDialogFragment.newInstance(
						R.string.disappearing_messages_title,
						R.string.disappearing_messages_explanation_long)
				.show(getChildFragmentManager(), OnboardingFullDialogFragment.TAG);
	}
}