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

package org.anonchatsecure.anonchat.api.feed;

import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class Feed {

	private final Blog blog;
	private final LocalAuthor localAuthor;
	private final RssProperties properties;
	private final long added, updated, lastEntryTime;

	public Feed(Blog blog, LocalAuthor localAuthor, RssProperties properties,
			long added, long updated, long lastEntryTime) {
		this.blog = blog;
		this.localAuthor = localAuthor;
		this.properties = properties;
		this.added = added;
		this.updated = updated;
		this.lastEntryTime = lastEntryTime;
	}

	public GroupId getBlogId() {
		return blog.getId();
	}

	public Blog getBlog() {
		return blog;
	}

	public LocalAuthor getLocalAuthor() {
		return localAuthor;
	}

	public String getTitle() {
		return blog.getName();
	}

	public RssProperties getProperties() {
		return properties;
	}

	public long getAdded() {
		return added;
	}

	public long getUpdated() {
		return updated;
	}

	public long getLastEntryTime() {
		return lastEntryTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof Feed) {
			Feed f = (Feed) o;
			return blog.equals(f.blog);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return blog.hashCode();
	}
}
