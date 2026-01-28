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

package org.anonchatsecure.anonchat.messaging;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.UniqueId;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
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
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_ATTACHMENTS_PER_MESSAGE;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.MSG_KEY_READ;
import static org.anonchatsecure.anonchat.messaging.MessageTypes.ATTACHMENT;
import static org.anonchatsecure.anonchat.messaging.MessageTypes.PRIVATE_MESSAGE;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_ATTACHMENT_HEADERS;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_HAS_TEXT;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_LOCAL;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_MSG_TYPE;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MSG_KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.util.ValidationUtils.validateAutoDeleteTimer;

@Immutable
@NotNullByDefault
class PrivateMessageValidator implements MessageValidator {

	private final BdfReaderFactory bdfReaderFactory;
	private final MetadataEncoder metadataEncoder;
	private final Clock clock;

	PrivateMessageValidator(BdfReaderFactory bdfReaderFactory,
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
			// TODO: Support large messages
			InputStream in = new ByteArrayInputStream(m.getBody());
			CountingInputStream countIn =
					new CountingInputStream(in, MAX_MESSAGE_BODY_LENGTH);
			BdfReader reader = bdfReaderFactory.createReader(countIn);
			BdfList list = reader.readList();
			long bytesRead = countIn.getBytesRead();
			BdfMessageContext context;
			if (list.size() == 1) {
				// Legacy private message
				if (!reader.eof()) throw new FormatException();
				context = validateLegacyPrivateMessage(m, list);
			} else {
				// Private message or attachment
				int messageType = list.getInt(0);
				if (messageType == PRIVATE_MESSAGE) {
					if (!reader.eof()) throw new FormatException();
					context = validatePrivateMessage(m, list);
				} else if (messageType == ATTACHMENT) {
					context = validateAttachment(m, list, bytesRead);
				} else {
					throw new InvalidMessageException();
				}
			}
			Metadata meta = metadataEncoder.encode(context.getDictionary());
			return new MessageContext(meta, context.getDependencies());
		} catch (IOException e) {
			throw new InvalidMessageException(e);
		}
	}

	private BdfMessageContext validateLegacyPrivateMessage(Message m,
			BdfList body) throws FormatException {
		// Client version 0.0: Private message text
		checkSize(body, 1);
		String text = body.getString(0);
		checkLength(text, 0, MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		// Return the metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_TIMESTAMP, m.getTimestamp());
		meta.put(MSG_KEY_LOCAL, false);
		meta.put(MSG_KEY_READ, false);
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validatePrivateMessage(Message m, BdfList body)
			throws FormatException {
		// Client version 0.1 to 0.2: Message type, optional private message
		// text, attachment headers.
		// Client version 0.3: Message type, optional private message text,
		// attachment headers, optional auto-delete timer.
		checkSize(body, 3, 4);
		String text = body.getOptionalString(1);
		checkLength(text, 0, MAX_PRIVATE_MESSAGE_TEXT_LENGTH);
		BdfList headers = body.getList(2);
		if (text == null) checkSize(headers, 1, MAX_ATTACHMENTS_PER_MESSAGE);
		else checkSize(headers, 0, MAX_ATTACHMENTS_PER_MESSAGE);
		for (int i = 0; i < headers.size(); i++) {
			BdfList header = headers.getList(i);
			// Message ID, content type
			checkSize(header, 2);
			byte[] id = header.getRaw(0);
			checkLength(id, UniqueId.LENGTH);
			String contentType = header.getString(1);
			checkLength(contentType, 1, MAX_CONTENT_TYPE_BYTES);
		}
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 4) {
			timer = validateAutoDeleteTimer(body.getOptionalLong(3));
		}
		// Return the metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_TIMESTAMP, m.getTimestamp());
		meta.put(MSG_KEY_LOCAL, false);
		meta.put(MSG_KEY_READ, false);
		meta.put(MSG_KEY_MSG_TYPE, PRIVATE_MESSAGE);
		meta.put(MSG_KEY_HAS_TEXT, text != null);
		meta.put(MSG_KEY_ATTACHMENT_HEADERS, headers);
		if (timer != NO_AUTO_DELETE_TIMER) {
			meta.put(MSG_KEY_AUTO_DELETE_TIMER, timer);
		}
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validateAttachment(Message m, BdfList descriptor,
			long descriptorLength) throws FormatException {
		// Message type, content type
		checkSize(descriptor, 2);
		String contentType = descriptor.getString(1);
		checkLength(contentType, 1, MAX_CONTENT_TYPE_BYTES);
		// Return the metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_TIMESTAMP, m.getTimestamp());
		meta.put(MSG_KEY_LOCAL, false);
		meta.put(MSG_KEY_MSG_TYPE, ATTACHMENT);
		meta.put(MSG_KEY_DESCRIPTOR_LENGTH, descriptorLength);
		meta.put(MSG_KEY_CONTENT_TYPE, contentType);
		return new BdfMessageContext(meta);
	}
}
