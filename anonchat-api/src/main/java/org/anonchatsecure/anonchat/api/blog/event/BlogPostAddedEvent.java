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

package org.anonchatsecure.anonchat.api.blog.event;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a blog post is added to the database.
 */
@Immutable
@NotNullByDefault
public class BlogPostAddedEvent extends Event {

	private final GroupId groupId;
	private final BlogPostHeader header;
	private final boolean local;

	public BlogPostAddedEvent(GroupId groupId, BlogPostHeader header,
			boolean local) {

		this.groupId = groupId;
		this.header = header;
		this.local = local;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public BlogPostHeader getHeader() {
		return header;
	}

	public boolean isLocal() {
		return local;
	}
}
