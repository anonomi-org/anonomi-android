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

package org.anonchatsecure.anonchat.api.forum;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.PostHeader;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ForumPostHeader extends PostHeader {

	private final boolean hasAudio;
	private final boolean hasImage;

	public ForumPostHeader(MessageId id, @Nullable MessageId parentId,
			long timestamp, Author author, AuthorInfo authorInfo,
			boolean read) {
		this(id, parentId, timestamp, author, authorInfo, read, false, false);
	}

	public ForumPostHeader(MessageId id, @Nullable MessageId parentId,
			long timestamp, Author author, AuthorInfo authorInfo,
			boolean read, boolean hasAudio) {
		this(id, parentId, timestamp, author, authorInfo, read, hasAudio,
				false);
	}

	public ForumPostHeader(MessageId id, @Nullable MessageId parentId,
			long timestamp, Author author, AuthorInfo authorInfo,
			boolean read, boolean hasAudio, boolean hasImage) {
		super(id, parentId, timestamp, author, authorInfo, read);
		this.hasAudio = hasAudio;
		this.hasImage = hasImage;
	}

	public boolean hasAudio() {
		return hasAudio;
	}

	public boolean hasImage() {
		return hasImage;
	}

}
