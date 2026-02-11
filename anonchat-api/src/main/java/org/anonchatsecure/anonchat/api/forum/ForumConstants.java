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

package org.anonchatsecure.anonchat.api.forum;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;

public interface ForumConstants {

	/**
	 * The maximum length of a forum's name in UTF-8 bytes.
	 */
	int MAX_FORUM_NAME_LENGTH = 100;

	/**
	 * The length of a forum's random salt in bytes.
	 */
	int FORUM_SALT_LENGTH = 32;

	/**
	 * The maximum length of a forum post's text in UTF-8 bytes.
	 */
	int MAX_FORUM_POST_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 1024;

	/**
	 * The maximum size of audio data in a forum audio message in bytes.
	 */
	int MAX_FORUM_AUDIO_SIZE = 32 * 1024;

	// Metadata keys
	String KEY_TIMESTAMP = "timestamp";
	String KEY_PARENT = "parent";
	String KEY_AUTHOR = "author";
	String KEY_LOCAL = "local";
	String KEY_READ = "read";
	String KEY_HAS_AUDIO = "hasAudio";

}
