package org.anonomi.android.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class VoiceCacheCleaner {

	private static final String TAG = "VoiceCacheCleaner";

	// Change to 1 day if you want more aggressive cleanup
	private static final long MAX_AGE_MILLIS = 1 * 24 * 60 * 60 * 1000L;

	public static void cleanupOldVoiceFiles(Context context) {
		File cacheDir = context.getCacheDir();
		if (cacheDir == null || !cacheDir.exists()) return;

		File[] files = cacheDir.listFiles((dir, name) ->
				name.endsWith(".ogg") && (name.startsWith("voice") || name.startsWith("recording_")));

		if (files == null) return;

		long now = System.currentTimeMillis();
		for (File file : files) {
			long age = now - file.lastModified();
			if (age > MAX_AGE_MILLIS) {
				if (file.delete()) {
					Log.i(TAG, "Deleted old voice file: " + file.getName());
				} else {
					Log.w(TAG, "Failed to delete: " + file.getName());
				}
			}
		}
	}
}