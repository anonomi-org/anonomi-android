package org.anonomi.android.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;

import static org.anonomi.android.util.UiUtils.formatDateAbsolute;

/**
 * Says which backup was opened and when it was made, so that the wrong file
 * is noticed before the account is put in place.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RestoreConfirmFragment extends RestoreFragment {

	private static final String TAG = RestoreConfirmFragment.class.getName();

	public static RestoreConfirmFragment newInstance() {
		return new RestoreConfirmFragment();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	protected String getHelpText() {
		return getString(R.string.restore_confirm_last_warning);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_restore_confirm, container,
				false);

		TextView found = v.findViewById(R.id.foundTextView);
		BackupManifest m = viewModel.getBackupManifest();
		String name = viewModel.getBackupName();
		if (m != null && name != null) {
			// The version that wrote the backup is deliberately left out: it
			// is text out of the file, and there is nothing to do with it
			found.setText(getString(R.string.restore_confirm_text, name,
					formatDateAbsolute(requireContext(), m.getCreated())));
		}

		Button continueButton = v.findViewById(R.id.continueButton);
		continueButton.setOnClickListener(this);

		return v;
	}

	@Override
	public void onClick(View view) {
		viewModel.onBackupConfirmed();
	}
}
