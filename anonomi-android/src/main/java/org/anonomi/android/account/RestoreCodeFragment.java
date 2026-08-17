package org.anonomi.android.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonomi.R;
import org.anonomi.android.account.SetupViewModel.RestorePhase;
import org.anonomi.android.account.SetupViewModel.RestoreProgress;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.anonomi.android.util.UiUtils.hideSoftKeyboard;

/**
 * Takes the recovery code and unpacks the backup with it. The unpacking is
 * shown here rather than on a screen of its own, so that going back from what
 * comes next lands on the code again.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RestoreCodeFragment extends RestoreFragment {

	private static final String TAG = RestoreCodeFragment.class.getName();

	private TextInputLayout codeWrapper;
	private TextInputEditText codeEntry;
	private Button continueButton;
	private View progressLayout;
	private LinearProgressIndicator progressBar;
	private TextView phaseTextView;

	public static RestoreCodeFragment newInstance() {
		return new RestoreCodeFragment();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	protected String getHelpText() {
		return getString(R.string.restore_code_text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_restore_code, container,
				false);

		codeWrapper = v.findViewById(R.id.codeEntryWrapper);
		codeEntry = v.findViewById(R.id.codeEntry);
		continueButton = v.findViewById(R.id.continueButton);
		progressLayout = v.findViewById(R.id.progressLayout);
		progressBar = v.findViewById(R.id.progressBar);
		phaseTextView = v.findViewById(R.id.phaseTextView);

		codeEntry.addTextChangedListener(this);
		continueButton.setOnClickListener(this);

		viewModel.getIsReadingBackup()
				.observe(getViewLifecycleOwner(), this::onReadingChanged);
		viewModel.getRestoreProgress()
				.observe(getViewLifecycleOwner(), this::onProgress);

		return v;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before,
			int count) {
		// Nothing is checked until a whole code has been typed, so that
		// typing one does not report a mistake at every digit
		boolean complete = RecoveryCode.normalise(s.toString()) != null;
		continueButton.setEnabled(complete);
		codeEntry.setOnEditorActionListener(complete ? this : null);
	}

	@Override
	public void onClick(View view) {
		hideSoftKeyboard(codeEntry);
		viewModel.readBackup(codeEntry.getText().toString());
	}

	private void onReadingChanged(boolean reading) {
		// An earlier attempt can have left the bar full, and the mode can
		// only be changed while it is off screen, which it still is here
		if (reading) progressBar.setIndeterminate(true);
		codeWrapper.setEnabled(!reading);
		codeEntry.setEnabled(!reading);
		continueButton.setVisibility(reading ? INVISIBLE : VISIBLE);
		progressLayout.setVisibility(reading ? VISIBLE : GONE);
	}

	private void onProgress(RestoreProgress p) {
		if (p.phase == RestorePhase.OPENING) {
			// Deriving the key reports nothing, so the bar stays on the
			// indeterminate animation the layout starts with
			phaseTextView.setText(R.string.restore_reading_opening);
			return;
		}
		phaseTextView.setText(R.string.restore_reading_unpacking);
		if (p.total <= 0) return;
		progressBar.setMax(100);
		// Takes the indicator out of indeterminate mode by finishing the
		// current cycle first, rather than cutting the animation off
		progressBar.setProgressCompat((int) (p.done * 100 / p.total), true);
	}
}
