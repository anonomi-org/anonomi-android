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

public enum PrivateMessageFormat {

	/**
	 * First version of the private message format, which doesn't support
	 * image attachments or auto-deletion.
	 */
	TEXT_ONLY,

	/**
	 * Second version of the private message format, which supports image
	 * attachments but not auto-deletion. Support for this format was
	 * added in client version 0.1.
	 */
	TEXT_IMAGES,

	/**
	 * Third version of the private message format, which supports image
	 * attachments and auto-deletion. Support for this format was added
	 * in client version 0.3.
	 */
	TEXT_IMAGES_AUTO_DELETE,

	/**
	 * Fourth version of the private message format, which additionally
	 * supports locations. Support for this format was added in client
	 * version 0.4.
	 */
	TEXT_IMAGES_AUTO_DELETE_LOCATION;

	// The constants are ordered by the features they add, so a format supports
	// a feature if it is not older than the one that introduced it. Comparing
	// with == instead would drop a feature each time a newer format is added.

	public boolean supportsImages() {
		return compareTo(TEXT_IMAGES) >= 0;
	}

	public boolean supportsAutoDelete() {
		return compareTo(TEXT_IMAGES_AUTO_DELETE) >= 0;
	}

	public boolean supportsLocation() {
		return compareTo(TEXT_IMAGES_AUTO_DELETE_LOCATION) >= 0;
	}
}
