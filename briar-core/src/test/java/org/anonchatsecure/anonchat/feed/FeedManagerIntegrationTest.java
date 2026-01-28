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

import org.anonchatsecure.bramble.api.identity.Identity;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogPostHeader;
import org.anonchatsecure.anonchat.api.feed.Feed;
import org.anonchatsecure.anonchat.api.feed.FeedManager;
import org.briarproject.nullsafety.NullSafety;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;

import javax.annotation.Nullable;

import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;
import static org.briarproject.nullsafety.NullSafety.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeedManagerIntegrationTest extends BrambleTestCase {

	private static final String FEED_PATH =
			"src/test/resources/briarproject.org_news_index.xml";
	private static final String FEED_URL =
			"https://briarproject.org/news/index.xml";
	private static final String FEED_TITLE = "News - Briar";

	private LifecycleManager lifecycleManager;
	private FeedManager feedManager;
	private BlogManager blogManager;
	private final File testDir = getTestDirectory();
	private final File testFile = new File(testDir, "feedTest");

	@Before
	public void setUp() throws Exception {
		assertTrue(testDir.mkdirs());
		FeedManagerIntegrationTestComponent component =
				DaggerFeedManagerIntegrationTestComponent.builder()
						.testDatabaseConfigModule(
								new TestDatabaseConfigModule(testFile)).build();
		FeedManagerIntegrationTestComponent.Helper
				.injectEagerSingletons(component);
		component.inject(this);

		IdentityManager identityManager = component.getIdentityManager();
		Identity identity = identityManager.createIdentity("feedTest");
		identityManager.registerIdentity(identity);

		lifecycleManager = component.getLifecycleManager();
		lifecycleManager.startServices(getSecretKey());
		lifecycleManager.waitForStartup();

		feedManager = component.getFeedManager();
		blogManager = component.getBlogManager();
	}

	@Test
	public void testFeedImportAndRemovalFromUrl() throws Exception {
		testFeedImportAndRemoval(FEED_URL, null);
	}

	@Test
	public void testFeedImportAndRemovalFromFile() throws Exception {
		testFeedImportAndRemoval(null, FEED_PATH);
	}

	private void testFeedImportAndRemoval(@Nullable String url,
			@Nullable String path) throws Exception {
		// initially, there's only the one personal blog
		Collection<Blog> blogs = blogManager.getBlogs();
		assertEquals(1, blogs.size());
		Blog personalBlog = blogs.iterator().next();

		// add feed into a dedicated blog
		if (url == null) {
			feedManager.addFeed(new FileInputStream(requireNonNull(path)));
		} else {
			feedManager.addFeed(url);
		}

		// now there's the feed's blog too
		blogs = blogManager.getBlogs();
		assertEquals(2, blogs.size());
		Blog feedBlog = null;
		for (Blog blog : blogs) {
			if (!blog.equals(personalBlog)) feedBlog = blog;
		}
		assertNotNull(feedBlog);

		// check the feed got saved as expected
		Collection<Feed> feeds = feedManager.getFeeds();
		assertEquals(1, feeds.size());
		Feed feed = feeds.iterator().next();
		assertTrue(feed.getLastEntryTime() > 0);
		assertTrue(feed.getAdded() > 0);
		assertTrue(feed.getUpdated() > 0);
		assertTrue(NullSafety.equals(url, feed.getProperties().getUrl()));
		assertEquals(feedBlog, feed.getBlog());
		assertEquals(FEED_TITLE, feed.getTitle());
		assertEquals(feed.getTitle(), feed.getBlog().getName());
		assertEquals(feed.getTitle(), feed.getLocalAuthor().getName());

		// check the feed entries have been added to the blog as expected
		Collection<BlogPostHeader> headers =
				blogManager.getPostHeaders(feedBlog.getId());
		assertFalse(headers.isEmpty());
		for (BlogPostHeader header : headers) {
			assertTrue(header.isRssFeed());
		}

		// now let's remove the feed's blog again
		blogManager.removeBlog(feedBlog);
		blogs = blogManager.getBlogs();
		assertEquals(1, blogs.size());
		assertEquals(personalBlog, blogs.iterator().next());
		assertEquals(0, feedManager.getFeeds().size());
	}

	@After
	public void tearDown() throws Exception {
		lifecycleManager.stopServices();
		lifecycleManager.waitForShutdown();
		deleteTestDirectory(testDir);
	}
}
