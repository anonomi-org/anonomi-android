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

package org.anonchatsecure.anonchat.api.client;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class PostHeader {

	private final MessageId id;
	@Nullable
	private final MessageId parentId;
	private final long timestamp;
	private final Author author;
	private final AuthorInfo authorInfo;
	private final boolean read;

	public PostHeader(MessageId id, @Nullable MessageId parentId,
			long timestamp, Author author, AuthorInfo authorInfo, boolean read) {
		this.id = id;
		this.parentId = parentId;
		this.timestamp = timestamp;
		this.author = author;
		this.authorInfo = authorInfo;
		this.read = read;
	}

	public MessageId getId() {
		return id;
	}

	public Author getAuthor() {
		return author;
	}

	public Status getAuthorStatus() {
		return authorInfo.getStatus();
	}

	public AuthorInfo getAuthorInfo() {
		return authorInfo;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isRead() {
		return read;
	}

	@Nullable
	public MessageId getParentId() {
		return parentId;
	}
}
