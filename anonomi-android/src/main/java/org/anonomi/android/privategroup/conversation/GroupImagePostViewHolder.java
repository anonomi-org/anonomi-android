package org.anonomi.android.privategroup.conversation;

import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import org.anonomi.R;
import org.anonomi.android.threaded.ThreadItemAdapter.ThreadItemListener;
import org.anonomi.android.threaded.ThreadPostViewHolder;
import org.anonomi.android.view.ImageViewActivity;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.UiThread;

@UiThread
@NotNullByDefault
class GroupImagePostViewHolder extends ThreadPostViewHolder<GroupMessageItem> {

	private final ImageView imageContent;

	GroupImagePostViewHolder(View v) {
		super(v);
		imageContent = v.findViewById(R.id.imageContent);
	}

	@Override
	public void bind(GroupMessageItem item,
			ThreadItemListener<GroupMessageItem> listener) {
		super.bind(item, listener);

		if (!item.hasImage()) return;

		byte[] data = item.getImageData();
		if (data == null) return;

		android.graphics.Bitmap bitmap =
				BitmapFactory.decodeByteArray(data, 0, data.length);
		if (bitmap != null) {
			imageContent.setImageBitmap(bitmap);
			imageContent.setOnClickListener(v ->
					ImageViewActivity.start(v.getContext(), data));
		}
	}

}
