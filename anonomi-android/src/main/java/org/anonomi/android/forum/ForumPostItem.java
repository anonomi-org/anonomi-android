package org.anonomi.android.forum;

import org.anonomi.R;
import org.anonomi.android.threaded.ThreadItem;
import org.anonchatsecure.anonchat.api.forum.ForumPostHeader;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;

@NotThreadSafe
class ForumPostItem extends ThreadItem {

	@Nullable
	private final byte[] audioData;
	@Nullable
	private final String audioContentType;

	ForumPostItem(ForumPostHeader h, String text) {
		this(h, text, null, null);
	}

	ForumPostItem(ForumPostHeader h, String text,
			@Nullable byte[] audioData, @Nullable String audioContentType) {
		super(h.getId(), h.getParentId(), text, h.getTimestamp(), h.getAuthor(),
				h.getAuthorInfo(), h.isRead());
		this.audioData = audioData;
		this.audioContentType = audioContentType;
	}

	public boolean hasAudio() {
		return audioData != null && audioData.length > 0;
	}

	@Nullable
	public byte[] getAudioData() {
		return audioData;
	}

	@Nullable
	public String getAudioContentType() {
		return audioContentType;
	}

	@LayoutRes
	public int getLayout() {
		if (hasAudio()) return R.layout.list_item_thread_audio;
		return R.layout.list_item_thread;
	}

}
