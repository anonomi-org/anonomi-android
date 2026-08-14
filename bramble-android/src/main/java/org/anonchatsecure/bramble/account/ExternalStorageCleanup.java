package org.anonchatsecure.bramble.account;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.logging.Logger;

import static android.content.Context.MODE_PRIVATE;
import static java.util.logging.Level.INFO;
import static org.anonchatsecure.bramble.util.IoUtils.deleteFileOrDir;

/**
 * Deletes the app's directories on external storage once an account has been
 * deleted.
 * <p>
 * Deleting an account does not reach them: the data directory it walks is the
 * one on internal storage, and the external directories it does name are the
 * cache and media ones. Nothing upstream writes to external files storage, so
 * it was never in the list.
 * <p>
 * The work is kept out of {@code deleteAccount} rather than added to it. What
 * is stored here is a tree of small files whose size depends on how much of a
 * map has been looked at, so the time it takes is unbounded, while every
 * caller of {@code deleteAccount} is somewhere that cannot afford to wait:
 * two of them are on the UI thread, one is on the thread starting the app, and
 * the last is inside a shutdown that is timed. Callers mark the work as due and
 * then choose when to do it.
 * <p>
 * A mark is left in preferences of its own, for the same reason the panic wipe
 * keeps its marker separate: deleting an account clears the preferences it is
 * handed before it deletes anything, and the shared preferences directory
 * itself is spared, so a file that is not one of the two survives. The mark is
 * only cleared once the directories are gone, so an attempt that is
 * interrupted - by the process being killed, or by the CPU sleeping once the
 * wake lock the app was holding has lapsed - is repeated at the next start.
 */
public class ExternalStorageCleanup {

	private static final Logger LOG =
			Logger.getLogger(ExternalStorageCleanup.class.getName());

	private static final String PREFS_NAME = "external_cleanup";
	private static final String CLEANUP_DUE = "due";

	/**
	 * Deleted before the rest of the tree. Everything on external storage goes
	 * in the end, but the tiles fetched while browsing an online map are the
	 * part that says where someone has been, and an attempt that does not run
	 * to completion should have spent its time on that rather than on the
	 * imported maps, which say nothing and can be imported again.
	 */
	private static final String DELETE_FIRST = "tiles/fetched";

	/**
	 * Held across the deletion so that a caller which waits for it and one
	 * which does not cannot run it twice over the same directories.
	 */
	private static final Object LOCK = new Object();

	private final Context appContext;

	public ExternalStorageCleanup(Context ctx) {
		appContext = ctx.getApplicationContext();
	}

	/**
	 * Records that the directories are due to be deleted. Called before the
	 * account is deleted, so that an interruption at any point afterwards
	 * still leaves the work to be found.
	 */
	public void markDue() {
		getPrefs().edit().putBoolean(CLEANUP_DUE, true).commit();
	}

	/**
	 * Deletes the directories if they are due, and blocks until they are gone.
	 * Safe to call when nothing is due, and safe to call again after an
	 * attempt that did not finish.
	 */
	public void runIfDue() {
		synchronized (LOCK) {
			SharedPreferences prefs = getPrefs();
			if (!prefs.getBoolean(CLEANUP_DUE, false)) return;
			File[] dirs = appContext.getExternalFilesDirs(null);
			// The trail first, in each of them
			for (File dir : dirs) {
				if (dir != null) delete(new File(dir, DELETE_FIRST));
			}
			for (File dir : dirs) {
				if (dir != null) delete(dir);
			}
			// The value goes rather than the file, which would need a call this
			// module's minimum API level does not have.
			prefs.edit().remove(CLEANUP_DUE).commit();
		}
	}

	/**
	 * Deletes the directories if they are due, on a thread of its own. For
	 * callers that must not block, and where nothing is waiting for the
	 * directories to be gone before it happens.
	 */
	public void runIfDueInBackground() {
		new Thread(this::runIfDue, "ExternalStorageCleanup").start();
	}

	private void delete(File file) {
		if (!file.exists()) return;
		if (LOG.isLoggable(INFO)) {
			LOG.info("Deleting " + file.getAbsolutePath());
		}
		deleteFileOrDir(file);
	}

	private SharedPreferences getPrefs() {
		return appContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	}
}
