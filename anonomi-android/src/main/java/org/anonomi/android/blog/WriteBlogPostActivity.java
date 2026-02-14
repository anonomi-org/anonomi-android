package org.anonomi.android.blog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.attachment.media.ImageCompressor;
import org.anonomi.android.map.MapLocationPickerActivity;
import org.anonomi.android.view.TextInputView;
import org.anonomi.android.view.TextSendController;
import org.anonomi.android.view.TextSendController.SendListener;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogPost;
import org.anonchatsecure.anonchat.api.blog.BlogPostFactory;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.StringUtils.isNullOrEmpty;
import static org.anonomi.android.view.TextSendController.SendState;
import static org.anonomi.android.view.TextSendController.SendState.SENT;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.MAX_BLOG_IMAGE_SIZE;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.MAX_BLOG_POST_TEXT_LENGTH;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class WriteBlogPostActivity extends BriarActivity
		implements SendListener {

	private static final Logger LOG =
			Logger.getLogger(WriteBlogPostActivity.class.getName());

	private static final int REQUEST_SEND_LOCATION = 2001;

	@Inject
	AndroidNotificationManager notificationManager;
	@Inject
	ImageCompressor imageCompressor;

	private TextInputView input;
	private TextSendController sendController;
	private NestedScrollView scrollView;
	private ProgressBar progressBar;

	private View pendingImageContainer;
	private ImageView pendingImagePreview;
	private View pendingLocationContainer;
	private TextView pendingLocationText;

	@Nullable
	private byte[] pendingImageBytes;
	@Nullable
	private String pendingImageContentType;
	@Nullable
	private String pendingLocationMessage;

	// Fields that are accessed from background threads must be volatile
	private volatile GroupId groupId;
	@Inject
	volatile IdentityManager identityManager;
	@Inject
	volatile BlogPostFactory blogPostFactory;
	@Inject
	volatile BlogManager blogManager;

	private final ActivityResultLauncher<String> imagePickerLauncher =
			registerForActivityResult(
					new ActivityResultContracts.GetContent(),
					this::onImageSelected);

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No Group in intent.");
		groupId = new GroupId(b);

		setContentView(R.layout.activity_write_blog_post);

		input = findViewById(R.id.textInput);
		sendController = new TextSendController(input, this, false);
		input.setSendController(sendController);
		input.setMaxTextLength(MAX_BLOG_POST_TEXT_LENGTH);
		input.setReady(true);

		// Move publish button to end so attachment previews appear
		// between the text input and the button.
		// LargeTextInputView is a LinearLayout; merge content adds
		// CardView(0), Button(1), then XML children follow.
		View publishButton = input.findViewById(R.id.compositeSendButton);
		((ViewGroup) input).removeView(publishButton);
		((ViewGroup) input).addView(publishButton);

		scrollView = findViewById(R.id.scrollView);
		progressBar = findViewById(R.id.progressBar);

		pendingImageContainer = findViewById(R.id.pendingImageContainer);
		pendingImagePreview = findViewById(R.id.pendingImagePreview);
		pendingLocationContainer = findViewById(R.id.pendingLocationContainer);
		pendingLocationText = findViewById(R.id.pendingLocationText);

		findViewById(R.id.removeImageButton)
				.setOnClickListener(v -> clearPendingImage());
		findViewById(R.id.removeLocationButton)
				.setOnClickListener(v -> clearPendingLocation());
	}

	@Override
	public void onStart() {
		super.onStart();
		notificationManager.blockNotification(groupId);
	}

	@Override
	public void onStop() {
		super.onStop();
		notificationManager.unblockNotification(groupId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.blog_write_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			onBackPressed();
			return true;
		} else if (itemId == R.id.action_attach_image) {
			imagePickerLauncher.launch("image/*");
			return true;
		} else if (itemId == R.id.action_send_location) {
			Intent intent = new Intent(this, MapLocationPickerActivity.class);
			startActivityForResult(intent, REQUEST_SEND_LOCATION);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int request, int result,
			@Nullable Intent data) {
		if (request == REQUEST_SEND_LOCATION &&
				result == RESULT_OK && data != null) {
			String message = data.getStringExtra(
					MapLocationPickerActivity.RESULT_MAP_MESSAGE);
			if (message != null) {
				pendingLocationMessage = message;
				String label = parseLocationLabel(message);
				pendingLocationText.setText(label);
				pendingLocationContainer.setVisibility(VISIBLE);
				updatePendingState();
			}
		} else {
			super.onActivityResult(request, result, data);
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public LiveData<SendState> onSendClick(@Nullable String text,
			List<AttachmentHeader> headers, long expectedAutoDeleteTimer) {
		// Build final text: user text + location message
		StringBuilder finalText = new StringBuilder();
		if (!isNullOrEmpty(text)) {
			finalText.append(text);
		}
		if (pendingLocationMessage != null) {
			if (finalText.length() > 0) finalText.append("\n");
			finalText.append(pendingLocationMessage);
		}

		// hide editor, show progress bar
		input.hideSoftKeyboard();
		scrollView.setVisibility(GONE);
		progressBar.setVisibility(VISIBLE);

		if (pendingImageBytes != null) {
			storeImagePost(pendingImageBytes, pendingImageContentType,
					finalText.toString());
		} else if (finalText.length() > 0) {
			storePost(finalText.toString());
		}
		return new MutableLiveData<>(SENT);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		if (pendingImageBytes != null || pendingLocationMessage != null) {
			clearPendingImage();
			clearPendingLocation();
			return;
		}
		super.onBackPressed();
	}

	private void onImageSelected(@Nullable Uri uri) {
		if (uri == null) return;

		new Thread(() -> {
			try {
				String mimeType = getContentResolver().getType(uri);
				if (mimeType == null) mimeType = "image/jpeg";
				InputStream is = getContentResolver().openInputStream(uri);
				if (is == null) {
					runOnUiThread(() -> {
						Toast.makeText(this,
								getString(R.string.image_compression_failed),
								Toast.LENGTH_SHORT).show();
						clearPendingImage();
					});
					return;
				}

				// Load bitmap for preview
				Bitmap preview = BitmapFactory.decodeStream(is);
				is.close();

				// Compress for sending
				InputStream is2 =
						getContentResolver().openInputStream(uri);
				if (is2 == null) {
					runOnUiThread(() -> {
						Toast.makeText(this,
								getString(R.string.image_compression_failed),
								Toast.LENGTH_SHORT).show();
						clearPendingImage();
					});
					return;
				}
				InputStream compressed = imageCompressor.compressImage(
						is2, mimeType, MAX_BLOG_IMAGE_SIZE);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int len;
				while ((len = compressed.read(buf)) != -1)
					bos.write(buf, 0, len);
				byte[] imageBytes = bos.toByteArray();

				runOnUiThread(() -> {
					pendingImageBytes = imageBytes;
					pendingImageContentType = "image/jpeg";
					if (preview != null) {
						pendingImagePreview.setImageBitmap(preview);
					}
					pendingImageContainer.setVisibility(VISIBLE);
					updatePendingState();
				});
			} catch (IOException e) {
				logException(LOG, WARNING, e);
				runOnUiThread(() -> {
					Toast.makeText(this,
							getString(R.string.image_compression_failed),
							Toast.LENGTH_SHORT).show();
					clearPendingImage();
				});
			}
		}).start();
	}

	private void clearPendingImage() {
		pendingImageBytes = null;
		pendingImageContentType = null;
		pendingImagePreview.setImageDrawable(null);
		pendingImageContainer.setVisibility(GONE);
		updatePendingState();
	}

	private void clearPendingLocation() {
		pendingLocationMessage = null;
		pendingLocationText.setText("");
		pendingLocationContainer.setVisibility(GONE);
		updatePendingState();
	}

	private void updatePendingState() {
		boolean hasAttachment = pendingImageBytes != null ||
				pendingLocationMessage != null;
		sendController.setHasPendingAttachment(hasAttachment);
		if (hasAttachment) {
			scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
		}
	}

	private String parseLocationLabel(String message) {
		// Format: "::map:label;:lat,lon;:zoom"
		String payload = message.substring(6); // Remove "::map:"
		int end = payload.indexOf(";:");
		if (end > 0) {
			return payload.substring(0, end);
		}
		return payload;
	}

	private void storePost(String text) {
		runOnDbThread(() -> {
			long timestamp = System.currentTimeMillis();
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				BlogPost p = blogPostFactory
						.createBlogPost(groupId, timestamp, null, author, text);
				blogManager.addLocalPost(p);
				postPublished();
			} catch (DbException | GeneralSecurityException
					| FormatException e) {
				logException(LOG, WARNING, e);
				postFailedToPublish();
			}
		});
	}

	private void storeImagePost(byte[] imageData,
			@Nullable String contentType, @Nullable String text) {
		runOnDbThread(() -> {
			long timestamp = System.currentTimeMillis();
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				BlogPost p = blogPostFactory.createBlogImagePost(
						groupId, timestamp, null, author,
						text != null ? text : "", imageData,
						contentType != null ? contentType : "image/jpeg");
				blogManager.addLocalImagePost(p);
				postPublished();
			} catch (DbException | GeneralSecurityException
					| FormatException e) {
				logException(LOG, WARNING, e);
				postFailedToPublish();
			}
		});
	}

	private void postPublished() {
		runOnUiThreadUnlessDestroyed(() -> {
			setResult(RESULT_OK);
			supportFinishAfterTransition();
		});
	}

	private void postFailedToPublish() {
		runOnUiThreadUnlessDestroyed(() -> {
			// hide progress bar, show editor
			progressBar.setVisibility(GONE);
			scrollView.setVisibility(VISIBLE);
		});
	}
}
