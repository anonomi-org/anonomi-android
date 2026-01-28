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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface BlogFactory {

	/**
	 * Creates a personal blog for a given author.
	 */
	Blog createBlog(Author author);

	/**
	 * Creates a RSS feed blog for a given author.
	 */
	Blog createFeedBlog(Author author);

	/**
	 * Parses a blog with the given Group
	 */
	Blog parseBlog(Group g) throws FormatException;

}
