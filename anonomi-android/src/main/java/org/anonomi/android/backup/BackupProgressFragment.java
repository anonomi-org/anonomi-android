package org.anonomi.android.backup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.anonomi.R;
import org.anonomi.android.backup.BackupViewModel.Phase;
import org.anonomi.android.backup.BackupViewModel.Progress;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static android.widget.Toast.LENGTH_SHORT;
import static org.anonomi.android.AppModule.getAndroidComponent;

/**
 * Shown while the backup is being written. Leaving does not stop the work,
 * but it does mean nobody sees whether it worked, so back is held here.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupProgressFragment extends Fragment {

	static final String TAG = BackupProgressFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BackupViewModel viewModel;
	private LinearProgressIndicator progressBar;
	private TextView phaseTextView;

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
		View v = inflater.inflate(R.layout.fragment_backup_progress, container,
				false);

		progressBar = v.findViewById(R.id.progressBar);
		phaseTextView = v.findViewById(R.id.phaseTextView);

		viewModel.getProgress()
				.observe(getViewLifecycleOwner(), this::onProgress);

		OnBackPressedCallback onBack = new OnBackPressedCallback(true) {

			@Override
			public void handleOnBackPressed() {
				Toast.makeText(requireContext(), R.string.backup_progress_wait,
						LENGTH_SHORT).show();
			}
		};
		requireActivity().getOnBackPressedDispatcher()
				.addCallback(getViewLifecycleOwner(), onBack);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.backup_progress_title);
	}

	private void onProgress(Progress p) {
		if (p.phase == Phase.SNAPSHOT) {
			// The snapshot reports nothing of its own, so it stays on the
			// indeterminate animation the layout starts with
			phaseTextView.setText(R.string.backup_progress_snapshot);
			return;
		}
		phaseTextView.setText(p.phase == Phase.WRITING ?
				R.string.backup_progress_writing :
				R.string.backup_progress_checking);
		if (p.total <= 0) return;
		progressBar.setMax(100);
		// Takes the indicator out of indeterminate mode by finishing the
		// current cycle first, rather than cutting the animation off
		progressBar.setProgressCompat((int) (p.done * 100 / p.total), true);
	}
}
