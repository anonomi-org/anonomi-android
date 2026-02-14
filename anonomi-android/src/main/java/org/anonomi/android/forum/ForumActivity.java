package org.anonomi.android.forum;

import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.map.MapLocationPickerActivity;
import org.anonomi.android.sharing.ForumSharingStatusActivity;
import org.anonomi.android.sharing.ShareForumActivity;
import org.anonomi.android.threaded.ThreadListActivity;
import org.anonomi.android.threaded.ThreadListViewModel;
import org.anonomi.android.util.AudioUtils;
import org.anonomi.android.util.WalkieTalkiePlayer;
import org.anonomi.android.view.CompositeSendButton;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.anonomi.android.attachment.media.ImageCompressor;

import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_IMAGE_SIZE;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static org.anonomi.android.activity.RequestCodes.REQUEST_SHARE_FORUM;
import static org.anonomi.android.util.UiUtils.observeOnce;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_POST_TEXT_LENGTH;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ForumActivity extends
		ThreadListActivity<ForumPostItem, ForumPostAdapter> {

	private static final int REQUEST_SEND_LOCATION = 2001;
	private static final String PREF_FORUM_DISTORTION =
			"forum_voice_distortion_";
	private static final String PREF_FORUM_WALKIE_TALKIE =
			"forum_walkie_talkie_";

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	@Inject
	ImageCompressor imageCompressor;

	private ForumViewModel viewModel;

	private AudioRecord audioRecord;
	private Thread recordingThread;
	private boolean isRecording = false;
	private volatile byte[] recordedPcmData;
	private TextView slideToCancelText;
	private CompositeSendButton compositeSendButton;
	private long recordingStartTime;
	private final Handler recordingTimerHandler =
			new Handler(Looper.getMainLooper());

	// Walkie-Talkie
	private volatile boolean walkieTalkieEnabled = false;
	private boolean isWalkieTalkieRecording = false;
	private TextView walkieTalkieBar;
	private WalkieTalkiePlayer walkieTalkiePlayer;
	private final List<android.util.Pair<byte[], String>> pendingAutoPlay =
			new ArrayList<>();

	private final Runnable walkieTalkieTimerRunnable = new Runnable() {
		@Override
		public void run() {
			if (isWalkieTalkieRecording) {
				long elapsed = System.currentTimeMillis() - recordingStartTime;
				int seconds = (int) (elapsed / 1000);

				if (seconds >= 11) {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ForumActivity.this, R.color.anon_red_700));
				} else if (seconds >= 10) {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ForumActivity.this, R.color.anon_orange_500));
				} else {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ForumActivity.this, R.color.anon_red_500));
				}
				walkieTalkieBar.setText(getString(
						R.string.walkie_talkie_recording_timer, seconds));

				if (seconds >= 12) {
					isWalkieTalkieRecording = false;
					recordingTimerHandler.removeCallbacks(this);
					stopAndSendRecording();
					vibrateShort();
					setWalkieTalkieBarIdle();
					drainPendingAutoPlay();
				} else {
					recordingTimerHandler.postDelayed(this, 250);
				}
			}
		}
	};

	private final Runnable recordingTimerRunnable = new Runnable() {
		@Override
		public void run() {
			if (isRecording) {
				long elapsed = System.currentTimeMillis() - recordingStartTime;
				int seconds = (int) (elapsed / 1000);

				if (seconds >= 11) {
					slideToCancelText.setTextColor(ContextCompat.getColor(
							ForumActivity.this, R.color.anon_red_500));
				} else if (seconds >= 10) {
					slideToCancelText.setTextColor(ContextCompat.getColor(
							ForumActivity.this, R.color.anon_orange_500));
				} else {
					slideToCancelText.setTextColor(ContextCompat.getColor(
							ForumActivity.this, R.color.white));
				}
				slideToCancelText.setText(getString(
						R.string.slide_to_cancel_with_timer, seconds));

				if (seconds >= 12) {
					slideToCancelText.setText(
							getString(R.string.recording_max_reached));
					stopAndSendRecording();
				} else {
					recordingTimerHandler.postDelayed(this, 250);
				}
			}
		}
	};

	private final ActivityResultLauncher<String> imagePickerLauncher =
			registerForActivityResult(
					new ActivityResultContracts.GetContent(),
					this::onImageSelected);

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(ForumViewModel.class);
	}

	@Override
	protected ThreadListViewModel<ForumPostItem> getViewModel() {
		return viewModel;
	}

	@Override
	protected ForumPostAdapter createAdapter() {
		return new ForumPostAdapter(this);
	}

	@Override
	protected int getLayoutResId() {
		return R.layout.activity_forum_conversation;
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		Toolbar toolbar = setUpCustomToolbar(false);
		// Open member list on Toolbar click
		toolbar.setOnClickListener(v -> {
			Intent i = new Intent(ForumActivity.this,
					ForumSharingStatusActivity.class);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i);
		});

		String groupName = getIntent().getStringExtra(GROUP_NAME);
		if (groupName != null) {
			setTitle(groupName);
		} else {
			observeOnce(viewModel.loadForum(), this, forum ->
					setTitle(forum.getName())
			);
		}

		// Walkie-Talkie init
		walkieTalkieBar = findViewById(R.id.walkieTalkieBar);
		walkieTalkiePlayer = new WalkieTalkiePlayer(this);
		walkieTalkiePlayer.setListener(new WalkieTalkiePlayer.Listener() {
			@Override
			public void onPlaybackStarted(String senderName) {
				setWalkieTalkieBarPlaying(senderName);
			}
			@Override
			public void onAllPlaybackFinished() {
				setWalkieTalkieBarIdle();
			}
		});
		walkieTalkieEnabled = isWalkieTalkieEnabled();
		updateWalkieTalkieBar();
		viewModel.getAutoPlayAudio().observeEvent(this, pair -> {
			if (walkieTalkieEnabled && pair.first != null) {
				if (isWalkieTalkieRecording) {
					pendingAutoPlay.add(new android.util.Pair<>(
							pair.first, pair.second));
				} else {
					walkieTalkiePlayer.play(pair.first, pair.second);
				}
			}
		});

		// Voice recording UI
		slideToCancelText = findViewById(R.id.slideToCancelText);
		compositeSendButton = textInput.findViewById(R.id.compositeSendButton);
		updateMicColor();

		// Image picker
		compositeSendButton.setImagesSupported();
		compositeSendButton.setOnImageClickListener(
				v -> imagePickerLauncher.launch("image/*"));

		AppCompatImageButton recordButton =
				textInput.findViewById(R.id.recordButton);
		recordButton.setOnTouchListener(new View.OnTouchListener() {
			float startX = 0;
			boolean cancelled = false;
			static final float SWIPE_CANCEL_THRESHOLD = 150f;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startX = event.getX();
						cancelled = false;
						slideToCancelText.setText(
								getString(R.string.slide_to_cancel));
						slideToCancelText.setAlpha(1f);
						slideToCancelText.setVisibility(View.VISIBLE);
						startRecording();
						recordingStartTime = System.currentTimeMillis();
						recordingTimerHandler.post(recordingTimerRunnable);
						return true;

					case MotionEvent.ACTION_MOVE:
						float deltaX = event.getX() - startX;
						if (Math.abs(deltaX) > SWIPE_CANCEL_THRESHOLD
								&& !cancelled) {
							cancelRecording();
							cancelled = true;
							slideToCancelText.setText(
									R.string.recording_cancelled);
							slideToCancelText.animate()
									.alpha(0f)
									.setDuration(500)
									.withEndAction(() ->
											slideToCancelText.setVisibility(
													View.GONE))
									.start();
						}
						return true;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (!cancelled) stopAndSendRecording();
						slideToCancelText.animate()
								.alpha(0f)
								.setDuration(300)
								.withEndAction(() ->
										slideToCancelText.setVisibility(
												View.GONE))
								.start();
						return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void onActivityResult(int request, int result,
			@Nullable Intent data) {
		if (request == REQUEST_SEND_LOCATION &&
				result == RESULT_OK && data != null) {
			String message = data.getStringExtra(
					MapLocationPickerActivity.RESULT_MAP_MESSAGE);
			if (message != null) {
				MessageId replyId = viewModel.getCurrentReplyId();
				viewModel.createAndStoreMessage(message, replyId);
				viewModel.clearReplyId();
				Toast.makeText(this, R.string.location_sent,
						Toast.LENGTH_SHORT).show();
			}
		} else if (request == REQUEST_SHARE_FORUM && result == RESULT_OK) {
			displaySnackbar(R.string.forum_shared_snackbar);
		} else {
			super.onActivityResult(request, result, data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.forum_actions, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		int itemId = item.getItemId();
		if (itemId == R.id.action_forum_share) {
			Intent i = new Intent(this, ShareForumActivity.class);
			i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivityForResult(i, REQUEST_SHARE_FORUM);
			return true;
		} else if (itemId == R.id.action_forum_sharing_status) {
			Intent i = new Intent(this, ForumSharingStatusActivity.class);
			i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i);
			return true;
		} else if (itemId == R.id.action_forum_delete) {
			showUnsubscribeDialog();
			return true;
		} else if (itemId == R.id.action_send_location) {
			openSendLocationScreen();
			return true;
		} else if (itemId == R.id.action_forum_voice_distortion) {
			toggleVoiceDistortion();
			return true;
		} else if (itemId == R.id.action_walkie_talkie) {
			toggleWalkieTalkie();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getMaxTextLength() {
		return MAX_FORUM_POST_TEXT_LENGTH;
	}

	private void showUnsubscribeDialog() {
		OnClickListener okListener = (dialog, which) -> viewModel.deleteForum();
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this,
				R.style.AnonDialogTheme);
		builder.setTitle(getString(R.string.dialog_title_leave_forum));
		builder.setMessage(getString(R.string.dialog_message_leave_forum));
		builder.setNegativeButton(R.string.dialog_button_leave, okListener);
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	// Location sending

	private void openSendLocationScreen() {
		Intent intent = new Intent(this, MapLocationPickerActivity.class);
		startActivityForResult(intent, REQUEST_SEND_LOCATION);
	}

	// Image sending

	private void onImageSelected(@Nullable Uri uri) {
		if (uri == null) return;
		String text = textInput.getText();
		textInput.clearText();
		MessageId replyId = viewModel.getCurrentReplyId();
		new Thread(() -> {
			try {
				String mimeType = getContentResolver().getType(uri);
				if (mimeType == null) mimeType = "image/jpeg";
				InputStream is = getContentResolver().openInputStream(uri);
				if (is == null) {
					runOnUiThread(() -> Toast.makeText(this,
							getString(R.string.image_compression_failed),
							Toast.LENGTH_SHORT).show());
					return;
				}
				InputStream compressed = imageCompressor.compressImage(
						is, mimeType, MAX_FORUM_IMAGE_SIZE);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int len;
				while ((len = compressed.read(buf)) != -1)
					bos.write(buf, 0, len);
				byte[] imageBytes = bos.toByteArray();
				runOnUiThread(() -> {
					viewModel.createAndStoreImageMessage(
							imageBytes, "image/jpeg", replyId, text);
					viewModel.clearReplyId();
					Toast.makeText(this,
							getString(R.string.image_sent),
							Toast.LENGTH_SHORT).show();
				});
			} catch (IOException e) {
				runOnUiThread(() -> Toast.makeText(this,
						getString(R.string.image_compression_failed),
						Toast.LENGTH_SHORT).show());
			}
		}).start();
	}

	// Voice recording

	private void startRecording() {
		if (ContextCompat.checkSelfPermission(this,
				android.Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{android.Manifest.permission.RECORD_AUDIO},
					1001);
			Toast.makeText(this,
					getString(R.string.microphone_permission_required),
					Toast.LENGTH_SHORT).show();
			return;
		}

		int sampleRate = 16000;
		int channelConfig = AudioFormat.CHANNEL_IN_MONO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate,
				channelConfig, audioFormat) * 3;

		audioRecord = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				sampleRate, channelConfig, audioFormat, bufferSize);

		audioRecord.startRecording();
		isRecording = true;

		recordingThread = new Thread(() -> {
			try {
				ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();
				byte[] buffer = new byte[bufferSize];
				while (isRecording) {
					int read = audioRecord.read(buffer, 0, buffer.length);
					if (read > 0) {
						pcmOut.write(buffer, 0, read);
					}
				}
				recordedPcmData = pcmOut.toByteArray();
			} catch (Exception e) {
				Log.e("ForumActivity", "Error recording audio", e);
			}
		}, "ForumAudioRecorder");

		recordingThread.start();
	}

	private void stopAndSendRecording() {
		try {
			isRecording = false;
			if (audioRecord != null &&
					audioRecord.getRecordingState() ==
							AudioRecord.RECORDSTATE_RECORDING) {
				audioRecord.stop();
			}
			if (audioRecord != null) {
				audioRecord.release();
				audioRecord = null;
			}

			recordingTimerHandler.removeCallbacks(recordingTimerRunnable);

			try {
				Thread.sleep(100);
				if (recordingThread != null) {
					recordingThread.join();
					recordingThread = null;
				}
			} catch (InterruptedException e) {
				Log.e("ForumActivity",
						"Recording thread interrupted", e);
				Toast.makeText(this,
						getString(R.string.recording_error),
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (recordedPcmData == null || recordedPcmData.length == 0) {
				Toast.makeText(this, getString(R.string.recording_error),
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (recordedPcmData.length < 1024) {
				Toast.makeText(this, getString(R.string.recording_too_short),
						Toast.LENGTH_SHORT).show();
				return;
			}

			boolean isDistorted = isVoiceDistortionEnabled();
			MessageId replyId = viewModel.getCurrentReplyId();

			File wavFile = new File(getCacheDir(),
					isDistorted ? "forum_distorted.wav"
							: "forum_recording.wav");
			File oggFile = new File(getCacheDir(), "forum_audio.ogg");

			new Thread(() -> {
				try {
					byte[] pcmToWrite = recordedPcmData;

					if (isDistorted) {
						pcmToWrite = AudioUtils.distortPcm(recordedPcmData);
						if (pcmToWrite == null) {
							runOnUiThread(() -> Toast.makeText(this,
									getString(R.string.distortion_failed),
									Toast.LENGTH_SHORT).show());
							return;
						}
					}

					AudioUtils.writeWavFile(wavFile, pcmToWrite, 16000, 1);

					String ffmpegCommand = String.format(
							"-y -i \"%s\" -c:a opus -strict -2 -b:a 16k \"%s\"",
							wavFile.getAbsolutePath(),
							oggFile.getAbsolutePath());

					FFmpegKit.executeAsync(ffmpegCommand, session -> {
						ReturnCode returnCode = session.getReturnCode();

						if (ReturnCode.isSuccess(returnCode)
								&& oggFile.exists()) {
							try {
								byte[] oggData = readFileBytes(oggFile);
								runOnUiThread(() -> {
									viewModel.createAndStoreAudioMessage(
											oggData, "audio/ogg", replyId);
									viewModel.clearReplyId();
									Toast.makeText(this,
											getString(R.string.voice_sent),
											Toast.LENGTH_SHORT).show();
								});
							} catch (IOException e) {
								runOnUiThread(() -> Toast.makeText(this,
										getString(R.string.conversion_failed),
										Toast.LENGTH_SHORT).show());
							}
						} else {
							runOnUiThread(() -> Toast.makeText(this,
									getString(R.string.conversion_failed),
									Toast.LENGTH_SHORT).show());
						}

						wavFile.delete();
						oggFile.delete();
					});
				} catch (Exception e) {
					runOnUiThread(() -> Toast.makeText(this,
							getString(R.string.processing_error),
							Toast.LENGTH_SHORT).show());
					e.printStackTrace();
				}
			}).start();

		} catch (Exception e) {
			Toast.makeText(this,
					getString(R.string.general_sendaudio_failure),
					Toast.LENGTH_SHORT).show();
			Log.e("ForumActivity", "Catch all error", e);
		}
	}

	private void cancelRecording() {
		try {
			isRecording = false;
			recordingTimerHandler.removeCallbacks(recordingTimerRunnable);
			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
			}
			Toast.makeText(this, getString(R.string.recording_cancelled),
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private byte[] readFileBytes(File file) throws IOException {
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int len;
		while ((len = fis.read(buf)) != -1) bos.write(buf, 0, len);
		fis.close();
		return bos.toByteArray();
	}

	// Voice distortion

	private boolean isVoiceDistortionEnabled() {
		SharedPreferences prefs = getSharedPreferences("forum_prefs",
				MODE_PRIVATE);
		return prefs.getBoolean(
				PREF_FORUM_DISTORTION + groupId.hashCode(), false);
	}

	private void toggleVoiceDistortion() {
		SharedPreferences prefs = getSharedPreferences("forum_prefs",
				MODE_PRIVATE);
		String key = PREF_FORUM_DISTORTION + groupId.hashCode();
		boolean current = prefs.getBoolean(key, false);
		prefs.edit().putBoolean(key, !current).apply();
		updateMicColor();
		Toast.makeText(this, !current
						? getString(R.string.distorted_voice_checkbox)
						: getString(R.string.voice_record),
				Toast.LENGTH_SHORT).show();
	}

	private void updateMicColor() {
		if (compositeSendButton != null) {
			compositeSendButton.setMicColor(isVoiceDistortionEnabled());
		}
	}

	// ---- Walkie-Talkie ----

	private boolean isWalkieTalkieEnabled() {
		SharedPreferences prefs =
				getSharedPreferences("forum_prefs", MODE_PRIVATE);
		return prefs.getBoolean(
				PREF_FORUM_WALKIE_TALKIE + groupId.hashCode(), false);
	}

	private void toggleWalkieTalkie() {
		SharedPreferences prefs =
				getSharedPreferences("forum_prefs", MODE_PRIVATE);
		String key = PREF_FORUM_WALKIE_TALKIE + groupId.hashCode();
		boolean current = prefs.getBoolean(key, false);
		prefs.edit().putBoolean(key, !current).apply();
		walkieTalkieEnabled = !current;
		updateWalkieTalkieBar();
		if (!walkieTalkieEnabled) {
			walkieTalkiePlayer.stop();
		}
		Toast.makeText(this, walkieTalkieEnabled
				? R.string.walkie_talkie_enabled
				: R.string.walkie_talkie_disabled,
				Toast.LENGTH_SHORT).show();
	}

	private void updateWalkieTalkieBar() {
		if (walkieTalkieBar == null) return;
		walkieTalkieBar.setVisibility(
				walkieTalkieEnabled ? View.VISIBLE : View.GONE);
		setWalkieTalkieBarIdle();
	}

	private void setWalkieTalkieBarIdle() {
		if (walkieTalkieBar == null) return;
		walkieTalkieBar.setText(R.string.walkie_talkie_bar_text);
		android.util.TypedValue tv = new android.util.TypedValue();
		getTheme().resolveAttribute(R.attr.colorPrimary, tv, true);
		walkieTalkieBar.setBackgroundColor(tv.data);
	}

	private void setWalkieTalkieBarRecording() {
		if (walkieTalkieBar == null) return;
		walkieTalkieBar.setText(R.string.walkie_talkie_recording);
		walkieTalkieBar.setBackgroundColor(
				androidx.core.content.ContextCompat.getColor(
						this, R.color.anon_red_500));
	}

	private void setWalkieTalkieBarPlaying(String senderName) {
		if (walkieTalkieBar == null) return;
		walkieTalkieBar.setText(getString(R.string.walkie_talkie_playing,
				senderName));
		walkieTalkieBar.setBackgroundColor(
				androidx.core.content.ContextCompat.getColor(
						this, R.color.md_theme_tertiary));
	}

	private void drainPendingAutoPlay() {
		for (android.util.Pair<byte[], String> p : pendingAutoPlay) {
			walkieTalkiePlayer.play(p.first, p.second);
		}
		pendingAutoPlay.clear();
	}

	private int getPttKeyCode() {
		SharedPreferences prefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		String value = prefs.getString("pref_key_ptt_button", "volume_up");
		return "volume_down".equals(value)
				? KeyEvent.KEYCODE_VOLUME_DOWN
				: KeyEvent.KEYCODE_VOLUME_UP;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (walkieTalkieEnabled) {
			int pttButton = getPttKeyCode();
			if (event.getKeyCode() == pttButton) {
				if (event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getRepeatCount() == 0
						&& !isWalkieTalkieRecording) {
					isWalkieTalkieRecording = true;
					startRecording();
					vibrateShort();
					recordingStartTime = System.currentTimeMillis();
					setWalkieTalkieBarRecording();
					recordingTimerHandler.postDelayed(
							walkieTalkieTimerRunnable, 250);
					return true;
				}
				if (event.getAction() == KeyEvent.ACTION_UP
						&& isWalkieTalkieRecording) {
					isWalkieTalkieRecording = false;
					recordingTimerHandler.removeCallbacks(
							walkieTalkieTimerRunnable);
					stopAndSendRecording();
					vibrateShort();
					setWalkieTalkieBarIdle();
					drainPendingAutoPlay();
					return true;
				}
				return true; // consume repeats
			}
		}
		return super.dispatchKeyEvent(event);
	}

	private void vibrateShort() {
		Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		if (v != null) {
			v.vibrate(VibrationEffect.createOneShot(50,
					VibrationEffect.DEFAULT_AMPLITUDE));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		walkieTalkiePlayer.stop();
		setWalkieTalkieBarIdle();
		if (isWalkieTalkieRecording) {
			isWalkieTalkieRecording = false;
			cancelRecording();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		walkieTalkiePlayer.release();
	}

}
