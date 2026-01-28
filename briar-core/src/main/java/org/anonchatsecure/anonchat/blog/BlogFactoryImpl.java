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

package org.anonchatsecure.anonchat.blog;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupFactory;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.util.ValidationUtils.checkSize;
import static org.anonchatsecure.anonchat.api.blog.BlogManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.blog.BlogManager.MAJOR_VERSION;

@Immutable
@NotNullByDefault
class BlogFactoryImpl implements BlogFactory {

	private final GroupFactory groupFactory;
	private final ClientHelper clientHelper;

	@Inject
	BlogFactoryImpl(GroupFactory groupFactory, ClientHelper clientHelper) {

		this.groupFactory = groupFactory;
		this.clientHelper = clientHelper;
	}

	@Override
	public Blog createBlog(Author a) {
		return createBlog(a, false);
	}

	@Override
	public Blog createFeedBlog(Author a) {
		return createBlog(a, true);
	}

	private Blog createBlog(Author a, boolean rssFeed) {
		try {
			BdfList blog = BdfList.of(clientHelper.toList(a), rssFeed);
			byte[] descriptor = clientHelper.toByteArray(blog);
			Group g = groupFactory.createGroup(CLIENT_ID, MAJOR_VERSION,
					descriptor);
			return new Blog(g, a, rssFeed);
		} catch (FormatException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Blog parseBlog(Group g) throws FormatException {
		// Author, RSS feed
		BdfList descriptor = clientHelper.toList(g.getDescriptor());
		checkSize(descriptor, 2);
		BdfList authorList = descriptor.getList(0);
		boolean rssFeed = descriptor.getBoolean(1);

		Author author = clientHelper.parseAndValidateAuthor(authorList);
		return new Blog(g, author, rssFeed);
	}

}
