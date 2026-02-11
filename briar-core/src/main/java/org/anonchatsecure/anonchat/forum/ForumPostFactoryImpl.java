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

package org.anonchatsecure.anonchat.forum;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.forum.ForumPost;
import org.anonchatsecure.anonchat.api.forum.ForumPostFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.util.StringUtils.utf8IsTooLong;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_POST_TEXT_LENGTH;

@Immutable
@NotNullByDefault
class ForumPostFactoryImpl implements ForumPostFactory {

	private final ClientHelper clientHelper;

	@Inject
	ForumPostFactoryImpl(ClientHelper clientHelper) {
		this.clientHelper = clientHelper;
	}

	@Override
	public ForumPost createPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text)
			throws FormatException, GeneralSecurityException {
		// Validate the arguments
		if (utf8IsTooLong(text, MAX_FORUM_POST_TEXT_LENGTH))
			throw new IllegalArgumentException();
		// Serialise the data to be signed
		BdfList authorList = clientHelper.toList(author);
		BdfList signed = BdfList.of(groupId, timestamp, parent, authorList,
				text);
		// Sign the data
		byte[] sig = clientHelper.sign(SIGNING_LABEL_POST, signed,
				author.getPrivateKey());
		// Serialise the signed message
		BdfList message = BdfList.of(parent, authorList, text, sig);
		Message m = clientHelper.createMessage(groupId, timestamp, message);
		return new ForumPost(m, parent, author);
	}

	@Override
	public ForumPost createAudioPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text,
			byte[] audioData, String contentType)
			throws FormatException, GeneralSecurityException {
		// Validate the arguments
		if (utf8IsTooLong(text, MAX_FORUM_POST_TEXT_LENGTH))
			throw new IllegalArgumentException();
		// Serialise the data to be signed
		BdfList authorList = clientHelper.toList(author);
		BdfList signed = BdfList.of(groupId, timestamp, parent, authorList,
				text, audioData, contentType);
		// Sign the data
		byte[] sig = clientHelper.sign(SIGNING_LABEL_AUDIO_POST, signed,
				author.getPrivateKey());
		// Serialise the signed message
		BdfList message = BdfList.of(parent, authorList, text, sig,
				audioData, contentType);
		Message m = clientHelper.createMessage(groupId, timestamp, message);
		return new ForumPost(m, parent, author);
	}

}
