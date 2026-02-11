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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.crypto.CryptoExecutor;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;

import static org.anonchatsecure.anonchat.api.forum.ForumManager.CLIENT_ID;

@NotNullByDefault
public interface ForumPostFactory {

	String SIGNING_LABEL_POST = CLIENT_ID.getString() + "/POST";
	String SIGNING_LABEL_AUDIO_POST = CLIENT_ID.getString() + "/AUDIO_POST";

	@CryptoExecutor
	ForumPost createPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text)
			throws FormatException, GeneralSecurityException;

	@CryptoExecutor
	ForumPost createAudioPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text,
			byte[] audioData, String contentType)
			throws FormatException, GeneralSecurityException;

}
