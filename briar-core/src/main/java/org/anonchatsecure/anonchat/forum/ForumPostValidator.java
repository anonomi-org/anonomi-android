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
import org.anonchatsecure.bramble.api.UniqueId;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
import org.anonchatsecure.bramble.api.client.BdfMessageValidator;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;
import java.util.Collection;

import javax.annotation.concurrent.Immutable;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkLength;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkSize;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_AUTHOR;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_PARENT;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_READ;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_AUDIO_SIZE;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_POST_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_HAS_AUDIO;
import static org.anonchatsecure.anonchat.api.forum.ForumPostFactory.SIGNING_LABEL_AUDIO_POST;
import static org.anonchatsecure.anonchat.api.forum.ForumPostFactory.SIGNING_LABEL_POST;

@Immutable
@NotNullByDefault
class ForumPostValidator extends BdfMessageValidator {

	ForumPostValidator(ClientHelper clientHelper,
			MetadataEncoder metadataEncoder, Clock clock) {
		super(clientHelper, metadataEncoder, clock);
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException {
		// Parent ID, author, text, signature [, audioData, contentType]
		boolean hasAudio = body.size() == 6;
		if (!hasAudio) checkSize(body, 4);

		// Parent ID is optional
		byte[] parent = body.getOptionalRaw(0);
		checkLength(parent, UniqueId.LENGTH);

		// Author
		BdfList authorList = body.getList(1);
		Author author = clientHelper.parseAndValidateAuthor(authorList);

		// Text
		String text = body.getString(2);
		checkLength(text, 0, MAX_FORUM_POST_TEXT_LENGTH);

		// Signature
		byte[] sig = body.getRaw(3);
		checkLength(sig, 1, MAX_SIGNATURE_LENGTH);

		// Audio data (optional)
		byte[] audioData = null;
		String contentType = null;
		if (hasAudio) {
			audioData = body.getRaw(4);
			checkLength(audioData, 1, MAX_FORUM_AUDIO_SIZE);
			contentType = body.getString(5);
			checkLength(contentType, 1, 50);
		}

		// Verify the signature
		String signingLabel;
		BdfList signed;
		if (hasAudio) {
			signingLabel = SIGNING_LABEL_AUDIO_POST;
			signed = BdfList.of(g.getId(), m.getTimestamp(), parent,
					authorList, text, audioData, contentType);
		} else {
			signingLabel = SIGNING_LABEL_POST;
			signed = BdfList.of(g.getId(), m.getTimestamp(), parent,
					authorList, text);
		}
		try {
			clientHelper.verifySignature(sig, signingLabel,
					signed, author.getPublicKey());
		} catch (GeneralSecurityException e) {
			throw new InvalidMessageException(e);
		}

		// Return the metadata and dependencies
		BdfDictionary meta = new BdfDictionary();
		Collection<MessageId> dependencies = emptyList();
		meta.put(KEY_TIMESTAMP, m.getTimestamp());
		if (parent != null) {
			meta.put(KEY_PARENT, parent);
			dependencies = singletonList(new MessageId(parent));
		}
		meta.put(KEY_AUTHOR, authorList);
		meta.put(KEY_READ, false);
		if (hasAudio) meta.put(KEY_HAS_AUDIO, true);
		return new BdfMessageContext(meta, dependencies);
	}
}
