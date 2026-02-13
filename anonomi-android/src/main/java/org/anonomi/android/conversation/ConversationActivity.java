package org.anonomi.android.conversation;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.Slide;
import android.transition.Transition;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.Pair;
import org.anonchatsecure.bramble.api.connection.ConnectionRegistry;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.contact.event.ContactRemovedEvent;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.NoSuchContactException;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.plugin.event.ContactConnectedEvent;
import org.anonchatsecure.bramble.api.plugin.event.ContactDisconnectedEvent;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.sync.event.MessagesAckedEvent;
import org.anonchatsecure.bramble.api.sync.event.MessagesSentEvent;
import org.anonchatsecure.bramble.api.versioning.event.ClientVersionUpdatedEvent;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.attachment.AttachmentItem;
import org.anonomi.android.attachment.AttachmentRetriever;
import org.anonomi.android.blog.BlogActivity;
import org.anonomi.android.contact.connect.ConnectViaBluetoothActivity;
import org.anonomi.android.conversation.ConversationVisitor.AttachmentCache;
import org.anonomi.android.conversation.ConversationVisitor.TextCache;
import org.anonomi.android.forum.ForumActivity;
import org.anonomi.android.fragment.BaseFragment.BaseFragmentListener;
import org.anonomi.android.introduction.IntroductionActivity;
import org.anonomi.android.privategroup.conversation.GroupActivity;
import org.anonomi.android.removabledrive.RemovableDriveActivity;
import org.anonomi.android.util.BriarSnackbarBuilder;
import org.anonomi.android.view.BriarRecyclerView;
import org.anonomi.android.view.ImagePreview;
import org.anonomi.android.view.TextAttachmentController;
import org.anonomi.android.view.TextAttachmentController.AttachmentListener;
import org.anonomi.android.view.TextInputView;
import org.anonomi.android.view.TextSendController;
import org.anonomi.android.view.TextSendController.SendState;
import org.anonomi.android.widget.LinkDialogFragment;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.attachment.Attachment;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.autodelete.event.ConversationMessagesDeletedEvent;
import org.anonchatsecure.anonchat.api.blog.BlogSharingManager;
import org.anonchatsecure.anonchat.api.client.ProtocolStateException;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageHeader;
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageVisitor;
import org.anonchatsecure.anonchat.api.conversation.ConversationRequest;
import org.anonchatsecure.anonchat.api.conversation.ConversationResponse;
import org.anonchatsecure.anonchat.api.conversation.DeletionResult;
import org.anonchatsecure.anonchat.api.conversation.event.ConversationMessageReceivedEvent;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.introduction.IntroductionManager;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;
import org.anonomi.android.map.MapViewActivity;

import androidx.activity.result.contract.ActivityResultContracts;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.anonomi.android.util.WalkieTalkiePlayer;
import org.anonomi.android.view.CompositeSendButton;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionPredicates;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.SelectionTracker.SelectionObserver;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import org.anonomi.android.map.MapLocationPickerActivity;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;

import org.anonomi.android.util.AudioUtils;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import android.content.pm.PackageManager;

import static android.view.Gravity.RIGHT;
import static android.widget.Toast.LENGTH_SHORT;
import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static androidx.lifecycle.Lifecycle.State.STARTED;
import static androidx.recyclerview.widget.SortedList.INVALID_POSITION;
import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.LogUtils.now;
import static org.anonchatsecure.bramble.util.StringUtils.fromHexString;
import static org.anonchatsecure.bramble.util.StringUtils.isNullOrEmpty;
import static org.anonchatsecure.bramble.util.StringUtils.join;
import static org.anonomi.android.activity.RequestCodes.REQUEST_INTRODUCTION;
import static org.anonomi.android.conversation.ImageActivity.ATTACHMENTS;
import static org.anonomi.android.conversation.ImageActivity.ATTACHMENT_POSITION;
import static org.anonomi.android.conversation.ImageActivity.DATE;
import static org.anonomi.android.conversation.ImageActivity.ITEM_ID;
import static org.anonomi.android.conversation.ImageActivity.NAME;
import static org.anonomi.android.util.UiUtils.launchActivityToOpenFile;
import static org.anonomi.android.util.UiUtils.observeOnce;
import static org.anonomi.android.view.AuthorView.setAvatar;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_ATTACHMENTS_PER_MESSAGE;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES_AUTO_DELETE;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_ONLY;


