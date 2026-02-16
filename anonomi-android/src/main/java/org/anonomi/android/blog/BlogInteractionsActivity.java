package org.anonomi.android.blog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonchatsecure.bramble.api.connection.ConnectionRegistry;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.view.AuthorView;
import org.anonomi.android.view.BriarRecyclerView;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.android.blog.BlogPostFragment.POST_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class BlogInteractionsActivity extends BriarActivity {

	private static final Logger LOG =
			getLogger(BlogInteractionsActivity.class.getName());

	@Inject
	volatile BlogManager blogManager;
	@Inject
	volatile IdentityManager identityManager;
	@Inject
	volatile ConnectionRegistry connectionRegistry;
	@Inject
	volatile ContactManager contactManager;

	private BriarRecyclerView list;
	private InteractionsAdapter adapter;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);

		setContentView(R.layout.activity_sharing_status);

		Intent i = getIntent();
		byte[] groupBytes = i.getByteArrayExtra(GROUP_ID);
		byte[] postBytes = i.getByteArrayExtra(POST_ID);
		if (groupBytes == null || postBytes == null) {
			throw new IllegalStateException("Missing intent extras");
		}
		GroupId groupId = new GroupId(groupBytes);
		MessageId postId = new MessageId(postBytes);

		list = findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(this));
		adapter = new InteractionsAdapter();
		list.setAdapter(adapter);
		list.setEmptyText(getString(R.string.nobody));

		TextView info = findViewById(R.id.info);
		info.setText(R.string.blog_interactions_info);

		loadInteractions(groupId, postId);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void loadInteractions(GroupId groupId, MessageId postId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor localAuthor = identityManager.getLocalAuthor();
				AuthorId localAuthorId = localAuthor.getId();

				// Build the postKey for the target post
				String targetPostKey = null;
				Collection<Blog> blogs = blogManager.getBlogs();
				List<BlogPostHeader> allHeaders = new ArrayList<>();
				for (Blog blog : blogs) {
					allHeaders.addAll(
							blogManager.getPostHeaders(blog.getId()));
				}
				for (BlogPostHeader h : allHeaders) {
					if (h.getId().equals(postId)) {
						targetPostKey = BaseViewModel.postKey(h);
						break;
					}
				}

				if (targetPostKey == null) {
					runOnUiThreadUnlessDestroyed(() -> list.showData());
					return;
				}

				// Run the aggregation logic to get likers and commenters
				List<BlogPostItem> items =
						BaseViewModel.buildAndAggregatePosts(
								allHeaders, blogManager, localAuthorId);

				// Find the target post by postKey
				BlogPostItem targetPost = null;
				for (BlogPostItem item : items) {
					if (BaseViewModel.postKey(item.getHeader())
							.equals(targetPostKey)) {
						targetPost = item;
						break;
					}
				}

				if (targetPost == null) {
					runOnUiThreadUnlessDestroyed(() -> list.showData());
					return;
				}

				// Build the display list
				List<InteractionRow> rows = new ArrayList<>();

				// Liked by section
				List<BaseViewModel.BlogLiker> likers =
						targetPost.getLikers();
				if (!likers.isEmpty()) {
					rows.add(new InteractionRow(
							getString(R.string.blog_liked_by)));
					for (BaseViewModel.BlogLiker liker : likers) {
						rows.add(resolveRow(liker, localAuthorId));
					}
				}

				// Commented by section
				List<BaseViewModel.BlogLiker> commenters =
						targetPost.getCommenterAuthors();
				if (!commenters.isEmpty()) {
					rows.add(new InteractionRow(
							getString(R.string.blog_commented_by)));
					for (BaseViewModel.BlogLiker commenter : commenters) {
						rows.add(resolveRow(commenter, localAuthorId));
					}
				}

				List<InteractionRow> finalRows = rows;
				runOnUiThreadUnlessDestroyed(() -> {
					adapter.setItems(finalRows);
					list.showData();
				});
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private InteractionRow resolveRow(BaseViewModel.BlogLiker liker,
			AuthorId localAuthorId) {
		boolean connected = false;
		try {
			Contact contact = contactManager.getContact(
					liker.author.getId(), localAuthorId);
			connected = connectionRegistry.isConnected(contact.getId());
		} catch (DbException e) {
			// Author might be ourselves or not a direct contact
		}
		return new InteractionRow(liker.author, liker.authorInfo,
				connected);
	}

	private static class InteractionRow {
		static final int TYPE_HEADER = 0;
		static final int TYPE_AUTHOR = 1;

		final int type;
		// Header fields
		@Nullable
		final String headerText;
		// Author fields
		@Nullable
		final Author author;
		@Nullable
		final AuthorInfo authorInfo;
		final boolean connected;

		// Header constructor
		InteractionRow(String headerText) {
			this.type = TYPE_HEADER;
			this.headerText = headerText;
			this.author = null;
			this.authorInfo = null;
			this.connected = false;
		}

		// Author constructor
		InteractionRow(Author author, AuthorInfo authorInfo,
				boolean connected) {
			this.type = TYPE_AUTHOR;
			this.headerText = null;
			this.author = author;
			this.authorInfo = authorInfo;
			this.connected = connected;
		}
	}

	private class InteractionsAdapter
			extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		private List<InteractionRow> items = new ArrayList<>();

		void setItems(List<InteractionRow> items) {
			this.items = items;
			notifyDataSetChanged();
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@Override
		public int getItemViewType(int position) {
			return items.get(position).type;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(
				@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			if (viewType == InteractionRow.TYPE_HEADER) {
				TextView tv = new TextView(parent.getContext());
				int pad = getResources().getDimensionPixelSize(
						R.dimen.margin_medium);
				tv.setPadding(pad, pad, pad, pad / 2);
				tv.setTextAppearance(
						com.google.android.material.R.style
								.TextAppearance_MaterialComponents_Subtitle1);
				return new HeaderViewHolder(tv);
			} else {
				View v = inflater.inflate(R.layout.list_item_group_member,
						parent, false);
				return new AuthorViewHolder(v);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
				int position) {
			InteractionRow row = items.get(position);
			if (holder instanceof HeaderViewHolder) {
				((HeaderViewHolder) holder).bind(row);
			} else if (holder instanceof AuthorViewHolder) {
				((AuthorViewHolder) holder).bind(row);
			}
		}
	}

	private static class HeaderViewHolder extends RecyclerView.ViewHolder {
		private final TextView textView;

		HeaderViewHolder(TextView tv) {
			super(tv);
			this.textView = tv;
		}

		void bind(InteractionRow row) {
			textView.setText(row.headerText);
		}
	}

	private static class AuthorViewHolder extends RecyclerView.ViewHolder {
		private final AuthorView authorView;
		private final ImageView bulb;
		private final TextView creator;

		AuthorViewHolder(View v) {
			super(v);
			authorView = v.findViewById(R.id.authorView);
			bulb = v.findViewById(R.id.bulbView);
			creator = v.findViewById(R.id.creatorView);
			creator.setVisibility(View.GONE);
		}

		void bind(InteractionRow row) {
			if (row.author != null && row.authorInfo != null) {
				authorView.setAuthor(row.author, row.authorInfo);
			}
			if (row.connected) {
				bulb.setImageResource(R.drawable.contact_connected);
			} else {
				bulb.setImageResource(R.drawable.contact_disconnected);
			}
			bulb.setVisibility(View.VISIBLE);
		}
	}
}
