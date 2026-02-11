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

package org.anonchatsecure.anonchat.api.privategroup;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;

public interface PrivateGroupConstants {

	/**
	 * The maximum length of a group's name in UTF-8 bytes.
	 */
	int MAX_GROUP_NAME_LENGTH = 100;

	/**
	 * The length of a group's random salt in bytes.
	 */
	int GROUP_SALT_LENGTH = 32;

	/**
	 * The maximum length of a group post's text in UTF-8 bytes.
	 */
	int MAX_GROUP_POST_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 1024;

	/**
	 * The maximum length of a group invitation's optional text in UTF-8 bytes.
	 */
	int MAX_GROUP_INVITATION_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 1024;

	/**
	 * The maximum size of audio data in a group audio message in bytes.
	 */
	int MAX_GROUP_AUDIO_SIZE = 32 * 1024;

}
