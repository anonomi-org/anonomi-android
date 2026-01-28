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

package org.anonchatsecure.anonchat.feed;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.AuthorFactory;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogFactory;
import org.anonchatsecure.anonchat.api.feed.Feed;
import org.anonchatsecure.anonchat.api.feed.RssProperties;
import org.jmock.Expectations;
import org.junit.Test;

import static org.anonchatsecure.bramble.test.TestUtils.getGroup;
import static org.anonchatsecure.bramble.test.TestUtils.getLocalAuthor;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.blog.BlogManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.blog.BlogManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_ADDED;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_AUTHOR;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_DESC;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_LAST_ENTRY;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_PRIVATE_KEY;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_RSS_AUTHOR;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_RSS_LINK;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_RSS_TITLE;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_RSS_URI;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_UPDATED;
import static org.anonchatsecure.anonchat.api.feed.FeedConstants.KEY_FEED_URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeedFactoryImplTest extends BrambleMockTestCase {

	private final AuthorFactory authorFactory =
			context.mock(AuthorFactory.class);
	private final BlogFactory blogFactory = context.mock(BlogFactory.class);
	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final Clock clock = context.mock(Clock.class);

	private final LocalAuthor localAuthor = getLocalAuthor();
	private final Group blogGroup = getGroup(CLIENT_ID, MAJOR_VERSION);
	private final Blog blog = new Blog(blogGroup, localAuthor, true);
	private final BdfList authorList = BdfList.of("foo");
	private final long added = 123, updated = 234, lastEntryTime = 345;

	private final String url = getRandomString(123);
	private final String description = getRandomString(123);
	private final String rssAuthor = getRandomString(123);
	private final String title = getRandomString(123);
	private final String link = getRandomString(123);
	private final String uri = getRandomString(123);

	private final FeedFactoryImpl feedFactory = new FeedFactoryImpl(
			authorFactory, blogFactory, clientHelper, clock);

	@Test
	public void testSerialiseAndDeserialiseWithoutOptionalFields()
			throws Exception {
		RssProperties propertiesBefore = new RssProperties(null, null, null,
				null, null, null);
		Feed before = new Feed(blog, localAuthor, propertiesBefore, added,
				updated, lastEntryTime);


		context.checking(new Expectations() {{
			oneOf(clientHelper).toList(localAuthor);
			will(returnValue(authorList));
		}});

		BdfDictionary dict = feedFactory.feedToBdfDictionary(before);

		BdfDictionary expectedDict = BdfDictionary.of(
				new BdfEntry(KEY_FEED_AUTHOR, authorList),
				new BdfEntry(KEY_FEED_PRIVATE_KEY, localAuthor.getPrivateKey()),
				new BdfEntry(KEY_FEED_ADDED, added),
				new BdfEntry(KEY_FEED_UPDATED, updated),
				new BdfEntry(KEY_FEED_LAST_ENTRY, lastEntryTime)
		);
		assertEquals(expectedDict, dict);

		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(localAuthor));
			oneOf(blogFactory).createFeedBlog(localAuthor);
			will(returnValue(blog));
		}});

		Feed after = feedFactory.createFeed(dict);
		RssProperties afterProperties = after.getProperties();

		assertNull(afterProperties.getUrl());
		assertNull(afterProperties.getTitle());
		assertNull(afterProperties.getDescription());
		assertNull(afterProperties.getAuthor());
		assertNull(afterProperties.getLink());
		assertNull(afterProperties.getUri());
		assertEquals(added, after.getAdded());
		assertEquals(updated, after.getUpdated());
		assertEquals(lastEntryTime, after.getLastEntryTime());
	}

	@Test
	public void testSerialiseAndDeserialiseWithOptionalFields()
			throws Exception {
		RssProperties propertiesBefore = new RssProperties(url, title,
				description, rssAuthor, link, uri);
		Feed before = new Feed(blog, localAuthor, propertiesBefore, added,
				updated, lastEntryTime);


		context.checking(new Expectations() {{
			oneOf(clientHelper).toList(localAuthor);
			will(returnValue(authorList));
		}});

		BdfDictionary dict = feedFactory.feedToBdfDictionary(before);

		BdfDictionary expectedDict = BdfDictionary.of(
				new BdfEntry(KEY_FEED_AUTHOR, authorList),
				new BdfEntry(KEY_FEED_PRIVATE_KEY, localAuthor.getPrivateKey()),
				new BdfEntry(KEY_FEED_ADDED, added),
				new BdfEntry(KEY_FEED_UPDATED, updated),
				new BdfEntry(KEY_FEED_LAST_ENTRY, lastEntryTime),
				new BdfEntry(KEY_FEED_URL, url),
				new BdfEntry(KEY_FEED_RSS_TITLE, title),
				new BdfEntry(KEY_FEED_DESC, description),
				new BdfEntry(KEY_FEED_RSS_AUTHOR, rssAuthor),
				new BdfEntry(KEY_FEED_RSS_LINK, link),
				new BdfEntry(KEY_FEED_RSS_URI, uri)
		);
		assertEquals(expectedDict, dict);

		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(localAuthor));
			oneOf(blogFactory).createFeedBlog(localAuthor);
			will(returnValue(blog));
		}});

		Feed after = feedFactory.createFeed(dict);
		RssProperties afterProperties = after.getProperties();

		assertEquals(url, afterProperties.getUrl());
		assertEquals(title, afterProperties.getTitle());
		assertEquals(description, afterProperties.getDescription());
		assertEquals(rssAuthor, afterProperties.getAuthor());
		assertEquals(link, afterProperties.getLink());
		assertEquals(uri, afterProperties.getUri());
		assertEquals(added, after.getAdded());
		assertEquals(updated, after.getUpdated());
		assertEquals(lastEntryTime, after.getLastEntryTime());
	}
}
