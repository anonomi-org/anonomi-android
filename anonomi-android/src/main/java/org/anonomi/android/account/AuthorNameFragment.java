package org.anonomi.android.account;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.anonomi.R;
import org.anonomi.android.util.UiUtils;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;

import static org.anonomi.android.util.UiUtils.setError;
import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.anonchatsecure.bramble.util.StringUtils.toUtf8;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class AuthorNameFragment extends SetupFragment {

	private static final String TAG = AuthorNameFragment.class.getName();

	private TextInputLayout authorNameWrapper;
	private TextInputEditText authorNameInput;
	private Button nextButton;

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	public static AuthorNameFragment newInstance() {
		return new AuthorNameFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_setup_author_name, container, false);

		authorNameWrapper = v.findViewById(R.id.nickname_entry_wrapper);
		authorNameInput = v.findViewById(R.id.nickname_entry);

		nextButton = v.findViewById(R.id.next);
		nextButton.setOnClickListener(this);

		Button infoButton = v.findViewById(R.id.info_button);
		infoButton.setOnClickListener(view ->
				UiUtils.showFragment(
						requireActivity().getSupportFragmentManager(),
						SetupInfoFragment.newInstance(),
						"SetupInfoFragment"
				)
		);

		authorNameInput.addTextChangedListener(textWatcher);

		// Ensure initial state is correct
		validateInputs();

		return v;
	}

	@Override
	protected String getHelpText() {
		return getString(R.string.setup_name_explanation);
	}

	private final TextWatcher textWatcher = new TextWatcher() {
		@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
			validateInputs();
		}
		@Override public void afterTextChanged(Editable s) {}
	};

	private void validateInputs() {
		String nickname = authorNameInput.getText() != null
				? authorNameInput.getText().toString().trim()
				: "";

		int nicknameLength = toUtf8(nickname).length;
		boolean nicknameError = nicknameLength > MAX_AUTHOR_NAME_LENGTH;
		setError(authorNameWrapper, getString(R.string.name_too_long), nicknameError);

		boolean nameValid = nicknameLength > 0 && !nicknameError;

		authorNameInput.setOnEditorActionListener(nameValid ? this : null);
		nextButton.setEnabled(nameValid);
	}

	@Override
	public void onClick(View view) {
		Editable nicknameText = authorNameInput.getText();
		if (nicknameText != null) {
			viewModel.setAuthorName(nicknameText.toString().trim());
		}
	}
}