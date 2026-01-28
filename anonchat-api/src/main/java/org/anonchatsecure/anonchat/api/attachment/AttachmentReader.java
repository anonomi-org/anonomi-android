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

package org.anonchatsecure.anonchat.api.attachment;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.NoSuchMessageException;
import org.anonchatsecure.bramble.api.db.Transaction;

public interface AttachmentReader {

	/**
	 * Returns the attachment with the given attachment header.
	 *
	 * @throws NoSuchMessageException If the header refers to a message in
	 * a different group from the one specified in the header, to a message
	 * that is not an attachment, or to an attachment that does not have the
	 * expected content type. This is meant to prevent social engineering
	 * attacks that use invalid attachment IDs to test whether messages exist
	 * in the victim's database
	 */
	Attachment getAttachment(AttachmentHeader h) throws DbException;

	/**
	 * Returns the attachment with the given attachment header.
	 *
	 * @throws NoSuchMessageException If the header refers to a message in
	 * a different group from the one specified in the header, to a message
	 * that is not an attachment, or to an attachment that does not have the
	 * expected content type. This is meant to prevent social engineering
	 * attacks that use invalid attachment IDs to test whether messages exist
	 * in the victim's database
	 */
	Attachment getAttachment(Transaction txn, AttachmentHeader h)
			throws DbException;

}
