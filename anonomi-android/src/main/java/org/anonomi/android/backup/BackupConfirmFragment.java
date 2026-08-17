package org.anonomi.android.backup;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonomi.R;
import org.anonomi.android.util.ActivityLaunchers.CreateDocumentAdvanced;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static android.widget.Toast.LENGTH_LONG;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.android.AppModule.getAndroidComponent;
import static org.anonomi.android.backup.BackupViewModel.isLocalDestination;
import static org.anonomi.android.util.UiUtils.hideSoftKeyboard;
import static org.anonomi.android.util.UiUtils.setError;

/**
 * Asks for the recovery code back, so the user finds out here rather than at
 * a restore that what they wrote down is not what they were shown.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupConfirmFragment extends Fragment {

	static final String TAG = BackupConfirmFragment.class.getName();
	private static final Logger LOG = getLogger(TAG);

	private final ActivityResultLauncher<String> launcher =
			registerForActivityResult(new CreateDocumentAdvanced(),
					this::onDocumentCreated);

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BackupViewModel viewModel;
	private TextInputLayout codeWrapper;
	private EditText codeEntry;
	private Button chooseFileButton;

	/**
	 * The dialog asking about the destination, and the document the picker
	 * made for it, while neither button has been pressed.
	 */
	@Nullable
	private AlertDialog warning = null;
	@Nullable
	private Uri undecided = null;

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
		View v = inflater.inflate(R.layout.fragment_backup_confirm, container,
				false);

		codeWrapper = v.findViewById(R.id.codeEntryWrapper);
		codeEntry = v.findViewById(R.id.codeEntry);
		chooseFileButton = v.findViewById(R.id.chooseFileButton);

		codeEntry.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				setError(codeWrapper, null, false);
				// Nothing is checked until a whole code has been typed, so
				// that typing one does not report a mistake at every digit
				chooseFileButton.setEnabled(
						RecoveryCode.normalise(s.toString()) != null);
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		codeEntry.setOnEditorActionListener((view, actionId, event) -> {
			hideSoftKeyboard(view);
			return true;
		});
		chooseFileButton.setOnClickListener(view -> chooseFile());

		Button showAgain = v.findViewById(R.id.showCodeAgainButton);
		showAgain.setOnClickListener(view -> viewModel.showCodeAgain());

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.backup_confirm_title);
	}

	private void chooseFile() {
		if (!viewModel.isCodeConfirmed(codeEntry.getText().toString())) {
			setError(codeWrapper, getString(R.string.backup_confirm_wrong),
					true);
			return;
		}
		hideSoftKeyboard(codeEntry);
		try {
			launcher.launch(viewModel.getFileName());
		} catch (ActivityNotFoundException e) {
			logException(LOG, WARNING, e);
			Toast.makeText(requireContext(), R.string.error_start_activity,
					LENGTH_LONG).show();
		}
	}

	private void onDocumentCreated(@Nullable Uri uri) {
		if (uri == null) return;
		if (viewModel.getRecoveryCode() == null) {
			// The activity was destroyed while the picker was up, taking the
			// code with it. There is nothing to write the backup with
			Toast.makeText(requireContext(), R.string.backup_start_again,
					LENGTH_LONG).show();
			viewModel.restart();
			return;
		}
		if (isLocalDestination(uri)) showLocalWarning(uri);
		else showCloudWarning(uri);
	}

	/**
	 * Every destination the picker can offer is on the phone, so this is not
	 * a choice between a safe place and an unsafe one. It is said here rather
	 * than only on the result screen because afterwards the job feels done,
	 * and moving the file is the half that is left.
	 */
	private void showLocalWarning(Uri uri) {
		showDestinationWarning(uri, R.string.backup_local_warning_title,
				R.string.backup_local_warning_text,
				R.string.backup_local_warning_continue);
	}

	/**
	 * There is no way to ask a provider whether it syncs the file somewhere
	 * online, so anywhere we do not recognise is worth saying so about.
	 */
	private void showCloudWarning(Uri uri) {
		showDestinationWarning(uri, R.string.backup_cloud_warning_title,
				R.string.backup_cloud_warning_text,
				R.string.backup_cloud_warning_continue);
	}

	private void showDestinationWarning(Uri uri, @StringRes int title,
			@StringRes int text, @StringRes int confirm) {
		undecided = uri;
		warning = new MaterialAlertDialogBuilder(requireContext(),
				R.style.AnonDialogTheme)
				.setTitle(title)
				.setMessage(text)
				// The picker has already made the file, so dismissing this
				// without answering would leave one behind at a destination
				// the user is in the middle of rejecting
				.setCancelable(false)
				.setPositiveButton(confirm, (dialog, which) -> {
					undecided = null;
					viewModel.exportTo(uri);
				})
				.setNegativeButton(R.string.backup_warning_choose,
						(dialog, which) -> {
							undecided = null;
							viewModel.discardDocument(uri);
							chooseFile();
						})
				.show();
	}

	@Override
	public void onDestroyView() {
		// A rotation takes the dialog down without either button firing, so
		// the file the picker made would be stranded at a destination that
		// was never agreed to - the same thing setCancelable(false) is for
		if (warning != null) {
			warning.dismiss();
			warning = null;
		}
		if (undecided != null) {
			viewModel.discardDocument(undecided);
			undecided = null;
		}
		super.onDestroyView();
	}
}
