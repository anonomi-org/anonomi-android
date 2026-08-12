package org.anonomi.android.panic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Records an unfinished wipe in the default preferences.
 * <p>
 * Deleting an account clears these preferences as part of clearing the app's
 * data, so a wipe that runs to the end takes the record with it and leaves
 * nothing behind that could make a later start delete a newly created
 * account. A wipe that is interrupted before it gets that far leaves the
 * record in place, which is the point.
 * <p>
 * Values are committed rather than applied: the process may have only
 * milliseconds left, and a record that never reached disk is no use.
 */
class PanicWipeMarker implements PanicWipe.Marker {

	private static final String UNFINISHED_WIPE = "wipe_unfinished";

	private final SharedPreferences prefs;

	PanicWipeMarker(Context ctx) {
		prefs = PreferenceManager.getDefaultSharedPreferences(
				ctx.getApplicationContext());
	}

	@Override
	public void set() {
		prefs.edit().putBoolean(UNFINISHED_WIPE, true).commit();
	}

	@Override
	public boolean isSet() {
		return prefs.getBoolean(UNFINISHED_WIPE, false);
	}

	@Override
	public void clear() {
		prefs.edit().remove(UNFINISHED_WIPE).commit();
	}
}
