package org.anonomi.android.forum;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.anonomi.R;
import org.anonomi.android.threaded.BaseThreadItemViewHolder;
import org.anonomi.android.threaded.ThreadItemAdapter;
import org.anonomi.android.threaded.ThreadPostViewHolder;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;

@UiThread
@NotNullByDefault
class ForumPostAdapter extends ThreadItemAdapter<ForumPostItem> {

	ForumPostAdapter(ThreadItemListener<ForumPostItem> listener) {
		super(listener);
	}

	@LayoutRes
	@Override
	public int getItemViewType(int position) {
		ForumPostItem item = getItem(position);
		return item.getLayout();
	}

	@Override
	public BaseThreadItemViewHolder<ForumPostItem> onCreateViewHolder(
			ViewGroup parent, int type) {
		View v = LayoutInflater.from(parent.getContext())
				.inflate(type, parent, false);
		if (type == R.layout.list_item_thread_audio) {
			return new ForumAudioPostViewHolder(v);
		}
		return new ThreadPostViewHolder<>(v);
	}

}
