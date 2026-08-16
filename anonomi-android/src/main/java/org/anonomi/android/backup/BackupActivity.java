package org.anonomi.android.backup;

import android.os.Bundle;
import android.view.MenuItem;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.backup.BackupViewModel.State;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;
import static org.anonomi.android.util.UiUtils.showFragment;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BackupActivity extends BriarActivity {

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BackupViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(BackupViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// The recovery code is shown here, so screenshots are off even in a
		// debug build, where the rest of the app allows them
		getWindow().addFlags(FLAG_SECURE);
		setContentView(R.layout.activity_fragment_container);
		viewModel.getState().observe(this, this::onStateChanged);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onStateChanged(State state) {
		Fragment f;
		String tag;
		if (state == State.PASSWORD) {
			f = new BackupIntroFragment();
			tag = BackupIntroFragment.TAG;
		} else if (state == State.SHOW_CODE) {
			f = new BackupCodeFragment();
			tag = BackupCodeFragment.TAG;
		} else if (state == State.CONFIRM_CODE) {
			f = new BackupConfirmFragment();
			tag = BackupConfirmFragment.TAG;
		} else if (state == State.RUNNING) {
			f = new BackupProgressFragment();
			tag = BackupProgressFragment.TAG;
		} else {
			f = new BackupResultFragment();
			tag = BackupResultFragment.TAG;
		}
		// Nothing goes on the back stack: until the file has been written
		// there is nothing to come back to, and afterwards there is nothing
		// to repeat
		showFragment(getSupportFragmentManager(), f, tag, false);
	}
}
