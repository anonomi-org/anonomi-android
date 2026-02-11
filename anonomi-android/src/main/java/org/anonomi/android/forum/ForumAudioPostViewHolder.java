package org.anonomi.android.forum;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.anonomi.R;
import org.anonomi.android.threaded.ThreadItemAdapter.ThreadItemListener;
import org.anonomi.android.threaded.ThreadPostViewHolder;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import androidx.annotation.UiThread;

import static java.util.logging.Logger.getLogger;

@UiThread
@NotNullByDefault
class ForumAudioPostViewHolder extends ThreadPostViewHolder<ForumPostItem> {

	private static final Logger LOG =
			getLogger(ForumAudioPostViewHolder.class.getName());

	private final ImageButton playPause;
	private final SeekBar seekBar;
	private final TextView duration;

	ForumAudioPostViewHolder(View v) {
		super(v);
		playPause = v.findViewById(R.id.btnPlayPause);
		seekBar = v.findViewById(R.id.seekBar);
		duration = v.findViewById(R.id.txtDuration);
	}

	@Override
	public void bind(ForumPostItem item,
			ThreadItemListener<ForumPostItem> listener) {
		super.bind(item, listener);

		if (!item.hasAudio()) return;

		byte[] audioData = item.getAudioData();
		if (audioData == null) return;

		// Reset UI state
		playPause.setImageResource(R.drawable.ic_play_arrow);
		seekBar.setProgress(0);
		duration.setText("0:00");

		File tempFile;
		try {
			tempFile = File.createTempFile("forum_voice", ".ogg",
					getContext().getCacheDir());
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(audioData);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			LOG.warning("Failed to write audio temp file");
			return;
		}

		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(tempFile.getAbsolutePath());
			player.prepare();
			duration.setText(formatDuration(player.getDuration()));
		} catch (IOException e) {
			LOG.warning("Failed to prepare MediaPlayer");
			return;
		}

		seekBar.setMax(player.getDuration());

		Handler handler = new Handler(Looper.getMainLooper());
		Runnable updateSeekbar = new Runnable() {
			@Override
			public void run() {
				if (player.isPlaying()) {
					seekBar.setProgress(player.getCurrentPosition());
					handler.postDelayed(this, 500);
				}
			}
		};

		playPause.setOnClickListener(v -> {
			if (player.isPlaying()) {
				player.pause();
				playPause.setImageResource(R.drawable.ic_play_arrow);
			} else {
				player.start();
				playPause.setImageResource(R.drawable.ic_pause);
				handler.post(updateSeekbar);
			}
		});

		player.setOnCompletionListener(mp -> {
			playPause.setImageResource(R.drawable.ic_play_arrow);
			seekBar.setProgress(0);
		});
	}

	private String formatDuration(int ms) {
		int seconds = ms / 1000;
		int minutes = seconds / 60;
		seconds = seconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}

}
