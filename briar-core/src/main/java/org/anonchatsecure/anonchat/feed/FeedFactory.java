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

import com.rometools.rome.feed.synd.SyndFeed;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.anonchat.api.feed.Feed;

import javax.annotation.Nullable;

interface FeedFactory {

	/**
	 * Create a new feed based on the feed url
	 * and the metadata of an existing {@link SyndFeed}.
	 */
	Feed createFeed(@Nullable String url, SyndFeed sf);

	/**
	 * Creates a new updated feed, based on the given existing feed,
	 * new metadata from the given {@link SyndFeed}
	 * and the time of the last feed entry.
	 */
	Feed updateFeed(Feed feed, SyndFeed sf, long lastEntryTime);

	/**
	 * De-serializes a {@link BdfDictionary} into a {@link Feed}.
	 */
	Feed createFeed(BdfDictionary d) throws FormatException;

	/**
	 * Serializes a {@link Feed} into a {@link BdfDictionary}.
	 */
	BdfDictionary feedToBdfDictionary(Feed feed);

}
