package org.anonomi.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ProgressBar;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
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

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.StringUtils.isNullOrEmpty;
import static org.anonomi.android.view.TextSendController.SendState;
import static org.anonomi.android.view.TextSendController.SendState.SENT;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.MAX_BLOG_POST_TEXT_LENGTH;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class WriteBlogPostActivity extends BriarActivity
		implements SendListener {

	private static final Logger LOG =
			Logger.getLogger(WriteBlogPostActivity.class.getName());

	@Inject
	AndroidNotificationManager notificationManager;

	private TextInputView input;
	private ProgressBar progressBar;

	// Fields that are accessed from background threads must be volatile
	private volatile GroupId groupId;
	@Inject
	volatile IdentityManager identityManager;
	@Inject
	volatile BlogPostFactory blogPostFactory;
	@Inject
	volatile BlogManager blogManager;

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		Intent i = getIntent();
		byte[] b = i.getByteArrayExtra(GROUP_ID);
		if (b == null) throw new IllegalStateException("No Group in intent.");
		groupId = new GroupId(b);

		setContentView(R.layout.activity_write_blog_post);

		input = findViewById(R.id.textInput);
		TextSendController sendController =
				new TextSendController(input, this, false);
		input.setSendController(sendController);
		input.setMaxTextLength(MAX_BLOG_POST_TEXT_LENGTH);
		input.setReady(true);

		progressBar = findViewById(R.id.progressBar);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public LiveData<SendState> onSendClick(@Nullable String text,
			List<AttachmentHeader> headers, long expectedAutoDeleteTimer) {
		if (isNullOrEmpty(text)) throw new AssertionError();

		// hide publish button, show progress bar
		input.hideSoftKeyboard();
		input.setVisibility(GONE);
		progressBar.setVisibility(VISIBLE);

		storePost(text);
		return new MutableLiveData<>(SENT);
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

	private void postPublished() {
		runOnUiThreadUnlessDestroyed(() -> {
			setResult(RESULT_OK);
			supportFinishAfterTransition();
		});
	}

	private void postFailedToPublish() {
		runOnUiThreadUnlessDestroyed(() -> {
			// hide progress bar, show publish button
			progressBar.setVisibility(GONE);
			input.setVisibility(VISIBLE);
			// TODO show error
		});
	}
}
