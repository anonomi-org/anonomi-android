package org.anonomi.android.conversation;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;

import org.anonomi.R;
import org.anonomi.android.attachment.AttachmentItem;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.annotation.UiThread;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool;

import static androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT;
import static androidx.core.content.ContextCompat.getColor;
import static androidx.core.widget.ImageViewCompat.setImageTintList;

import java.io.File;
import java.io.IOException;
import org.anonchatsecure.anonchat.api.attachment.Attachment;
import org.anonomi.android.attachment.AttachmentRetriever;
import org.anonchatsecure.bramble.api.db.DbException;

import java.io.InputStream;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

@UiThread
@NotNullByDefault
class ConversationMessageViewHolder extends ConversationItemViewHolder {

	private final AttachmentRetriever attachmentRetriever;
	private final ImageAdapter adapter;
	private final ViewGroup statusLayout;
	private final int timeColor, timeColorBubble;
	private final ConstraintSet textConstraints = new ConstraintSet();
	private final ConstraintSet imageConstraints = new ConstraintSet();
	private final ConstraintSet imageTextConstraints = new ConstraintSet();

	ConversationMessageViewHolder(View v, ConversationListener listener,
			boolean isIncoming, RecycledViewPool imageViewPool,
			ImageItemDecoration imageItemDecoration,
			AttachmentRetriever attachmentRetriever) {
		super(v, listener, isIncoming);
		this.attachmentRetriever = attachmentRetriever;
		statusLayout = v.findViewById(R.id.statusLayout);

		// image list (only if present in this layout)
		RecyclerView list = v.findViewById(R.id.imageList);
		ImageAdapter tempAdapter = null;
		if (list != null) {
			tempAdapter = new ImageAdapter(v.getContext(), listener);
			list.setRecycledViewPool(imageViewPool);
			list.setAdapter(tempAdapter);
			list.addItemDecoration(imageItemDecoration);
		}
		this.adapter = tempAdapter;  // set once

		// remember original status text color
		timeColor = time.getCurrentTextColor();
		timeColorBubble =
				getColor(v.getContext(), R.color.msg_status_bubble_foreground);

		// clone constraint sets from layout files
		textConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_in_content);
		imageConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_image);
		imageTextConstraints.clone(v.getContext(),
				R.layout.list_item_conversation_msg_image_text);

		// in/out are different layouts, so we need to do this only once
		textConstraints.setHorizontalBias(R.id.statusLayout, isIncoming ? 1 : 0);
		imageConstraints.setHorizontalBias(R.id.statusLayout, isIncoming ? 1 : 0);
		imageTextConstraints.setHorizontalBias(R.id.statusLayout, isIncoming ? 1 : 0);
	}

	@Override
	void bind(ConversationItem conversationItem, boolean selected) {
		super.bind(conversationItem, selected);
		ConversationMessageItem item = (ConversationMessageItem) conversationItem;

		if (item.getAttachments().isEmpty()) {
			bindTextItem();
		} else {
			AttachmentItem firstAttachment = item.getAttachments().get(0);
			String contentType = firstAttachment.getHeader().getContentType();
			if (contentType.startsWith("audio/")) {
				bindVoiceItem(item, firstAttachment);
			} else {
				bindImageItem(item);
			}
		}
	}

	private void bindVoiceItem(ConversationMessageItem item, AttachmentItem attachment) {
		resetStatusLayoutForText();

		View voiceBubble = layout.findViewById(R.id.voiceBubble);
		if (voiceBubble == null) return;  // safety

		ImageButton playPause = layout.findViewById(R.id.btnPlayPause);
		SeekBar seekBar = layout.findViewById(R.id.seekBar);
		TextView duration = layout.findViewById(R.id.txtDuration);

		File tempFile;
		try {
			Attachment a = attachmentRetriever.getMessageAttachment(attachment.getHeader());
			InputStream input = a.getStream();

			// Write input stream to temp file
			tempFile = File.createTempFile("voice", ".ogg", layout.getContext().getCacheDir());
			java.io.OutputStream output = new java.io.FileOutputStream(tempFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}
			output.flush();
			output.close();
			input.close();

		} catch (IOException | DbException e) {
			e.printStackTrace();
			return;
		}

		String filePath = tempFile.getAbsolutePath();
		MediaPlayer player = new MediaPlayer();

		try {
			player.setDataSource(filePath);
			player.prepare();
			duration.setText(formatDuration(player.getDuration()));
		} catch (IOException e) {
			e.printStackTrace();
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



	private void bindTextItem() {
		resetStatusLayoutForText();
		textConstraints.applyTo(layout);
		if (adapter != null) adapter.clear();
	}

	private void bindImageItem(ConversationMessageItem item) {
		if (adapter == null) return; // Prevent crash if layout lacks imageList

		ConstraintSet constraintSet;
		if (item.getText() == null) {
			statusLayout.setBackgroundResource(R.drawable.msg_status_bubble);
			time.setTextColor(timeColorBubble);
			setImageTintList(bomb, ColorStateList.valueOf(timeColorBubble));
			constraintSet = imageConstraints;
		} else {
			resetStatusLayoutForText();
			constraintSet = imageTextConstraints;
		}

		if (item.getAttachments().size() == 1) {
			AttachmentItem attachment = item.getAttachments().get(0);
			constraintSet.constrainWidth(R.id.imageList, attachment.getThumbnailWidth());
			constraintSet.constrainHeight(R.id.imageList, attachment.getThumbnailHeight());
		} else {
			constraintSet.constrainWidth(R.id.imageList, WRAP_CONTENT);
			constraintSet.constrainHeight(R.id.imageList, WRAP_CONTENT);
		}
		constraintSet.applyTo(layout);
		adapter.setConversationItem(item);
		adapter.notifyDataSetChanged();
	}

	private void resetStatusLayoutForText() {
		statusLayout.setBackgroundResource(0);
		// also reset padding (the background drawable defines some)
		statusLayout.setPadding(0, 0, 0, 0);
		time.setTextColor(timeColor);
		setImageTintList(bomb, ColorStateList.valueOf(timeColor));
	}

}
