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

package org.anonchatsecure.anonchat.attachment;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.NoSuchMessageException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.attachment.Attachment;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.attachment.AttachmentReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.inject.Inject;

import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_CONTENT_TYPE;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MSG_KEY_DESCRIPTOR_LENGTH;

public class AttachmentReaderImpl implements AttachmentReader {

	private final TransactionManager db;
	private final ClientHelper clientHelper;

	@Inject
	public AttachmentReaderImpl(TransactionManager db,
			ClientHelper clientHelper) {
		this.db = db;
		this.clientHelper = clientHelper;
	}

	@Override
	public Attachment getAttachment(AttachmentHeader h) throws DbException {
		return db.transactionWithResult(true, txn -> getAttachment(txn, h));
	}

	@Override
	public Attachment getAttachment(Transaction txn, AttachmentHeader h)
			throws DbException {
		// TODO: Support large messages
		MessageId m = h.getMessageId();
		Message message = clientHelper.getMessage(txn, m);
		// Check that the message is in the expected group, to prevent it from
		// being loaded in the context of a different group
		if (!message.getGroupId().equals(h.getGroupId())) {
			throw new NoSuchMessageException();
		}
		byte[] body = message.getBody();
		try {
			BdfDictionary meta =
					clientHelper.getMessageMetadataAsDictionary(txn, m);
			String contentType = meta.getString(MSG_KEY_CONTENT_TYPE);
			if (!contentType.equals(h.getContentType()))
				throw new NoSuchMessageException();
			int offset = meta.getInt(MSG_KEY_DESCRIPTOR_LENGTH);
			InputStream stream = new ByteArrayInputStream(body, offset,
					body.length - offset);
			return new Attachment(h, stream);
		} catch (FormatException e) {
			throw new NoSuchMessageException();
		}
	}

}
