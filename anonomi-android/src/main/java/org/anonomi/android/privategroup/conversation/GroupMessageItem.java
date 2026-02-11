package org.anonomi.android.privategroup.conversation;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.threaded.ThreadItem;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageHeader;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import androidx.annotation.LayoutRes;
import androidx.annotation.UiThread;

@UiThread
@NotThreadSafe
class GroupMessageItem extends ThreadItem {

	private final GroupId groupId;
	@Nullable
	private final byte[] audioData;
	@Nullable
	private final String audioContentType;
	@Nullable
	private final byte[] imageData;
	@Nullable
	private final String imageContentType;

	private GroupMessageItem(MessageId messageId, GroupId groupId,
			@Nullable MessageId parentId, String text, long timestamp,
			Author author, AuthorInfo authorInfo, boolean isRead,
			@Nullable byte[] audioData, @Nullable String audioContentType,
			@Nullable byte[] imageData, @Nullable String imageContentType) {
		super(messageId, parentId, text, timestamp, author, authorInfo, isRead);
		this.groupId = groupId;
		this.audioData = audioData;
		this.audioContentType = audioContentType;
		this.imageData = imageData;
		this.imageContentType = imageContentType;
	}

	GroupMessageItem(GroupMessageHeader h, String text) {
		this(h.getId(), h.getGroupId(), h.getParentId(), text, h.getTimestamp(),
				h.getAuthor(), h.getAuthorInfo(), h.isRead(),
				null, null, null, null);
	}

	GroupMessageItem(GroupMessageHeader h, String text,
			@Nullable byte[] audioData, @Nullable String audioContentType) {
		this(h.getId(), h.getGroupId(), h.getParentId(), text, h.getTimestamp(),
				h.getAuthor(), h.getAuthorInfo(), h.isRead(), audioData,
				audioContentType, null, null);
	}

	GroupMessageItem(GroupMessageHeader h, String text,
			@Nullable byte[] audioData, @Nullable String audioContentType,
			@Nullable byte[] imageData, @Nullable String imageContentType) {
		this(h.getId(), h.getGroupId(), h.getParentId(), text, h.getTimestamp(),
				h.getAuthor(), h.getAuthorInfo(), h.isRead(), audioData,
				audioContentType, imageData, imageContentType);
	}

	public GroupId getGroupId() {
		return groupId;
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

	public boolean hasImage() {
		return imageData != null && imageData.length > 0;
	}

	@Nullable
	public byte[] getImageData() {
		return imageData;
	}

	@Nullable
	public String getImageContentType() {
		return imageContentType;
	}

	@LayoutRes
	public int getLayout() {
		if (hasImage()) return R.layout.list_item_thread_image;
		if (hasAudio()) return R.layout.list_item_thread_audio;
		return R.layout.list_item_thread;
	}

}
