/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.api.blog;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.PostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import static org.anonchatsecure.anonchat.api.blog.MessageType.WRAPPED_COMMENT;
import static org.anonchatsecure.anonchat.api.blog.MessageType.WRAPPED_POST;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class BlogPostHeader extends PostHeader {

	private final MessageType type;
	private final GroupId groupId;
	private final long timeReceived;
	private final boolean rssFeed;
	private final boolean hasImage;
	private final MessageId originalId;

	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			@Nullable MessageId parentId, long timestamp, long timeReceived,
			Author author, AuthorInfo authorInfo, boolean rssFeed,
			boolean read) {
		this(type, groupId, id, parentId, timestamp, timeReceived, author,
				authorInfo, rssFeed, read, false);
	}

	/** For a message that is its own original; rejects a wrapped copy. */
	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			@Nullable MessageId parentId, long timestamp, long timeReceived,
			Author author, AuthorInfo authorInfo, boolean rssFeed,
			boolean read, boolean hasImage) {
		this(type, groupId, id, parentId, timestamp, timeReceived, author,
				authorInfo, rssFeed, read, hasImage, id);
	}

	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			@Nullable MessageId parentId, long timestamp, long timeReceived,
			Author author, AuthorInfo authorInfo, boolean rssFeed,
			boolean read, boolean hasImage, MessageId originalId) {
		super(id, parentId, timestamp, author, authorInfo, read);
		// A wrapped copy is a different message in a different blog, so its
		// own ID as the original would key it onto the wrong post.
		if ((type == WRAPPED_POST || type == WRAPPED_COMMENT)
				&& originalId.equals(id)) {
			throw new IllegalArgumentException(
					"A wrapped copy needs the ID it had in its first blog");
		}
		this.type = type;
		this.groupId = groupId;
		this.timeReceived = timeReceived;
		this.rssFeed = rssFeed;
		this.hasImage = hasImage;
		this.originalId = originalId;
	}

	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			long timestamp, long timeReceived, Author author,
			AuthorInfo authorInfo, boolean rssFeed, boolean read) {
		this(type, groupId, id, null, timestamp, timeReceived, author,
				authorInfo, rssFeed, read, false);
	}

	public MessageType getType() {
		return type;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public long getTimeReceived() {
		return timeReceived;
	}

	public boolean isRssFeed() {
		return rssFeed;
	}

	public boolean hasImage() {
		return hasImage;
	}

	/**
	 * The ID this message had in the blog it was first posted to, which is
	 * the same in every blog it has been wrapped into. Equal to
	 * {@link #getId()} unless this is a wrapped copy.
	 */
	public MessageId getOriginalId() {
		return originalId;
	}

}
