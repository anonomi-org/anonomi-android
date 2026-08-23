package org.anonomi.android.util;

import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;

import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

/**
 * The calculator disguise: whether it is on, and how to keep the task in
 * recents looking like a calculator while it is.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class Disguise {

	public static final String CALCULATOR_ALIAS =
			"org.anonomi.android.splash.CalculatorAlias";
	public static final String SPLASH =
			"org.anonomi.android.splash.SplashScreenActivity";

	private static final String LABEL = "Calculator";

	private Disguise() {
	}

	/**
	 * Whether the app is wearing the disguise, which is what the launcher was
	 * last told rather than what any preference of ours says.
	 */
	public static boolean isEnabled(Context ctx) {
		ComponentName alias =
				new ComponentName(ctx.getPackageName(), CALCULATOR_ALIAS);
		return ctx.getPackageManager().getComponentEnabledSetting(alias) ==
				COMPONENT_ENABLED_STATE_ENABLED;
	}

	/**
	 * Labels the activity's task as the calculator while the disguise is on,
	 * and restores the default otherwise. Every activity in the task shares
	 * the entry in recents, so this has to be applied to all of them.
	 */
	public static void applyTaskDescription(Activity a) {
		if (!isEnabled(a)) {
			a.setTaskDescription(new TaskDescription());
		} else {
			// A bitmap rather than a resource id: launchers read the former
			// reliably, the latter only sometimes.
			a.setTaskDescription(new TaskDescription(LABEL,
					BitmapFactory.decodeResource(a.getResources(),
							R.mipmap.ic_calculator_launcher)));
		}
	}
}
