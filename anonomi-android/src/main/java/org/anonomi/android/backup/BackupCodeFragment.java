package org.anonomi.android.backup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static org.anonomi.android.AppModule.getAndroidComponent;

/**
 * Shows the recovery code, once. It is not stored anywhere, so there is
 * nothing to copy it from and nothing to show it from again later.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupCodeFragment extends Fragment {

	static final String TAG = BackupCodeFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BackupViewModel viewModel;

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
		View v = inflater.inflate(R.layout.fragment_backup_code, container,
				false);

		String code = viewModel.getRecoveryCode();
		TextView codeView = v.findViewById(R.id.codeView);
		if (code != null) codeView.setText(RecoveryCode.format(code));

		Button writtenDown = v.findViewById(R.id.writtenDownButton);
		writtenDown.setOnClickListener(view -> viewModel.onCodeWrittenDown());

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.backup_code_title);
		// This screen can be restored by the fragment manager after the
		// process was killed, with nothing left to show
		if (viewModel.getRecoveryCode() == null) viewModel.restart();
	}
}
