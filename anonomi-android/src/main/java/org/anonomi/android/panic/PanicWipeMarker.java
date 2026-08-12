package org.anonomi.android.panic;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * Records an unfinished wipe in preferences of its own.
 * <p>
 * Deliberately not the default preferences. Deleting an account clears those
 * before it starts deleting files, so a record kept there would be gone while
 * there was still a directory of attachments and voice files to remove - and
 * a wipe interrupted at that point would never be recognised again. This file
 * is not one of the ones that gets cleared, so it outlives every step of the
 * wipe and is removed at the end, once there is nothing left to finish.
 * <p>
 * Values are committed rather than applied: the process may have only
 * milliseconds left, and a record that never reached disk is no use.
 */
class PanicWipeMarker implements PanicWipe.Marker {

	private static final String PREFS_NAME = "cleanup";
	private static final String UNFINISHED_WIPE = "unfinished";

	private final Context appContext;
	private final SharedPreferences prefs;

	PanicWipeMarker(Context ctx) {
		appContext = ctx.getApplicationContext();
		prefs = appContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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
		// The file goes rather than the value in it, so a device that has
		// been wiped is not carrying a note saying so.
		appContext.deleteSharedPreferences(PREFS_NAME);
	}
}
