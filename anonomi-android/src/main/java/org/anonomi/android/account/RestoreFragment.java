package org.anonomi.android.account;

import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

/**
 * A screen of the restore flow.
 * <p>
 * None of these screens is on the fragment back stack, but the fragment
 * manager still brings the last one back after the process has been killed -
 * and the view model that knew about the restore went with the process. The
 * screen is on display, and interactive, before the new view model's first
 * event lands, so each one checks on the way in rather than waiting to be
 * replaced.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class RestoreFragment extends SetupFragment {

	@Override
	public void onStart() {
		super.onStart();
		if (!viewModel.isRestoring()) viewModel.restartSetup();
	}
}
