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

package org.anonchatsecure.anonchat.avatar;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.BdfReader;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.db.Metadata;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageContext;
import org.anonchatsecure.bramble.api.sync.validation.MessageValidator;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.anonchat.attachment.CountingInputStream;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.MAX_CLOCK_DIFFERENCE;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkLength;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkSize;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_CONTENT_TYPE;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_DESCRIPTOR_LENGTH;
import static org.anonchatsecure.anonchat.avatar.AvatarConstants.MSG_KEY_VERSION;
import static org.anonchatsecure.anonchat.avatar.AvatarConstants.MSG_TYPE_UPDATE;

@Immutable
@NotNullByDefault
class AvatarValidator implements MessageValidator {

	private final BdfReaderFactory bdfReaderFactory;
	private final MetadataEncoder metadataEncoder;
	private final Clock clock;

	AvatarValidator(BdfReaderFactory bdfReaderFactory,
			MetadataEncoder metadataEncoder, Clock clock) {
		this.bdfReaderFactory = bdfReaderFactory;
		this.metadataEncoder = metadataEncoder;
		this.clock = clock;
	}

	@Override
	public MessageContext validateMessage(Message m, Group g)
			throws InvalidMessageException {
		// Reject the message if it's too far in the future
		long now = clock.currentTimeMillis();
		if (m.getTimestamp() - now > MAX_CLOCK_DIFFERENCE) {
			throw new InvalidMessageException(
					"Timestamp is too far in the future");
		}
		try {
			InputStream in = new ByteArrayInputStream(m.getBody());
			CountingInputStream countIn =
					new CountingInputStream(in, MAX_MESSAGE_BODY_LENGTH);
			BdfReader reader = bdfReaderFactory.createReader(countIn);
			BdfList list = reader.readList();
			long bytesRead = countIn.getBytesRead();
			BdfDictionary d = validateUpdate(list, bytesRead);
			Metadata meta = metadataEncoder.encode(d);
			return new MessageContext(meta);
		} catch (IOException e) {
			throw new InvalidMessageException(e);
		}
	}

	private BdfDictionary validateUpdate(BdfList body, long descriptorLength)
			throws FormatException {
		// 0.0: Message Type, Version, Content-Type
		checkSize(body, 3);
		// Message Type
		int messageType = body.getInt(0);
		if (messageType != MSG_TYPE_UPDATE) throw new FormatException();
		// Version
		long version = body.getLong(1);
		if (version < 0) throw new FormatException();
		// Content-Type
		String contentType = body.getString(2);
		checkLength(contentType, 1, MAX_CONTENT_TYPE_BYTES);

		// Return the metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_VERSION, version);
		meta.put(MSG_KEY_CONTENT_TYPE, contentType);
		meta.put(MSG_KEY_DESCRIPTOR_LENGTH, descriptorLength);
		return meta;
	}

}
