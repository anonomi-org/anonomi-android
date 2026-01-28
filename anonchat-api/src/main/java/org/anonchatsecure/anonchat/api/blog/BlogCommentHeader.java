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
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.api.blog.MessageType.COMMENT;
import static org.anonchatsecure.anonchat.api.blog.MessageType.WRAPPED_COMMENT;

@Immutable
@NotNullByDefault
public class BlogCommentHeader extends BlogPostHeader {

	@Nullable
	private final String comment;
	private final BlogPostHeader parent;

	public BlogCommentHeader(MessageType type, GroupId groupId,
			@Nullable String comment, BlogPostHeader parent, MessageId id,
			long timestamp, long timeReceived, Author author,
			AuthorInfo authorInfo, boolean read) {

		super(type, groupId, id, parent.getId(), timestamp,
				timeReceived, author, authorInfo, false, read);

		if (type != COMMENT && type != WRAPPED_COMMENT)
			throw new IllegalArgumentException("Incompatible Message Type");

		this.comment = comment;
		this.parent = parent;
	}

	@Nullable
	public String getComment() {
		return comment;
	}

	public BlogPostHeader getParent() {
		return parent;
	}

	public BlogPostHeader getRootPost() {
		if (parent instanceof BlogCommentHeader)
			return ((BlogCommentHeader) parent).getRootPost();
		return parent;
	}

}
