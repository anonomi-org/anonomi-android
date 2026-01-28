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

package org.anonchatsecure.anonchat.api.identity;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface AuthorManager {

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(AuthorId a) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given author.
	 */
	AuthorInfo getAuthorInfo(Transaction txn, AuthorId a) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given contact.
	 */
	AuthorInfo getAuthorInfo(Contact c) throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the given contact.
	 */
	AuthorInfo getAuthorInfo(Transaction txn, Contact c)
			throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the {@link LocalAuthor}.
	 */
	AuthorInfo getMyAuthorInfo() throws DbException;

	/**
	 * Returns the {@link AuthorInfo} for the {@link LocalAuthor}.
	 */
	AuthorInfo getMyAuthorInfo(Transaction txn) throws DbException;
}
