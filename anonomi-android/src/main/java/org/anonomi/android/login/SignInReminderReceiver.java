package org.anonomi.android.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonomi.android.AndroidComponent;
import org.anonomi.android.AnonChatApplication;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;

import javax.inject.Inject;

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.content.Intent.ACTION_MY_PACKAGE_REPLACED;
import static org.anonomi.android.settings.NotificationsFragment.PREF_NOTIFY_SIGN_IN;
import static org.anonchatsecure.anonchat.api.android.AndroidNotificationManager.ACTION_DISMISS_REMINDER;

public class SignInReminderReceiver extends BroadcastReceiver {

	@Inject
	AccountManager accountManager;
	@Inject
	AndroidNotificationManager notificationManager;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		AnonChatApplication app = (AnonChatApplication) ctx.getApplicationContext();
		AndroidComponent applicationComponent = app.getApplicationComponent();
		applicationComponent.inject(this);

		String action = intent.getAction();
		if (action == null) return;
		if (action.equals(ACTION_BOOT_COMPLETED) ||
				action.equals(ACTION_MY_PACKAGE_REPLACED)) {
			if (accountManager.accountExists() &&
					!accountManager.hasDatabaseKey()) {
				SharedPreferences prefs = app.getDefaultSharedPreferences();
				if (prefs.getBoolean(PREF_NOTIFY_SIGN_IN, true)) {
					notificationManager.showSignInNotification();
				}
			}
		} else if (action.equals(ACTION_DISMISS_REMINDER)) {
			notificationManager.clearSignInNotification();
		}
	}

}
