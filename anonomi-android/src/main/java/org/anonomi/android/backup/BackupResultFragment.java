package org.anonomi.android.backup;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonomi.R;
import org.anonomi.android.backup.BackupViewModel.Failure;
import org.anonomi.android.backup.BackupViewModel.Result;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import static androidx.core.widget.ImageViewCompat.setImageTintList;
import static org.anonomi.android.AppModule.getAndroidComponent;
import static org.anonomi.android.backup.BackupViewModel.State.DONE;
import static org.anonomi.android.util.UiUtils.hideViewOnSmallScreen;

/**
 * The last screen of the flow, whether the backup was written or not. Its
 * text is built here rather than taken from a resource because it names the
 * file and says whether it is still on this device.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupResultFragment extends Fragment {

	static final String TAG = BackupResultFragment.class.getName();

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
		ActionBar actionBar =
				((AppCompatActivity) context).getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setHomeButtonEnabled(false);
		}
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_final, container, false);

		boolean succeeded = viewModel.getState().getValue() == DONE;
		ImageView iconView = v.findViewById(R.id.iconView);
		TextView titleView = v.findViewById(R.id.titleView);
		TextView textView = v.findViewById(R.id.textView);
		Button button = v.findViewById(R.id.button);

		int icon = succeeded ? R.drawable.ic_check_circle_outline :
				R.drawable.alerts_and_states_error;
		int tint = succeeded ? R.color.briar_brand_green : R.color.anon_red_500;
		iconView.setImageResource(icon);
		setImageTintList(iconView, ColorStateList
				.valueOf(getResources().getColor(tint)));
		titleView.setText(succeeded ? R.string.backup_success_title :
				R.string.backup_error_title);
		textView.setText(succeeded ? getSuccessText() : getFailureText());
		button.setText(R.string.finish);
		button.setOnClickListener(view -> finish());

		OnBackPressedCallback onBack = new OnBackPressedCallback(true) {

			@Override
			public void handleOnBackPressed() {
				finish();
			}
		};
		requireActivity().getOnBackPressedDispatcher()
				.addCallback(getViewLifecycleOwner(), onBack);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(viewModel.getState().getValue() == DONE ?
				R.string.backup_success_title : R.string.backup_error_title);
		hideViewOnSmallScreen(requireView().findViewById(R.id.iconView));
	}

	@Override
	public void onDetach() {
		ActionBar actionBar =
				((AppCompatActivity) requireActivity()).getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeButtonEnabled(true);
		}
		super.onDetach();
	}

	private String getSuccessText() {
		Result r = viewModel.getResult();
		StringBuilder sb = new StringBuilder();
		if (r == null) sb.append(getString(R.string.backup_success_text));
		else {
			sb.append(getString(R.string.backup_success_named_text,
					r.displayName));
			// A card in this phone is seized with this phone, so a backup on
			// one is not a backup yet
			if (r.onThisDevice) {
				sb.append("\n\n");
				sb.append(getString(R.string.backup_success_on_this_device));
			}
		}
		return sb.toString();
	}

	private String getFailureText() {
		Failure f = viewModel.getFailure();
		int text;
		if (f == Failure.NOT_ENOUGH_SPACE) {
			text = R.string.backup_error_no_space;
		} else if (f == Failure.UNREADABLE) {
			text = R.string.backup_error_unreadable;
		} else if (f == Failure.SIGNED_OUT) {
			text = R.string.backup_error_signed_out;
		} else {
			text = R.string.backup_error_write;
		}
		StringBuilder sb = new StringBuilder(getString(text));
		sb.append("\n\n");
		// Say what became of the file either way: a half-written one that
		// looks like a backup is the thing to warn about
		Result r = viewModel.getResult();
		if (r != null && r.fileLeftBehind) {
			sb.append(getString(R.string.backup_error_file_left_behind,
					r.displayName));
		} else {
			sb.append(getString(R.string.backup_error_file_deleted));
		}
		return sb.toString();
	}

	private void finish() {
		requireActivity().supportFinishAfterTransition();
	}
}
