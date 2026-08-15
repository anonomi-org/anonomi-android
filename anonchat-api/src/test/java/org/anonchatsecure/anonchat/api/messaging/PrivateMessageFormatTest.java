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

package org.anonchatsecure.anonchat.api.messaging;

import org.junit.Test;

import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES_AUTO_DELETE;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES_AUTO_DELETE_LOCATION;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_ONLY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrivateMessageFormatTest {

	@Test
	public void testTextOnlySupportsNothing() {
		assertFalse(TEXT_ONLY.supportsImages());
		assertFalse(TEXT_ONLY.supportsAutoDelete());
	}

	@Test
	public void testTextImagesSupportsImagesOnly() {
		assertTrue(TEXT_IMAGES.supportsImages());
		assertFalse(TEXT_IMAGES.supportsAutoDelete());
	}

	@Test
	public void testTextImagesAutoDeleteSupportsBoth() {
		assertTrue(TEXT_IMAGES_AUTO_DELETE.supportsImages());
		assertTrue(TEXT_IMAGES_AUTO_DELETE.supportsAutoDelete());
		assertFalse(TEXT_IMAGES_AUTO_DELETE.supportsLocation());
	}

	@Test
	public void testTextImagesAutoDeleteLocationSupportsAll() {
		assertTrue(TEXT_IMAGES_AUTO_DELETE_LOCATION.supportsImages());
		assertTrue(TEXT_IMAGES_AUTO_DELETE_LOCATION.supportsAutoDelete());
		assertTrue(TEXT_IMAGES_AUTO_DELETE_LOCATION.supportsLocation());
	}

	/**
	 * Every format added after the one that introduced a feature has to keep
	 * reporting support for it, otherwise adding a format silently withdraws
	 * the feature from the contacts that just gained it.
	 */
	@Test
	public void testLaterFormatsKeepEarlierFeatures() {
		for (PrivateMessageFormat f : PrivateMessageFormat.values()) {
			if (f.compareTo(TEXT_IMAGES) >= 0) assertTrue(f.supportsImages());
			if (f.compareTo(TEXT_IMAGES_AUTO_DELETE) >= 0) {
				assertTrue(f.supportsAutoDelete());
			}
			if (f.compareTo(TEXT_IMAGES_AUTO_DELETE_LOCATION) >= 0) {
				assertTrue(f.supportsLocation());
			}
		}
	}
}
