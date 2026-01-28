package org.anonomi.android;

import android.app.Activity;
import android.content.SharedPreferences;

import org.anonchatsecure.bramble.BrambleApplication;
import org.anonomi.android.navdrawer.NavDrawerActivity;

/**
 * This exists so that the Application object will not necessarily be cast
 * directly to the Briar application object.
 */
public interface AnonChatApplication extends BrambleApplication {

	Class<? extends Activity> ENTRY_ACTIVITY = NavDrawerActivity.class;

	AndroidComponent getApplicationComponent();

	SharedPreferences getDefaultSharedPreferences();

	boolean isRunningInBackground();

	boolean isInstrumentationTest();
}
