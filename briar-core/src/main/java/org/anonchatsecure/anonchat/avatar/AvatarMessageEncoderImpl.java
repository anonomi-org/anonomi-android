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

import org.anonchatsecure.bramble.api.Pair;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.anonchat.api.attachment.FileTooBigException;
import org.anonchatsecure.anonchat.api.avatar.AvatarMessageEncoder;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.anonchatsecure.bramble.util.IoUtils.copyAndClose;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_CONTENT_TYPE;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_DESCRIPTOR_LENGTH;
import static org.anonchatsecure.anonchat.avatar.AvatarConstants.MSG_KEY_VERSION;
import static org.anonchatsecure.anonchat.avatar.AvatarConstants.MSG_TYPE_UPDATE;

@Immutable
@NotNullByDefault
class AvatarMessageEncoderImpl implements AvatarMessageEncoder {

	private final ClientHelper clientHelper;
	private final Clock clock;

	@Inject
	AvatarMessageEncoderImpl(ClientHelper clientHelper, Clock clock) {
		this.clientHelper = clientHelper;
		this.clock = clock;
	}

	@Override
	public Pair<Message, BdfDictionary> encodeUpdateMessage(GroupId groupId,
			long version, String contentType, InputStream in)
			throws IOException {
		// 0.0: Message Type, Version, Content-Type
		BdfList list = BdfList.of(MSG_TYPE_UPDATE, version, contentType);
		byte[] descriptor = clientHelper.toByteArray(list);

		// add BdfList and stream content to body
		ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
		bodyOut.write(descriptor);
		copyAndClose(in, bodyOut);
		if (bodyOut.size() > MAX_MESSAGE_BODY_LENGTH)
			throw new FileTooBigException();

		// assemble message
		byte[] body = bodyOut.toByteArray();
		long timestamp = clock.currentTimeMillis();
		Message m = clientHelper.createMessage(groupId, timestamp, body);

		// encode metadata
		BdfDictionary meta = new BdfDictionary();
		meta.put(MSG_KEY_VERSION, version);
		meta.put(MSG_KEY_CONTENT_TYPE, contentType);
		meta.put(MSG_KEY_DESCRIPTOR_LENGTH, descriptor.length);

		return new Pair<>(m, meta);
	}

}
