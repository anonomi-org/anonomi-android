package org.anonomi.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.conversation.MapMessageData;
import org.anonomi.android.fragment.BaseFragment;
import org.anonomi.android.map.MapViewActivity;
import org.anonomi.android.widget.LinkDialogFragment;
import org.anonomi.android.viewmodel.LiveResult;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.UiThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Logger.getLogger;
import static org.anonomi.android.activity.BriarActivity.GROUP_ID;
import static org.anonomi.android.util.UiUtils.MIN_DATE_RESOLUTION;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BlogPostFragment extends BaseFragment
		implements OnBlogPostClickListener {

	private static final String TAG = BlogPostFragment.class.getName();
	private static final Logger LOG = getLogger(TAG);

	static final String POST_ID = "briar.POST_ID";

	protected BlogViewModel viewModel;
	private final Handler handler = new Handler(Looper.getMainLooper());

	private ProgressBar progressBar;
	private BlogPostViewHolder ui;
	private BlogPostItem post;
	private Runnable refresher;

	private final MutableLiveData<LiveResult<BlogPostItem>> individualPost =
			new MutableLiveData<>();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	static BlogPostFragment newInstance(GroupId blogId, MessageId postId) {
		BlogPostFragment f = new BlogPostFragment();
		Bundle bundle = new Bundle();
		bundle.putByteArray(GROUP_ID, blogId.getBytes());
		bundle.putByteArray(POST_ID, postId.getBytes());
		f.setArguments(bundle);
		return f;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(requireActivity(), viewModelFactory)
				.get(BlogViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		Bundle args = requireArguments();
		GroupId groupId =
				new GroupId(requireNonNull(args.getByteArray(GROUP_ID)));
		MessageId postId =
				new MessageId(requireNonNull(args.getByteArray(POST_ID)));

		View view = inflater.inflate(R.layout.fragment_blog_post, container,
				false);
		progressBar = view.findViewById(R.id.progressBar);
		progressBar.setVisibility(VISIBLE);
		ui = new BlogPostViewHolder(view, true, this, false);
		LifecycleOwner owner = getViewLifecycleOwner();

		// Combine initial load and updates into one LiveData
		individualPost.observe(owner, result ->
				result.onError(this::handleException)
						.onSuccess(this::onBlogPostLoaded)
		);

		// Observe the main posts list for updates to this specific post
		viewModel.getBlogPosts().observe(owner, result ->
				result.onSuccess(this::onBlogPostsLoaded)
		);

		viewModel.loadBlogPost(groupId, postId).observe(owner, individualPost::setValue);

		return view;
	}

	private void onBlogPostsLoaded(BaseViewModel.ListUpdate update) {
		// Try to find the latest version of the post in the main list
		byte[] postIdBytes = requireArguments().getByteArray(POST_ID);
		if (postIdBytes == null) return;
		MessageId currentId = new MessageId(postIdBytes);
		
		for (BlogPostItem item : update.getItems()) {
			if (item.getId().equals(currentId)) {
				individualPost.setValue(new LiveResult<>(item));
				return;
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		stopPeriodicUpdate();
	}

	@UiThread
	private void onBlogPostLoaded(BlogPostItem post) {
		progressBar.setVisibility(INVISIBLE);
		this.post = post;
		ui.bindItem(post);
	}

	@Override
	public void onBlogPostClick(BlogPostItem post) {
		// We're already there
	}

	@Override
	public void onAuthorClick(BlogPostItem post) {
		Intent i = new Intent(requireContext(), BlogActivity.class);
		i.putExtra(GROUP_ID, post.getGroupId().getBytes());
		i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
		requireContext().startActivity(i);
	}

	@Override
	public void onLinkClick(String url) {
		LinkDialogFragment f = LinkDialogFragment.newInstance(url);
		f.show(getParentFragmentManager(), f.getUniqueTag());
	}

	@Override
	public void onMapMessageClicked(MapMessageData data) {
		Intent i = new Intent(requireContext(), MapViewActivity.class);
		i.putExtra(MapViewActivity.EXTRA_LABEL, data.label);
		i.putExtra(MapViewActivity.EXTRA_LATITUDE, data.latitude);
		i.putExtra(MapViewActivity.EXTRA_LONGITUDE, data.longitude);
		i.putExtra(MapViewActivity.EXTRA_ZOOM, data.zoom);
		startActivity(i);
	}

	@Override
	public void onLikeClick(BlogPostItem post) {
		if (post.isLikedByMe()) viewModel.unlikePost(post);
		else viewModel.likePost(post);
	}

	@Override
	public void onInteractionsClick(BlogPostItem post) {
		Intent i = new Intent(requireContext(),
				BlogInteractionsActivity.class);
		i.putExtra(GROUP_ID, post.getGroupId().getBytes());
		i.putExtra(POST_ID, post.getId().getBytes());
		startActivity(i);
	}

	@Override
	public void onCommentClick(BlogPostItem post) {
		if (getContext() == null) return;
		android.widget.EditText input = new android.widget.EditText(getContext());
		input.setHint(R.string.comment_blog_post_hint);
		input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
				| android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
				| android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		int pad = getResources().getDimensionPixelSize(
				R.dimen.listitem_vertical_margin);
		android.widget.FrameLayout container =
				new android.widget.FrameLayout(getContext());
		container.setPadding(pad, pad / 2, pad, 0);
		container.addView(input);
		new MaterialAlertDialogBuilder(getContext(),
				R.style.AnonDialogTheme)
				.setTitle(R.string.comment_blog_post)
				.setView(container)
				.setPositiveButton(android.R.string.ok, (d, w) -> {
					String comment = input.getText().toString().trim();
					if (!comment.isEmpty()) {
						viewModel.commentOnPost(post, comment);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void startPeriodicUpdate() {
		refresher = () -> {
			if (post != null) ui.updateDate(post.getTimestamp());
			handler.postDelayed(refresher, MIN_DATE_RESOLUTION);
		};
		handler.postDelayed(refresher, MIN_DATE_RESOLUTION);
	}

	private void stopPeriodicUpdate() {
		if (refresher != null) {
			handler.removeCallbacks(refresher);
		}
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}
