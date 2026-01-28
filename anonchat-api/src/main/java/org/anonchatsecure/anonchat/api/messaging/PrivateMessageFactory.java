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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.List;

import javax.annotation.Nullable;

@NotNullByDefault
public interface PrivateMessageFactory {

	/**
	 * Creates a private message in the
	 * {@link PrivateMessageFormat#TEXT_ONLY TEXT_ONLY} format.
	 */
	PrivateMessage createLegacyPrivateMessage(GroupId groupId, long timestamp,
			String text) throws FormatException;

	/**
	 * Creates a private message in the
	 * {@link PrivateMessageFormat#TEXT_IMAGES TEXT_IMAGES} format. This format
	 * requires the contact to support client version 0.1 or higher.
	 */
	PrivateMessage createPrivateMessage(GroupId groupId, long timestamp,
			@Nullable String text, List<AttachmentHeader> headers)
			throws FormatException;

	/**
	 * Creates a private message in the
	 * {@link PrivateMessageFormat#TEXT_IMAGES_AUTO_DELETE TEXT_IMAGES_AUTO_DELETE}
	 * format. This format requires the contact to support client version 0.3
	 * or higher.
	 */
	PrivateMessage createPrivateMessage(GroupId groupId, long timestamp,
			@Nullable String text, List<AttachmentHeader> headers,
			long autoDeleteTimer) throws FormatException;
}
