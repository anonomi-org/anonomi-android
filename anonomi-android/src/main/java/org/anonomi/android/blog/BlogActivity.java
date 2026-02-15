package org.anonomi.android.blog;

import android.content.Intent;
import android.os.Bundle;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.fragment.BaseFragment;
import org.anonomi.android.fragment.BaseFragment.BaseFragmentListener;
import org.anonomi.android.sharing.BlogSharingStatusActivity;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import static java.util.Objects.requireNonNull;
import static org.anonomi.android.blog.BlogPostFragment.POST_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BlogActivity extends BriarActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private BlogViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(BlogViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		// GroupId from Intent
		Intent i = getIntent();
		GroupId groupId =
				new GroupId(requireNonNull(i.getByteArrayExtra(GROUP_ID)));
		// Get post info from intent
		@Nullable byte[] postId = i.getByteArrayExtra(POST_ID);

		// Always load all posts to ensure real-time updates via blogPosts LiveData
		viewModel.setGroupId(groupId, true);

		setContentView(R.layout.activity_fragment_container_toolbar);
		Toolbar toolbar = setUpCustomToolbar(false);

		// Open Sharing Status on Toolbar click
		toolbar.setOnClickListener(v -> {
			Intent i1 = new Intent(BlogActivity.this,
					BlogSharingStatusActivity.class);
			i1.putExtra(GROUP_ID, groupId.getBytes());
			startActivity(i1);
		});

		viewModel.getBlog().observe(this, blog ->
				setTitle(blog.getBlog().getAuthor().getName())
		);
		viewModel.getSharingInfo().observe(this, info ->
				setToolbarSubTitle(info.total, info.online)
		);

		if (state == null) {
			if (postId == null) {
				showInitialFragment(BlogFragment.newInstance(groupId));
			} else {
				MessageId messageId = new MessageId(postId);
				BaseFragment f =
						BlogPostFragment.newInstance(groupId, messageId);
				showInitialFragment(f);
			}
		}
	}

	private void setToolbarSubTitle(int total, int online) {
		requireNonNull(getSupportActionBar())
				.setSubtitle(getString(R.string.shared_with, total, online));
	}

}
