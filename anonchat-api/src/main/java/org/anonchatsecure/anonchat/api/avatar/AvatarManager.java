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

package org.anonchatsecure.anonchat.api.avatar;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

@NotNullByDefault
public interface AvatarManager {

	/**
	 * The unique ID of the avatar client.
	 */
	ClientId CLIENT_ID = new ClientId("org.anonchatsecure.anonchat.avatar");

	/**
	 * The current major version of the avatar client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the avatar client.
	 */
	int MINOR_VERSION = 0;

	/**
	 * Store a new profile image represented by the given InputStream
	 * and share it with all contacts.
	 */
	AttachmentHeader addAvatar(String contentType, InputStream in)
			throws DbException, IOException;

	/**
	 * Returns the current known profile image header for the given contact
	 * or null if none is known.
	 */
	@Nullable
	AttachmentHeader getAvatarHeader(Transaction txn, Contact c)
			throws DbException;

	/**
	 * Returns our current profile image header or null if none has been added.
	 */
	@Nullable
	AttachmentHeader getMyAvatarHeader(Transaction txn) throws DbException;
}
