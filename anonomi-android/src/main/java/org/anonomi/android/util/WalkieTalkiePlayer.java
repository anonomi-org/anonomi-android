package org.anonomi.android.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class WalkieTalkiePlayer {

	private static final String TAG = "WalkieTalkiePlayer";

	public interface Listener {
		void onPlaybackStarted(String senderName);
		void onAllPlaybackFinished();
	}

	private static class QueueItem {
		final File file;
		final String senderName;
		QueueItem(File file, String senderName) {
			this.file = file;
			this.senderName = senderName;
		}
	}

	private MediaPlayer mediaPlayer;
	private final LinkedList<QueueItem> queue = new LinkedList<>();
	private boolean playing = false;
	private final Context context;
	private Listener listener;

	public WalkieTalkiePlayer(Context context) {
		this.context = context.getApplicationContext();
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public synchronized void play(byte[] audioData, String senderName) {
		try {
			File tempFile = File.createTempFile("wt_audio_", ".ogg",
					context.getCacheDir());
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(audioData);
			}
			queue.add(new QueueItem(tempFile, senderName));
			if (!playing) {
				playNext();
			}
		} catch (IOException e) {
			Log.e(TAG, "Failed to write temp audio file", e);
		}
	}

	private synchronized void playNext() {
		if (queue.isEmpty()) {
			playing = false;
			if (listener != null) {
				listener.onAllPlaybackFinished();
			}
			return;
		}
		playing = true;
		QueueItem item = queue.poll();
		if (listener != null) {
			listener.onPlaybackStarted(item.senderName);
		}
		try {
			if (mediaPlayer != null) {
				mediaPlayer.release();
			}
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(item.file.getAbsolutePath());
			mediaPlayer.setOnCompletionListener(mp -> {
				item.file.delete();
				playNext();
			});
			mediaPlayer.setOnErrorListener((mp, what, extra) -> {
				Log.e(TAG, "MediaPlayer error: " + what + "/" + extra);
				item.file.delete();
				playNext();
				return true;
			});
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IOException e) {
			Log.e(TAG, "Failed to play audio", e);
			item.file.delete();
			playNext();
		}
	}

	public synchronized void stop() {
		for (QueueItem qi : queue) {
			qi.file.delete();
		}
		queue.clear();
		if (mediaPlayer != null) {
			try {
				mediaPlayer.setOnCompletionListener(null);
				mediaPlayer.setOnErrorListener(null);
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				mediaPlayer.release();
			} catch (IllegalStateException ignored) {
			}
			mediaPlayer = null;
		}
		playing = false;
	}

	public synchronized boolean isPlaying() {
		return playing;
	}

	public synchronized void release() {
		stop();
	}
}
