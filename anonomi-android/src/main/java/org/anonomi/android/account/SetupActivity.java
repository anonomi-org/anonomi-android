package org.anonomi.android.account;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BaseActivity;
import org.anonomi.android.fragment.BaseFragment;
import org.anonomi.android.fragment.BaseFragment.BaseFragmentListener;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static android.view.WindowManager.LayoutParams.FLAG_SECURE;
import static android.widget.Toast.LENGTH_SHORT;
import static org.anonomi.android.AnonChatApplication.ENTRY_ACTIVITY;
import static org.anonomi.android.account.SetupViewModel.State.AUTHOR_NAME;
import static org.anonomi.android.account.SetupViewModel.State.CREATED;
import static org.anonomi.android.account.SetupViewModel.State.DOZE;
import static org.anonomi.android.account.SetupViewModel.State.FAILED;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_CODE;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_CONFIRM;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_FAILED;
import static org.anonomi.android.account.SetupViewModel.State.RESTORE_INTRO;
import static org.anonomi.android.account.SetupViewModel.State.SET_PASSWORD;
import static org.anonomi.android.util.UiUtils.setInputStateAlwaysVisible;
import static org.anonomi.android.util.UiUtils.setInputStateHidden;
import static org.anonomi.android.util.UiUtils.showFragment;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetupActivity extends BaseActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	private SetupViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(SetupViewModel.class);
		viewModel.getState().observeEvent(this, this::onStateChanged);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		setContentView(R.layout.activity_fragment_container);
		// The state arrives as an event and is consumed once, so rotating
		// anywhere in the restore flow brings the activity back without it
		if (viewModel.isRestoring()) secureWindow();
	}

	/**
	 * Keeps the recovery code out of screenshots and the recents thumbnail.
	 * Never taken off again: the rest of setup is no worse for it.
	 */
	private void secureWindow() {
		getWindow().addFlags(FLAG_SECURE);
	}

	private void onStateChanged(SetupViewModel.State state) {
		if (state == AUTHOR_NAME) {
			// Not always-visible, unlike the other typing screens: the keyboard
			// covers the offer to restore from a backup at the foot of this one
			setInputStateHidden(this);
			showInitialFragment(AuthorNameFragment.newInstance());
		} else if (state == SET_PASSWORD) {
			setInputStateAlwaysVisible(this);
			showSetupFragment(SetPasswordFragment.newInstance());
		} else if (state == DOZE) {
			setInputStateHidden(this);
			showDozeFragment();
		} else if (state == RESTORE_INTRO) {
			setInputStateHidden(this);
			// A recovery code is typed in from here on. The rest of the app
			// leaves screenshots on in a debug build; this does not
			secureWindow();
			showSetupFragment(RestoreIntroFragment.newInstance());
		} else if (state == RESTORE_CODE) {
			setInputStateAlwaysVisible(this);
			showSetupFragment(RestoreCodeFragment.newInstance());
		} else if (state == RESTORE_CONFIRM) {
			setInputStateHidden(this);
			showSetupFragment(RestoreConfirmFragment.newInstance());
		} else if (state == RESTORE_FAILED) {
			setInputStateHidden(this);
			showSetupFragment(RestoreErrorFragment.newInstance());
		} else if (state == CREATED || state == FAILED) {
			// TODO: Show an error if failed
			showApp();
		}
	}

	/**
	 * Shows a screen of the flow. Nothing is put on the back stack while an
	 * account is being restored: the restore fork can be walked backwards in
	 * a way the fragment manager cannot work out on its own, so
	 * {@link SetupViewModel#goBack()} does it instead.
	 */
	private void showSetupFragment(BaseFragment f) {
		showFragment(getSupportFragmentManager(), f, f.getUniqueTag(),
				!viewModel.isRestoring());
	}

	@TargetApi(23)
	private void showDozeFragment() {
		showSetupFragment(DozeFragment.newInstance());
	}

	@Override
	public void onBackPressed() {
		if (viewModel.isRestoring()) {
			if (viewModel.isBusy()) {
				Toast.makeText(this, R.string.restore_reading_wait,
						LENGTH_SHORT).show();
				return;
			}
			if (viewModel.goBack()) return;
		}
		super.onBackPressed();
	}

	private void showApp() {
		Intent i = new Intent(this, ENTRY_ACTIVITY);
		i.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_TASK_ON_HOME |
				FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		supportFinishAfterTransition();
		overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out);
	}

	@Override
	@Deprecated
	public void runOnDbThread(Runnable runnable) {
		throw new RuntimeException("Don't use this deprecated method here.");
	}

}
