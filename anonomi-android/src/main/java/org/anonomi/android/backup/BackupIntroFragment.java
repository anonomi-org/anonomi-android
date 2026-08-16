package org.anonomi.android.backup;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.material.textfield.TextInputLayout;

import org.anonchatsecure.bramble.api.crypto.DecryptionResult;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.anonchatsecure.bramble.api.crypto.DecryptionResult.KEY_STRENGTHENER_ERROR;
import static org.anonchatsecure.bramble.api.crypto.DecryptionResult.SUCCESS;
import static org.anonomi.android.AppModule.getAndroidComponent;
import static org.anonomi.android.login.LoginUtils.createKeyStrengthenerErrorDialog;
import static org.anonomi.android.util.UiUtils.hideSoftKeyboard;
import static org.anonomi.android.util.UiUtils.setError;

/**
 * Says what a backup is and what it costs to lose one, and asks for the
 * account password before any of it starts.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupIntroFragment extends Fragment {

	static final String TAG = BackupIntroFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BackupViewModel viewModel;
	private TextInputLayout passwordWrapper;
	private EditText password;
	private Button continueButton;
	private ProgressBar progress;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		FragmentActivity activity = requireActivity();
		getAndroidComponent(activity).inject(this);
		viewModel = new ViewModelProvider(activity, viewModelFactory)
				.get(BackupViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_backup_intro, container,
				false);

		passwordWrapper = v.findViewById(R.id.passwordEntryWrapper);
		password = v.findViewById(R.id.passwordEntry);
		continueButton = v.findViewById(R.id.continueButton);
		progress = v.findViewById(R.id.progressWheel);

		password.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				setError(passwordWrapper, null, false);
				continueButton.setEnabled(s.length() > 0);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		password.setOnEditorActionListener((view, actionId, event) -> {
			hideSoftKeyboard(view);
			return true;
		});
		continueButton.setOnClickListener(view -> checkPassword());

		viewModel.getPasswordResult().observeEvent(getViewLifecycleOwner(),
				this::onPasswordChecked);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.backup_title);
	}

	private void checkPassword() {
		hideSoftKeyboard(password);
		continueButton.setVisibility(INVISIBLE);
		progress.setVisibility(VISIBLE);
		viewModel.checkPassword(password.getText().toString());
	}

	private void onPasswordChecked(DecryptionResult result) {
		// A correct password moves the flow on; the fragment goes with it
		if (result == SUCCESS) return;
		continueButton.setVisibility(VISIBLE);
		progress.setVisibility(GONE);
		if (result == KEY_STRENGTHENER_ERROR) {
			createKeyStrengthenerErrorDialog(requireContext()).show();
		} else {
			setError(passwordWrapper, getString(R.string.try_again), true);
			password.setText("");
		}
	}

}
