package org.anonomi.android.account;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonomi.R;
import org.anonomi.android.account.SetupViewModel.RestoreFailure;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;

import static androidx.core.widget.ImageViewCompat.setImageTintList;
import static org.anonomi.android.account.SetupViewModel.RestoreFailure.WRONG_CODE_OR_DAMAGED;
import static org.anonomi.android.util.UiUtils.hideViewOnSmallScreen;

/**
 * Says why an account was not restored, and offers the one way on that makes
 * sense for the reason.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RestoreErrorFragment extends RestoreFragment {

	private static final String TAG = RestoreErrorFragment.class.getName();

	public static RestoreErrorFragment newInstance() {
		return new RestoreErrorFragment();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	protected String getHelpText() {
		return getString(getFailureText());
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_final, container, false);

		ImageView iconView = v.findViewById(R.id.iconView);
		iconView.setImageResource(R.drawable.alerts_and_states_error);
		setImageTintList(iconView, ColorStateList
				.valueOf(getResources().getColor(R.color.anon_red_500)));

		TextView titleView = v.findViewById(R.id.titleView);
		titleView.setText(R.string.restore_error_title);
		TextView textView = v.findViewById(R.id.textView);
		textView.setText(getFailureText());

		Button button = v.findViewById(R.id.button);
		// A code that did not open the file can simply be typed again;
		// everything else is a reason to go back to the file itself
		button.setText(viewModel.getRestoreFailure() == WRONG_CODE_OR_DAMAGED ?
				R.string.restore_error_try_code :
				R.string.restore_error_choose_file);
		button.setOnClickListener(this);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		hideViewOnSmallScreen(requireView().findViewById(R.id.iconView));
	}

	@Override
	public void onClick(View view) {
		viewModel.afterFailure();
	}

	private int getFailureText() {
		RestoreFailure f = viewModel.getRestoreFailure();
		if (f == RestoreFailure.NOT_A_BACKUP) {
			return R.string.restore_error_not_a_backup;
		} else if (f == WRONG_CODE_OR_DAMAGED) {
			return R.string.restore_error_wrong_code;
		} else if (f == RestoreFailure.TRUNCATED) {
			return R.string.restore_error_truncated;
		} else if (f == RestoreFailure.TOO_NEW) {
			return R.string.restore_error_too_new;
		} else if (f == RestoreFailure.UNSUPPORTED_FORMAT) {
			return R.string.restore_error_unsupported_format;
		} else if (f == RestoreFailure.TOO_OLD) {
			return R.string.restore_error_too_old;
		} else if (f == RestoreFailure.NOT_ENOUGH_MEMORY) {
			return R.string.restore_error_not_enough_memory;
		} else if (f == RestoreFailure.NOT_ENOUGH_SPACE) {
			return R.string.restore_error_not_enough_space;
		} else if (f == RestoreFailure.CANNOT_READ_FILE) {
			return R.string.restore_error_cannot_read;
		} else if (f == RestoreFailure.COULD_NOT_RESTORE) {
			return R.string.restore_error_could_not_restore;
		} else {
			return R.string.restore_error_damaged;
		}
	}
}
