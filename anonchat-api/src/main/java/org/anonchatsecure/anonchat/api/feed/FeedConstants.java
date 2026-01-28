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

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

public interface FeedConstants {

	/* delay after start before fetching feed */
	int FETCH_DELAY_INITIAL = 1;

	/* the interval the feed should be fetched */
	int FETCH_INTERVAL = 30;

	/* the unit that applies to the fetch times */
	TimeUnit FETCH_UNIT = MINUTES;

	// group metadata keys
	String KEY_FEEDS = "feeds";
	String KEY_FEED_URL = "feedURL";
	String KEY_FEED_AUTHOR = "feedAuthor";
	String KEY_FEED_PRIVATE_KEY = "feedPrivateKey";
	String KEY_FEED_DESC = "feedDesc";
	String KEY_FEED_RSS_AUTHOR = "feedRssAuthor";
	String KEY_FEED_RSS_TITLE = "feedRssTitle";
	String KEY_FEED_RSS_LINK = "feedRssLink";
	String KEY_FEED_RSS_URI = "feedRssUri";
	String KEY_FEED_ADDED = "feedAdded";
	String KEY_FEED_UPDATED = "feedUpdated";
	String KEY_FEED_LAST_ENTRY = "feedLastEntryTime";

}
