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

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class BlogPostHeader extends PostHeader {

	private final MessageType type;
	private final GroupId groupId;
	private final long timeReceived;
	private final boolean rssFeed;

	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			@Nullable MessageId parentId, long timestamp, long timeReceived,
			Author author, AuthorInfo authorInfo, boolean rssFeed, boolean read) {
		super(id, parentId, timestamp, author, authorInfo, read);
		this.type = type;
		this.groupId = groupId;
		this.timeReceived = timeReceived;
		this.rssFeed = rssFeed;
	}

	public BlogPostHeader(MessageType type, GroupId groupId, MessageId id,
			long timestamp, long timeReceived, Author author,
			AuthorInfo authorInfo, boolean rssFeed, boolean read) {
		this(type, groupId, id, null, timestamp, timeReceived, author,
				authorInfo, rssFeed, read);
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

}