@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ConversationActivity extends BriarActivity
		implements BaseFragmentListener, EventListener, ConversationListener,
		TextCache, AttachmentCache, AttachmentListener, ActionMode.Callback {

	public static final String CONTACT_ID = "briar.CONTACT_ID";
	private static final int REQUEST_SEND_LOCATION = 2001;

	private static final Logger LOG =
			getLogger(ConversationActivity.class.getName());

	private static final int TRANSITION_DURATION_MS = 500;
	private static final int ONBOARDING_DELAY_MS = 250;

	@Inject
	AndroidNotificationManager notificationManager;
	@Inject
	ConnectionRegistry connectionRegistry;
	@Inject
	ViewModelProvider.Factory viewModelFactory;
	@Inject
	FeatureFlags featureFlags;

	// Fields that are accessed from background threads must be volatile
	@Inject
	volatile ContactManager contactManager;
	@Inject
	volatile MessagingManager messagingManager;
	@Inject
	volatile ConversationManager conversationManager;
	@Inject
	volatile EventBus eventBus;
	@Inject
	volatile IntroductionManager introductionManager;
	@Inject
	volatile ForumSharingManager forumSharingManager;
	@Inject
	volatile BlogSharingManager blogSharingManager;
	@Inject
	volatile GroupInvitationManager groupInvitationManager;

	@Inject
	volatile PrivateMessageFactory privateMessageFactory;

	private final Map<MessageId, String> textCache = new ConcurrentHashMap<>();
	private final Observer<String> contactNameObserver = name -> {
		requireNonNull(name);
		loadMessages();
	};

	private final ActivityResultLauncher<String[]> docLauncher =
			registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onImageSelected);

	private final ActivityResultLauncher<String> contentLauncher =
			registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImageSelected);

	private ActivityResultLauncher<String[]> audioLauncher;


	private TextView slideToCancelText;
	private AttachmentRetriever attachmentRetriever;
	private ConversationViewModel viewModel;
	private ConversationVisitor visitor;
	private ConversationAdapter adapter;
	private Toolbar toolbar;
	private CircleImageView toolbarAvatar;
	private ImageView toolbarStatus;
	private TextView toolbarTitle;
	private BriarRecyclerView list;
	private LinearLayoutManager layoutManager;
	private TextInputView textInputView;
	private TextSendController sendController;
	private SelectionTracker<String> tracker;
	private CompositeSendButton compositeSendButton;
	private volatile byte[] recordedPcmData;
	@Nullable
	private Parcelable layoutManagerState;
	@Nullable
	private ActionMode actionMode;

	private AudioRecord audioRecord;
	private Thread recordingThread;
	private boolean isRecording = false;

	// Walkie-Talkie
	private static final String PREF_WALKIE_TALKIE = "walkie_talkie_";
	private volatile boolean walkieTalkieEnabled = false;
	private boolean isWalkieTalkieRecording = false;
	private volatile String contactDisplayName = "";
	private TextView walkieTalkieBar;
	private WalkieTalkiePlayer walkieTalkiePlayer;

	private final Runnable walkieTalkieTimerRunnable = new Runnable() {
		@Override
		public void run() {
			if (isWalkieTalkieRecording) {
				long elapsed = System.currentTimeMillis() - recordingStartTime;
				int seconds = (int) (elapsed / 1000);

				if (seconds >= 11) {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ConversationActivity.this, R.color.anon_red_700));
				} else if (seconds >= 10) {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ConversationActivity.this, R.color.anon_orange_500));
				} else {
					walkieTalkieBar.setBackgroundColor(ContextCompat.getColor(
							ConversationActivity.this, R.color.anon_red_500));
				}
				walkieTalkieBar.setText(getString(
						R.string.walkie_talkie_recording_timer, seconds));

				if (seconds >= 12) {
					isWalkieTalkieRecording = false;
					recordingTimerHandler.removeCallbacks(this);
					stopAndSendRecording();
					vibrateShort();
					setWalkieTalkieBarIdle();
				} else {
					recordingTimerHandler.postDelayed(this, 250);
				}
			}
		}
	};

	private volatile ContactId contactId;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(ConversationViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		// Spurious lint warning - using END causes a crash
		@SuppressLint("RtlHardcoded")
		Transition slide = new Slide(RIGHT);
		slide.setDuration(TRANSITION_DURATION_MS);
		setSceneTransitionAnimation(slide, null, slide);
		super.onCreate(state);

		Intent i = getIntent();
		int id = i.getIntExtra(CONTACT_ID, -1);
		if (id == -1) throw new IllegalStateException();
		contactId = new ContactId(id);

		viewModel.setContactId(contactId);
		audioLauncher = registerForActivityResult(
				new ActivityResultContracts.OpenMultipleDocuments(),
				uris -> {
					if (uris != null && !uris.isEmpty()) {
						Uri uri = uris.get(0);
						LiveData<SendState> result = viewModel.sendAudioAttachment(uri);
						result.observe(this, sendState -> {
							if (sendState == SendState.SENT) {
								Toast.makeText(this, getString(R.string.audio_sent), Toast.LENGTH_SHORT).show();
							} else if (sendState == SendState.ERROR) {
								Toast.makeText(this, getString(R.string.audio_send_failed), Toast.LENGTH_SHORT).show();
							}
						});
					}
				});


		attachmentRetriever = viewModel.getAttachmentRetriever();

		setContentView(R.layout.activity_conversation);

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

		slideToCancelText = findViewById(R.id.slideToCancelText);

		// Custom Toolbar
		toolbar = requireNonNull(setUpCustomToolbar(true));
		toolbarAvatar = toolbar.findViewById(R.id.contactAvatar);
		toolbarStatus = toolbar.findViewById(R.id.contactStatus);
		toolbarTitle = toolbar.findViewById(R.id.contactName);
		compositeSendButton = findViewById(R.id.compositeSendButton);

		viewModel.getContactItem().observe(this, contactItem -> {
			requireNonNull(contactItem);
			setAvatar(toolbarAvatar, contactItem);
			compositeSendButton.setMicColor(contactItem.getContact().isDistortedVoiceEnabled());
		});
		viewModel.getContactDisplayName().observe(this, contactName -> {
			requireNonNull(contactName);
			toolbarTitle.setText(contactName);
			contactDisplayName = contactName;
		});
		viewModel.isContactDeleted().observe(this, deleted -> {
			requireNonNull(deleted);
			if (deleted) finish();
		});
		viewModel.getAddedPrivateMessage().observeEvent(this,
				this::onAddedPrivateMessage);

		visitor = new ConversationVisitor(this, this, this,
				viewModel.getContactDisplayName());
		adapter = new ConversationAdapter(this, this, attachmentRetriever);
		list = findViewById(R.id.conversationView);
		layoutManager = new LinearLayoutManager(this);
		list.setLayoutManager(layoutManager);
		list.setAdapter(adapter);
		list.setEmptyText(getString(R.string.no_private_messages));
		ConversationScrollListener scrollListener =
				new ConversationScrollListener(adapter, viewModel);
		list.getRecyclerView().addOnScrollListener(scrollListener);
		addSelectionTracker();

		textInputView = findViewById(R.id.text_input_container);


		// 🎤 Hook up voice recording button
		AppCompatImageButton recordButton = textInputView.findViewById(R.id.recordButton);
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
						slideToCancelText.setText(getString(R.string.slide_to_cancel));
						slideToCancelText.setAlpha(1f);
						slideToCancelText.setVisibility(View.VISIBLE);
						startRecording();
						recordingStartTime = System.currentTimeMillis(); // NEW
						recordingTimerHandler.post(recordingTimerRunnable); // NEW

						return true;

					case MotionEvent.ACTION_MOVE:
						float deltaX = event.getX() - startX;
						if (Math.abs(deltaX) > SWIPE_CANCEL_THRESHOLD && !cancelled) {
							cancelRecording();
							cancelled = true;
							slideToCancelText.setText(R.string.recording_cancelled);
							slideToCancelText.animate()
									.alpha(0f)
									.setDuration(500)
									.withEndAction(() -> slideToCancelText.setVisibility(View.GONE))
									.start();
						}
						return true;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						if (!cancelled) stopAndSendRecording();
						slideToCancelText.animate()
								.alpha(0f)
								.setDuration(300)
								.withEndAction(() -> slideToCancelText.setVisibility(View.GONE))
								.start();
						return true;
				}
				return false;
			}
		});


		if (featureFlags.shouldEnableImageAttachments()) {
			ImagePreview imagePreview = findViewById(R.id.imagePreview);
			sendController = new TextAttachmentController(textInputView,
					imagePreview, this, viewModel);
			observeOnce(viewModel.getPrivateMessageFormat(), this, format -> {
				if (format != TEXT_ONLY) {
					// TODO: remove cast when removing feature flag
					((TextAttachmentController) sendController)
							.setImagesSupported();
				}
			});
		} else {
			sendController = new TextSendController(textInputView, this, false);
		}
		textInputView.setSendController(sendController);
		textInputView.setMaxTextLength(MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		textInputView.setReady(false);
		textInputView.setOnKeyboardShownListener(this::scrollToBottom);

		viewModel.getAutoDeleteTimer().observe(this, timer ->
				sendController.setAutoDeleteTimer(timer));
	}

	private MediaRecorder recorder;
	private File audioFile;

	private void startRecording() {
		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{android.Manifest.permission.RECORD_AUDIO}, 1001);
			Toast.makeText(this, getString(R.string.microphone_permission_required), Toast.LENGTH_SHORT).show();
			return;
		}

		int sampleRate = 16000; // 16 kHz is a good balance
		int channelConfig = AudioFormat.CHANNEL_IN_MONO;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 3;

		audioRecord = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				sampleRate,
				channelConfig,
				audioFormat,
				bufferSize
		);

		audioFile = new File(getCacheDir(), "recording.wav");

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
				AudioUtils.writeWavFile(audioFile, recordedPcmData, sampleRate, 1);

			} catch (IOException e) {
				Log.e("ConversationActivity", "Error writing WAV file", e);
			}
		}, "AudioRecorder Thread");

		recordingThread.start();
	}

	private long recordingStartTime;
	private final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
	private final Runnable recordingTimerRunnable = new Runnable() {
		@Override
		public void run() {
			if (isRecording) {
				long elapsed = System.currentTimeMillis() - recordingStartTime;
				int seconds = (int) (elapsed / 1000);

				if (seconds >= 11) {
					slideToCancelText.setTextColor(ContextCompat.getColor(ConversationActivity.this, R.color.anon_red_500));
				} else if (seconds >= 10) {
					slideToCancelText.setTextColor(ContextCompat.getColor(ConversationActivity.this, R.color.anon_orange_500));
				} else {
					slideToCancelText.setTextColor(ContextCompat.getColor(ConversationActivity.this, R.color.white));
				}
				slideToCancelText.setText(getString(R.string.slide_to_cancel_with_timer, seconds));

				if (seconds >= 12) {
					slideToCancelText.setText(getString(R.string.recording_max_reached));
					stopAndSendRecording();
				} else {
					recordingTimerHandler.postDelayed(this, 250);
				}
			}
		}
	};

	private void stopAndSendRecording() {
		try {
			isRecording = false;
			if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
				audioRecord.stop();
			}
			audioRecord.release();
			audioRecord = null;

			recordingTimerHandler.removeCallbacks(recordingTimerRunnable);

			try {
				Thread.sleep(100); // 100ms
				recordingThread.join();  // ✅ Ensure PCM recording has ended
				recordingThread = null;
			} catch (InterruptedException e) {
				Log.e("ConversationActivity", "Recording thread interrupted", e);
				Toast.makeText(this, getString(R.string.recording_error), Toast.LENGTH_SHORT).show();
				return;
			}

			if (recordedPcmData == null || recordedPcmData.length == 0) {
				Toast.makeText(this, getString(R.string.recording_error), Toast.LENGTH_SHORT).show();
				return;
			}

			if (recordedPcmData.length < 1024) { // Less than 0.06 seconds of audio at 16kHz
				Toast.makeText(this, getString(R.string.recording_too_short), Toast.LENGTH_SHORT).show();
				return;
			}

			boolean isDistorted = viewModel.getContactItem().getValue()
					.getContact().isDistortedVoiceEnabled();

			File wavFile = new File(getCacheDir(), isDistorted ? "distorted.wav" : "recording.wav");
			File oggFile = new File(getCacheDir(), "distorted.ogg");

			new Thread(() -> {
				try {
					byte[] pcmToWrite = recordedPcmData;

					if (isDistorted) {
						pcmToWrite = AudioUtils.distortPcm(recordedPcmData);
						if (pcmToWrite == null) {
							runOnUiThread(() -> Toast.makeText(this, getString(R.string.distortion_failed), Toast.LENGTH_SHORT).show());
							return;
						}
					}

					AudioUtils.writeWavFile(wavFile, pcmToWrite, 16000, 1);  // mono, 16kHz

					String ffmpegCommand = String.format(
							"-y -i \"%s\" -c:a opus -strict -2 -b:a 16k \"%s\"",
							wavFile.getAbsolutePath(), oggFile.getAbsolutePath()
					);

					FFmpegKit.executeAsync(ffmpegCommand, session -> {
						ReturnCode returnCode = session.getReturnCode();

						runOnUiThread(() -> {
							if (ReturnCode.isSuccess(returnCode)) {
								if (oggFile.exists()) {
									viewModel.sendAudioAttachment(Uri.fromFile(oggFile))
											.observe(this, sendState -> {
												if (sendState == SendState.SENT) {
													Toast.makeText(this, getString(R.string.voice_sent), Toast.LENGTH_SHORT).show();
												} else {
													Toast.makeText(this, getString(R.string.general_sendaudio_failure), Toast.LENGTH_SHORT).show();
												}

												// ✅ Only delete after LiveData observer finishes
												wavFile.delete();
												oggFile.delete();
											});
								} else {
									Toast.makeText(this, getString(R.string.conversion_failed), Toast.LENGTH_SHORT).show();
								}
							} else {
								Toast.makeText(this, getString(R.string.conversion_failed), Toast.LENGTH_SHORT).show();
							}

						});
					});
				} catch (Exception e) {
					runOnUiThread(() -> Toast.makeText(this, getString(R.string.processing_error), Toast.LENGTH_SHORT).show());
					e.printStackTrace();
				}
			}).start();

		} catch (Exception e) {
			Toast.makeText(this, getString(R.string.general_sendaudio_failure), Toast.LENGTH_SHORT).show();
			Log.e("ConversationActivity", "Catch all error", e);
			e.printStackTrace();
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
			if (audioFile != null && audioFile.exists()) {
				audioFile.delete();
			}
			Toast.makeText(this, getString(R.string.recording_cancelled), Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void onImageSelected(@Nullable Uri uri) {
		if (uri == null) return;

		String mimeType = getContentResolver().getType(uri);
		if (mimeType == null) {
			Toast.makeText(this, getString(R.string.unsupported_file_type), Toast.LENGTH_SHORT).show();
			return;
		}

		if (mimeType.startsWith("image/")) {
			List<Uri> uris = new ArrayList<>();
			uris.add(uri);
			((TextAttachmentController) sendController).onImageReceived(uris);
		} else if (mimeType.startsWith("audio/")) {
			LiveData<SendState> result = viewModel.sendAudioAttachment(uri);
			result.observe(this, sendState -> {
				if (sendState == SendState.SENT) {
					Toast.makeText(this, getString(R.string.audio_sent), Toast.LENGTH_SHORT).show();
				} else if (sendState == SendState.ERROR) {
					Toast.makeText(this, getString(R.string.audio_send_failed), Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			Toast.makeText(this, getString(R.string.unsupported_file_type), Toast.LENGTH_SHORT).show();
		}
	}

	private void scrollToBottom() {
		int items = adapter.getItemCount();
		if (items > 0) list.scrollToPosition(items - 1);
	}



	@Override
	protected void onActivityResult(int request, int result, @Nullable Intent data) {
		super.onActivityResult(request, result, data);

		if (request == REQUEST_INTRODUCTION && result == RESULT_OK) {
			new BriarSnackbarBuilder()
					.make(list, R.string.introduction_sent, Snackbar.LENGTH_SHORT)
					.show();
		} else if (request == REQUEST_SEND_LOCATION && result == RESULT_OK && data != null) {
			String message = data.getStringExtra(MapLocationPickerActivity.RESULT_MAP_MESSAGE);
			if (message != null) {
				sendMapMessage(message);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		eventBus.addListener(this);
		notificationManager.blockContactNotification(contactId);
		notificationManager.clearContactNotification(contactId);
		displayContactOnlineStatus();
		viewModel.getContactDisplayName().observe(this, contactNameObserver);
		list.startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		eventBus.removeListener(this);
		notificationManager.unblockContactNotification(contactId);
		viewModel.getContactDisplayName().removeObserver(contactNameObserver);
		list.stopPeriodicUpdate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (layoutManager != null) {
			layoutManagerState = layoutManager.onSaveInstanceState();
			outState.putParcelable("layoutManager", layoutManagerState);
		}
		if (tracker != null) tracker.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		layoutManagerState = savedInstanceState.getParcelable("layoutManager");
		if (tracker != null) tracker.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.conversation_actions, menu);

		// enable introduction action if available
		observeOnce(viewModel.showIntroductionAction(), this, enable -> {
			if (enable != null && enable) {
				menu.findItem(R.id.action_introduction).setEnabled(true);
				// show introduction onboarding, if needed
				viewModel.showIntroductionOnboarding().observeEvent(this,
						this::showIntroductionOnboarding);
			}
		});
		// enable alias and bluetooth action once available
		observeOnce(viewModel.getContactItem(), this, contact -> {
			menu.findItem(R.id.action_set_alias).setEnabled(true);
			menu.findItem(R.id.action_connect_via_bluetooth).setEnabled(true);
		});
		// Show auto-delete menu item if feature is enabled
		if (featureFlags.shouldEnableDisappearingMessages()) {
			MenuItem item = menu.findItem(R.id.action_conversation_settings);
			item.setVisible(true);
			// Enable menu item only if contact supports auto-delete
			viewModel.getPrivateMessageFormat().observe(this, format ->
					item.setEnabled(format == TEXT_IMAGES_AUTO_DELETE));
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// contactId gets set before in onCreate()
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
			return true;
		} else if (itemId == R.id.action_introduction) {
			Intent intent = new Intent(this, IntroductionActivity.class);
			intent.putExtra(CONTACT_ID, contactId.getInt());
			startActivityForResult(intent, REQUEST_INTRODUCTION);
			return true;
		} else if (itemId == R.id.action_set_alias) {
			AliasDialogFragment.newInstance().show(
					getSupportFragmentManager(), AliasDialogFragment.TAG);
			return true;
		} else if (itemId == R.id.action_conversation_settings) {
			onAutoDeleteTimerNoticeClicked();
			return true;
		} else if (itemId == R.id.action_connect_via_bluetooth) {
			Intent intent = new Intent(this, ConnectViaBluetoothActivity.class);
			intent.putExtra(CONTACT_ID, contactId.getInt());
			startActivity(intent);
			return true;
		} else if (itemId == R.id.action_transfer_data) {
			Intent intent = new Intent(this, RemovableDriveActivity.class);
			intent.putExtra(CONTACT_ID, contactId.getInt());
			startActivity(intent);
			return true;
		} else if (itemId == R.id.action_delete_all_messages) {
			askToDeleteAllMessages();
			return true;
		} else if (itemId == R.id.action_social_remove_person) {
			askToRemoveContact();
			return true;
		} else if (item.getItemId() == R.id.action_send_xmr) {
			openSendXmrDialog();
			return true;
		} else if (itemId == R.id.action_send_location) {
			openSendLocationScreen();
			return true;
		} else if (itemId == R.id.action_walkie_talkie) {
			toggleWalkieTalkie();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.conversation_message_actions, menu);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false; // no update needed
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		if (item.getItemId() == R.id.action_delete) {
			deleteSelectedMessages();
			return true;
		}
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		tracker.clearSelection();
		actionMode = null;
	}

	@Override
	public void onLinkClick(String url) {
		LinkDialogFragment f = LinkDialogFragment.newInstance(url);
		f.show(getSupportFragmentManager(), f.getUniqueTag());
	}

	private void addSelectionTracker() {
		RecyclerView recyclerView = list.getRecyclerView();
		if (recyclerView.getAdapter() != adapter)
			throw new IllegalStateException();

		tracker = new SelectionTracker.Builder<>(
				"conversationSelection",
				recyclerView,
				new ConversationItemKeyProvider(adapter),
				new ConversationItemDetailsLookup(recyclerView),
				StorageStrategy.createStringStorage()
		).withSelectionPredicate(
				SelectionPredicates.createSelectAnything()
		).build();

		SelectionObserver<String> observer = new SelectionObserver<String>() {
			@Override
			public void onItemStateChanged(String key, boolean selected) {
				if (selected && actionMode == null) {
					actionMode = startActionMode(ConversationActivity.this);
					updateActionModeTitle();
				} else if (actionMode != null) {
					if (selected || tracker.hasSelection()) {
						updateActionModeTitle();
					} else {
						actionMode.finish();
					}
				}
			}
		};
		tracker.addObserver(observer);
		adapter.setSelectionTracker(tracker);
	}

	private void updateActionModeTitle() {
		if (actionMode == null) throw new IllegalStateException();
		String title = String.valueOf(tracker.getSelection().size());
		actionMode.setTitle(title);
	}

	private Collection<MessageId> getSelection() {
		Selection<String> selection = tracker.getSelection();
		List<MessageId> messages = new ArrayList<>(selection.size());
		for (String str : selection) {
			try {
				MessageId id = new MessageId(fromHexString(str));
				messages.add(id);
			} catch (FormatException e) {
				LOG.warning("Invalid message id");
			}
		}
		return messages;
	}

	@UiThread
	private void displayContactOnlineStatus() {
		if (connectionRegistry.isConnected(contactId)) {
			toolbarStatus.setImageResource(R.drawable.contact_online);
			toolbarStatus.setContentDescription(getString(R.string.online));
		} else {
			toolbarStatus.setImageResource(R.drawable.contact_offline);
			toolbarStatus.setContentDescription(getString(R.string.offline));
		}
	}

	private void openSendXmrDialog() {
		Intent intent = new Intent(this, RequestXmrActivity.class);
		intent.putExtra("CONTACT_ID", contactId.getInt());
		startActivity(intent);
	}

	private void openSendLocationScreen() {
		Intent intent = new Intent(this, MapLocationPickerActivity.class);
		startActivityForResult(intent, REQUEST_SEND_LOCATION);
	}

	private void sendMapMessage(String messageText) {
		viewModel.sendMapMessage(messageText).observe(this, state -> {
			if (state == SendState.SENT) {
				Toast.makeText(this, R.string.location_sent, Toast.LENGTH_SHORT).show();
				loadMessages();  // refresh UI
			} else if (state == SendState.UNEXPECTED_TIMER) {
				Toast.makeText(this, "unexpected timer error", Toast.LENGTH_SHORT).show();
			} else if (state == SendState.ERROR) {
				Toast.makeText(this, R.string.error_creating_message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	private void loadMessages() {
		int revision = adapter.getRevision();
		runOnDbThread(() -> {
			try {
				long start = now();
				Collection<ConversationMessageHeader> headers =
						conversationManager.getMessageHeaders(contactId);
				logDuration(LOG, "Loading messages", start);
				// Sort headers by timestamp in *descending* order
				List<ConversationMessageHeader> sorted =
						new ArrayList<>(headers);
				sort(sorted, (a, b) ->
						Long.compare(b.getTimestamp(), a.getTimestamp()));
				if (!sorted.isEmpty()) {
					// If the latest header is a private message, eagerly load
					// its size so we can set the scroll position correctly
					ConversationMessageHeader latest = sorted.get(0);
					if (latest instanceof PrivateMessageHeader) {
						eagerlyLoadMessageSize((PrivateMessageHeader) latest);
					}
				}
				displayMessages(revision, sorted);
			} catch (NoSuchContactException e) {
				finishOnUiThread();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@DatabaseExecutor
	private void eagerlyLoadMessageSize(PrivateMessageHeader h) {
		try {
			MessageId id = h.getId();
			// If the message has text, load it
			if (h.hasText()) {
				String text = textCache.get(id);
				if (text == null) {
					LOG.info("Eagerly loading text for latest message");
					text = messagingManager.getMessageText(id);
					textCache.put(id, requireNonNull(text));
				}
			}
			// If the message has a single image, load its size - for multiple
			// images we use a grid so the size is fixed
			List<AttachmentHeader> headers = h.getAttachmentHeaders();
			if (headers.size() == 1) {
				LOG.info("Eagerly loading image size for latest message");
				AttachmentHeader header = headers.get(0);
				// get the item to retrieve its size
				attachmentRetriever
						.cacheAttachmentItemWithSize(h.getId(), header);
			}
		} catch (DbException e) {
			logException(LOG, WARNING, e);
		}
	}

	private void displayMessages(int revision,
			Collection<ConversationMessageHeader> headers) {
		runOnUiThreadUnlessDestroyed(() -> {
			if (revision == adapter.getRevision()) {
				adapter.incrementRevision();
				textInputView.setReady(true);
				// start observing onboarding after enabling
				if (featureFlags.shouldEnableImageAttachments()) {
					viewModel.showImageOnboarding().observeEvent(this,
							this::showImageOnboarding);
				}
				List<ConversationItem> items = createItems(headers);
				adapter.replaceAll(items);
				list.showData();
				if (layoutManagerState == null) {
					scrollToBottom();
				} else {
					// Restore the previous scroll position
					layoutManager.onRestoreInstanceState(layoutManagerState);
				}
			} else {
				LOG.info("Concurrent update, reloading");
				loadMessages();
			}
		});
	}

	/**
	 * Creates ConversationItems from headers loaded from the database.
	 * <p>
	 * Attention: Call this only after contactName has been initialized.
	 */
	private List<ConversationItem> createItems(
			Collection<ConversationMessageHeader> headers) {
		List<ConversationItem> items = new ArrayList<>(headers.size());
		for (ConversationMessageHeader h : headers)
			items.add(h.accept(visitor));
		return items;
	}

	private void loadMessageText(MessageId m) {
		runOnDbThread(() -> {
			try {
				long start = now();
				String text = messagingManager.getMessageText(m);
				logDuration(LOG, "Loading text", start);
				displayMessageText(m, requireNonNull(text));
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void displayMessageText(MessageId m, String text) {
		runOnUiThreadUnlessDestroyed(() -> {
			textCache.put(m, text);
			Pair<Integer, ConversationMessageItem> pair =
					adapter.getMessageItem(m);
			if (pair != null) {
				boolean scroll = shouldScrollWhenUpdatingMessage();
				pair.getSecond().setText(text);
				adapter.notifyItemChanged(pair.getFirst());
				if (scroll) scrollToBottom();
			}
		});
	}

	// When a message's text or attachments are loaded, scroll to the bottom
	// if the conversation is visible and we were previously at the bottom
	private boolean shouldScrollWhenUpdatingMessage() {
		return getLifecycle().getCurrentState().isAtLeast(STARTED)
				&& adapter.isScrolledToBottom(layoutManager);
	}

	@UiThread
	private void updateMessageAttachment(MessageId m, AttachmentItem item) {
		Pair<Integer, ConversationMessageItem> pair = adapter.getMessageItem(m);
		if (pair != null && pair.getSecond().updateAttachments(item)) {
			boolean scroll = shouldScrollWhenUpdatingMessage();
			adapter.notifyItemChanged(pair.getFirst());
			if (scroll) scrollToBottom();
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			ContactRemovedEvent c = (ContactRemovedEvent) e;
			if (c.getContactId().equals(contactId)) {
				//LOG.info("Contact removed");
				supportFinishAfterTransition();
			}
		} else if (e instanceof ConversationMessageReceivedEvent) {
			ConversationMessageReceivedEvent<?> p =
					(ConversationMessageReceivedEvent<?>) e;
			if (p.getContactId().equals(contactId)) {
				onNewConversationMessage(p.getMessageHeader());
				if (walkieTalkieEnabled) {
					ConversationMessageHeader hdr = p.getMessageHeader();
					if (!hdr.isLocal() && hdr instanceof PrivateMessageHeader) {
						PrivateMessageHeader pmh = (PrivateMessageHeader) hdr;
						for (AttachmentHeader ah : pmh.getAttachmentHeaders()) {
							if (ah.getContentType().startsWith("audio/")) {
								loadAndPlayAttachment(ah, contactDisplayName);
								break;
							}
						}
					}
				}
			}
		} else if (e instanceof MessagesSentEvent) {
			MessagesSentEvent m = (MessagesSentEvent) e;
			if (m.getContactId().equals(contactId)) {
				//LOG.info("Messages sent");
				markMessages(m.getMessageIds(), true, false);
			}
		} else if (e instanceof MessagesAckedEvent) {
			MessagesAckedEvent m = (MessagesAckedEvent) e;
			if (m.getContactId().equals(contactId)) {
				//LOG.info("Messages acked");
				markMessages(m.getMessageIds(), true, true);
			}
		} else if (e instanceof ConversationMessagesDeletedEvent) {
			ConversationMessagesDeletedEvent m =
					(ConversationMessagesDeletedEvent) e;
			if (m.getContactId().equals(contactId)) {
				//LOG.info("Messages auto-deleted");
				onConversationMessagesDeleted(m.getMessageIds());
			}
		} else if (e instanceof ContactConnectedEvent) {
			ContactConnectedEvent c = (ContactConnectedEvent) e;
			if (c.getContactId().equals(contactId)) {
				//LOG.info("Contact connected");
				displayContactOnlineStatus();
			}
		} else if (e instanceof ContactDisconnectedEvent) {
			ContactDisconnectedEvent c = (ContactDisconnectedEvent) e;
			if (c.getContactId().equals(contactId)) {
				//LOG.info("Contact disconnected");
				displayContactOnlineStatus();
			}
		} else if (e instanceof ClientVersionUpdatedEvent) {
			ClientVersionUpdatedEvent c = (ClientVersionUpdatedEvent) e;
			if (c.getContactId().equals(contactId)) {
				ClientId clientId = c.getClientVersion().getClientId();
				if (clientId.equals(MessagingManager.CLIENT_ID)) {
					//LOG.info("Contact's messaging client was updated");
					viewModel.recheckFeaturesAndOnboarding(contactId);
				}
			}
		}
	}

	@UiThread
	private void addConversationItem(ConversationItem item) {
		adapter.incrementRevision();
		adapter.add(item);
		// When adding a new message, scroll to the bottom if the conversation
		// is visible, even if we're not currently at the bottom
		if (getLifecycle().getCurrentState().isAtLeast(STARTED))
			scrollToBottom();
	}

	@UiThread
	private void onNewConversationMessage(ConversationMessageHeader h) {
		if (h instanceof ConversationRequest ||
				h instanceof ConversationResponse) {
			// contact name might not have been loaded
			observeOnce(viewModel.getContactDisplayName(), this,
					name -> addConversationItem(h.accept(visitor)));
		} else {
			// visitor also loads message text and attachments (if existing)
			addConversationItem(h.accept(visitor));
		}
	}

	@UiThread
	private void onConversationMessagesDeleted(
			Collection<MessageId> messageIds) {
		adapter.incrementRevision();
		adapter.removeItems(messageIds);
	}

	@UiThread
	private void markMessages(Collection<MessageId> messageIds, boolean sent,
			boolean seen) {
		adapter.incrementRevision();
		Set<MessageId> messages = new HashSet<>(messageIds);
		SparseArray<ConversationItem> list = adapter.getOutgoingMessages();
		for (int i = 0; i < list.size(); i++) {
			ConversationItem item = list.valueAt(i);
			if (messages.contains(item.getId())) {
				item.setSent(sent);
				item.setSeen(seen);
				adapter.notifyItemChanged(list.keyAt(i));
			}
		}
	}



	private void onAttachmentsChosen(@Nullable List<Uri> uris) {
		if (uris == null || uris.isEmpty()) return;
		// TODO: remove cast when removing feature flag
		((TextAttachmentController) sendController).onImageReceived(uris);
	}

	@Override
	public void onTooManyAttachments() {
		String format = getResources().getString(
				R.string.messaging_too_many_attachments_toast);
		String warning = String.format(format, MAX_ATTACHMENTS_PER_MESSAGE);
		Toast.makeText(this, warning, LENGTH_SHORT).show();
	}

	@Override
	public LiveData<SendState> onSendClick(@Nullable String text,
			List<AttachmentHeader> attachmentHeaders,
			long expectedAutoDeleteTimer) {
		if (isNullOrEmpty(text) && attachmentHeaders.isEmpty())
			throw new AssertionError();
		return viewModel
				.sendMessage(text, attachmentHeaders, expectedAutoDeleteTimer);
	}

	private void onAddedPrivateMessage(@Nullable PrivateMessageHeader h) {
		if (h == null) return;
		addConversationItem(h.accept(visitor));
	}

	private void askToDeleteAllMessages() {
		MaterialAlertDialogBuilder builder =
				new MaterialAlertDialogBuilder(this, R.style.AnonDialogTheme);
		builder.setTitle(getString(R.string.dialog_title_delete_all_messages));
		builder.setMessage(
				getString(R.string.dialog_message_delete_all_messages));
		builder.setNegativeButton(R.string.delete,
				(dialog, which) -> deleteAllMessages());
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	private void deleteAllMessages() {
		list.showProgressBar();
		runOnDbThread(() -> {
			try {
				DeletionResult result =
						conversationManager.deleteAllMessages(contactId);
				reloadConversationAfterDeletingMessages(result);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				runOnUiThreadUnlessDestroyed(() -> list.showData());
			}
		});
	}

	private void deleteSelectedMessages() {
		list.showProgressBar();
		Collection<MessageId> selected = getSelection();
		// close action mode only after getting the selection
		if (actionMode != null) actionMode.finish();
		runOnDbThread(() -> {
			try {
				DeletionResult result =
						conversationManager.deleteMessages(contactId, selected);
				reloadConversationAfterDeletingMessages(result);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				runOnUiThreadUnlessDestroyed(() -> list.showData());
			}
		});
	}

	private void reloadConversationAfterDeletingMessages(
			DeletionResult result) {
		runOnUiThreadUnlessDestroyed(() -> {
			adapter.clear();
			list.showProgressBar();  // otherwise clearing shows empty state
			loadMessages();
			if (!result.allDeleted()) showNotAllDeletedDialog(result);
		});
	}

	private void showNotAllDeletedDialog(DeletionResult result) {
		List<String> fails = new ArrayList<>();
		// get failures the user cannot immediately resolve
		if (result.hasIntroductionSessionInProgress() &&
				result.hasInvitationSessionInProgress()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_ongoing_both));
		} else if (result.hasIntroductionSessionInProgress()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_ongoing_introductions));
		} else if (result.hasInvitationSessionInProgress()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_ongoing_invitations));
		}
		// add problems the user can resolve
		if (result.hasNotAllIntroductionSelected() &&
				result.hasNotAllInvitationSelected()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_not_all_selected_both));
		} else if (result.hasNotAllIntroductionSelected()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_not_all_selected_introductions));
		} else if (result.hasNotAllInvitationSelected()) {
			fails.add(getString(
					R.string.dialog_message_not_deleted_not_all_selected_invitations));
		}
		String msg = join(fails, "\n\n");
		// show dialog
		MaterialAlertDialogBuilder builder =
				new MaterialAlertDialogBuilder(this, R.style.AnonDialogTheme);
		builder.setTitle(
				getString(R.string.dialog_title_not_all_messages_deleted));
		builder.setMessage(msg);
		builder.setPositiveButton(R.string.ok, null);
		builder.show();
	}

	private void askToRemoveContact() {
		DialogInterface.OnClickListener okListener =
				(dialog, which) -> removeContact();
		MaterialAlertDialogBuilder builder =
				new MaterialAlertDialogBuilder(ConversationActivity.this,
						R.style.AnonDialogTheme);
		builder.setTitle(getString(R.string.dialog_title_delete_contact));
		builder.setMessage(
				getString(R.string.dialog_message_delete_contact));
		builder.setNegativeButton(R.string.delete, okListener);
		builder.setPositiveButton(R.string.cancel, null);
		builder.show();
	}

	private void removeContact() {
		list.showProgressBar();
		runOnDbThread(() -> {
			try {
				contactManager.removeContact(contactId);
				getSharedPreferences("conversation_prefs", MODE_PRIVATE)
						.edit()
						.remove(PREF_WALKIE_TALKIE + contactId.getInt())
						.apply();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			} finally {
				finishAfterContactRemoved();
			}
		});
	}

	private void finishAfterContactRemoved() {
		runOnUiThreadUnlessDestroyed(() -> {
			String deleted = getString(R.string.contact_deleted_toast);
			Toast.makeText(ConversationActivity.this, deleted, LENGTH_SHORT)
					.show();
			supportFinishAfterTransition();
		});
	}

	private void showImageOnboarding(Boolean show) {
		if (!show) return;
		// show onboarding only after the enter transition has ended
		// otherwise the tap target animation won't play
		textInputView.postDelayed(this::showImageOnboarding,
				TRANSITION_DURATION_MS + ONBOARDING_DELAY_MS);
	}

	private void showImageOnboarding() {
		// TODO: remove cast when removing feature flag
		((TextAttachmentController) sendController)
				.showImageOnboarding(this);
	}

	private void showIntroductionOnboarding(@Nullable Boolean show) {
		if (show == null || !show) return;
		// show onboarding only after the enter transition has ended
		// otherwise the tap target animation won't play
		textInputView.postDelayed(this::showIntroductionOnboarding,
				TRANSITION_DURATION_MS + ONBOARDING_DELAY_MS);
	}

	private void showIntroductionOnboarding() {
		// find view of overflow icon
		View target = null;
		for (int i = 0; i < toolbar.getChildCount(); i++) {
			if (toolbar.getChildAt(i) instanceof ActionMenuView) {
				ActionMenuView menu =
						(ActionMenuView) toolbar.getChildAt(i);
				// The overflow icon should be the last child of the menu
				target = menu.getChildAt(menu.getChildCount() - 1);
				// If the menu hasn't been populated yet, use the menu itself
				// as the target
				if (target == null) target = menu;
				break;
			}
		}
		if (target == null) {
			LOG.warning("No Overflow Icon found!");
			return;
		}

		int color =
				ContextCompat.getColor(ConversationActivity.this, R.color.anontheme_accent);
		Drawable drawable = VectorDrawableCompat
				.create(getResources(), R.drawable.ic_more_vert_accent, null);
		new MaterialTapTargetPrompt.Builder(ConversationActivity.this,
				R.style.OnboardingDialogTheme).setTarget(target)
				.setPrimaryText(R.string.introduction_onboarding_title)
				.setSecondaryText(R.string.introduction_onboarding_text)
				.setIconDrawable(drawable)
				.setBackgroundColour(color)
				.show();
	}

	@UiThread
	@Override
	public void respondToRequest(ConversationRequestItem item,
			boolean accept) {
		item.setAnswered();
		int position = adapter.findItemPosition(item);
		if (position != INVALID_POSITION) {
			adapter.notifyItemChanged(position, item);
		}
		runOnDbThread(() -> {
			try {
				switch (item.getRequestType()) {
					case INTRODUCTION:
						respondToIntroductionRequest(item.getSessionId(),
								accept);
						break;
					case FORUM:
						respondToForumRequest(item.getSessionId(), accept);
						break;
					case BLOG:
						respondToBlogRequest(item.getSessionId(), accept);
						break;
					case GROUP:
						respondToGroupRequest(item.getSessionId(), accept);
						break;
					default:
						throw new IllegalArgumentException(
								"Unknown Request Type");
				}
				loadMessages();
			} catch (ProtocolStateException e) {
				// Action is no longer valid - reloading should solve the issue
				logException(LOG, INFO, e);
			} catch (DbException e) {
				// TODO show an error message
				logException(LOG, WARNING, e);
			}
		});
	}

	@UiThread
	@Override
	public void openRequestedShareable(ConversationRequestItem item) {
		if (item.getRequestedGroupId() == null)
			throw new IllegalArgumentException();
		Intent i;
		switch (item.getRequestType()) {
			case FORUM:
				i = new Intent(this, ForumActivity.class);
				break;
			case BLOG:
				i = new Intent(this, BlogActivity.class);
				break;
			case GROUP:
				i = new Intent(this, GroupActivity.class);
				break;
			default:
				throw new IllegalArgumentException("Unknown Request Type");
		}
		i.putExtra(GROUP_ID, item.getRequestedGroupId().getBytes());
		startActivity(i);
	}

	@Override
	public void onAttachmentClicked(View view,
			ConversationMessageItem messageItem, AttachmentItem item) {
		String name;
		if (messageItem.isIncoming()) {
			// must be available when items are being displayed
			name = viewModel.getContactDisplayName().getValue();
		} else {
			name = getString(R.string.you);
		}
		ArrayList<AttachmentItem> attachments =
				new ArrayList<>(messageItem.getAttachments());
		Intent i = new Intent(this, ImageActivity.class);
		i.putParcelableArrayListExtra(ATTACHMENTS, attachments);
		i.putExtra(ATTACHMENT_POSITION, attachments.indexOf(item));
		i.putExtra(NAME, name);
		i.putExtra(DATE, messageItem.getTime());
		i.putExtra(ITEM_ID, messageItem.getId().getBytes());
		// restoring list position should not trigger android bug #224270
		String transitionName = item.getTransitionName(messageItem.getId());
		ActivityOptionsCompat options =
				makeSceneTransitionAnimation(this, view, transitionName);
		ActivityCompat.startActivity(this, i, options.toBundle());
	}

	@Override
	public void onAutoDeleteTimerNoticeClicked() {
		ConversationSettingsDialog dialog =
				ConversationSettingsDialog.newInstance(contactId);
		dialog.show(getSupportFragmentManager(),
				ConversationSettingsDialog.TAG);
	}

	@DatabaseExecutor
	private void respondToIntroductionRequest(SessionId sessionId,
			boolean accept) throws DbException {
		introductionManager.respondToIntroduction(contactId, sessionId, accept);
	}

	@DatabaseExecutor
	private void respondToForumRequest(SessionId id, boolean accept)
			throws DbException {
		forumSharingManager.respondToInvitation(contactId, id, accept);
	}

	@DatabaseExecutor
	private void respondToBlogRequest(SessionId id, boolean accept)
			throws DbException {
		blogSharingManager.respondToInvitation(contactId, id, accept);
	}

	@DatabaseExecutor
	private void respondToGroupRequest(SessionId id, boolean accept)
			throws DbException {
		groupInvitationManager.respondToInvitation(contactId, id, accept);
	}

	@Nullable
	@Override
	public String getText(MessageId m) {
		String text = textCache.get(m);
		if (text == null) loadMessageText(m);
		return text;
	}

	/**
	 * Called by {@link PrivateMessageHeader#accept(ConversationMessageVisitor)}
	 */
	@Override
	public List<AttachmentItem> getAttachmentItems(PrivateMessageHeader h) {
		List<LiveData<AttachmentItem>> liveDataList =
				attachmentRetriever.getAttachmentItems(h);
		List<AttachmentItem> items = new ArrayList<>(liveDataList.size());
		for (LiveData<AttachmentItem> liveData : liveDataList) {
			// first remove all our observers to avoid having more than one
			// in case we reload the conversation, e.g. after deleting messages
			liveData.removeObservers(this);
			// add a new observer
			liveData.observe(this, new AttachmentObserver(h.getId(), liveData));
			items.add(requireNonNull(liveData.getValue()));
		}
		return items;
	}

	private class AttachmentObserver implements Observer<AttachmentItem> {
		private final MessageId conversationMessageId;
		private final LiveData<AttachmentItem> liveData;

		private AttachmentObserver(MessageId conversationMessageId,
				LiveData<AttachmentItem> liveData) {
			this.conversationMessageId = conversationMessageId;
			this.liveData = liveData;
		}

		@Override
		public void onChanged(AttachmentItem attachmentItem) {
			updateMessageAttachment(conversationMessageId, attachmentItem);
			if (attachmentItem.getState().isFinal())
				liveData.removeObserver(this);
		}
	}

	@Override
	public void onAttachImageClicked() {
		String[] mimeTypes = new String[] {
				"image/*",
				"audio/ogg",
				"audio/m4a",
				"audio/mp4",
				"audio/x-m4a",
				"audio/x-wav",
				"audio/webm"
		};
		launchActivityToOpenFile(this, docLauncher, contentLauncher, mimeTypes);
	}



	@Override
	public void onMapMessageClicked(MapMessageData data) {
		Intent intent = new Intent(this, MapViewActivity.class);
		intent.putExtra(MapViewActivity.EXTRA_LABEL, data.label);
		intent.putExtra(MapViewActivity.EXTRA_LATITUDE, data.latitude);
		intent.putExtra(MapViewActivity.EXTRA_LONGITUDE, data.longitude);
		intent.putExtra(MapViewActivity.EXTRA_ZOOM, data.zoom);
		startActivity(intent);
	}

	// ---- Walkie-Talkie ----

	private boolean isWalkieTalkieEnabled() {
		SharedPreferences prefs =
				getSharedPreferences("conversation_prefs", MODE_PRIVATE);
		return prefs.getBoolean(
				PREF_WALKIE_TALKIE + contactId.getInt(), false);
	}

	private void toggleWalkieTalkie() {
		SharedPreferences prefs =
				getSharedPreferences("conversation_prefs", MODE_PRIVATE);
		String key = PREF_WALKIE_TALKIE + contactId.getInt();
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
		walkieTalkieBar.setBackgroundColor(getThemeColor(R.attr.colorPrimary));
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

	private int getThemeColor(int attr) {
		android.util.TypedValue tv = new android.util.TypedValue();
		getTheme().resolveAttribute(attr, tv, true);
		return tv.data;
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

	private void loadAndPlayAttachment(AttachmentHeader ah,
			String senderName) {
		runOnDbThread(() -> {
			try {
				Attachment a =
						attachmentRetriever.getMessageAttachment(ah);
				InputStream stream = a.getStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int len;
				while ((len = stream.read(buf)) != -1) {
					baos.write(buf, 0, len);
				}
				stream.close();
				byte[] audioData = baos.toByteArray();
				runOnUiThread(() -> {
					if (walkieTalkieEnabled) {
						walkieTalkiePlayer.play(audioData, senderName);
					}
				});
			} catch (Exception e) {
				Log.e("ConversationActivity",
						"Failed to load attachment for auto-play", e);
			}
		});
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
