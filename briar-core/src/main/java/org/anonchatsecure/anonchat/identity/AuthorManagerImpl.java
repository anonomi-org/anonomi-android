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

package org.anonchatsecure.anonchat.identity;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.avatar.AvatarManager;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.OURSELVES;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNKNOWN;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.VERIFIED;

@ThreadSafe
@NotNullByDefault
class AuthorManagerImpl implements AuthorManager {

	private final DatabaseComponent db;
	private final IdentityManager identityManager;
	private final AvatarManager avatarManager;

	@Inject
	AuthorManagerImpl(DatabaseComponent db, IdentityManager identityManager,
			AvatarManager avatarManager) {
		this.db = db;
		this.identityManager = identityManager;
		this.avatarManager = avatarManager;
	}

	@Override
	public AuthorInfo getAuthorInfo(AuthorId a) throws DbException {
		return db.transactionWithResult(true, txn -> getAuthorInfo(txn, a));
	}

	@Override
	public AuthorInfo getAuthorInfo(Transaction txn, AuthorId authorId)
			throws DbException {
		LocalAuthor localAuthor = identityManager.getLocalAuthor(txn);
		if (localAuthor.getId().equals(authorId)) return getMyAuthorInfo(txn);
		Collection<Contact> contacts = db.getContactsByAuthorId(txn, authorId);
		if (contacts.isEmpty()) return new AuthorInfo(UNKNOWN);
		if (contacts.size() > 1) throw new AssertionError();
		Contact c = contacts.iterator().next();
		return getAuthorInfo(txn, c);
	}

	@Override
	public AuthorInfo getAuthorInfo(Contact c) throws DbException {
		return db.transactionWithResult(true, txn -> getAuthorInfo(txn, c));
	}

	@Override
	public AuthorInfo getAuthorInfo(Transaction txn, Contact c)
			throws DbException {
		AttachmentHeader avatar = avatarManager.getAvatarHeader(txn, c);
		if (c.isVerified())
			return new AuthorInfo(VERIFIED, c.getAlias(), avatar);
		else return new AuthorInfo(UNVERIFIED, c.getAlias(), avatar);
	}

	@Override
	public AuthorInfo getMyAuthorInfo() throws DbException {
		return db.transactionWithResult(true, this::getMyAuthorInfo);
	}

	@Override
	public AuthorInfo getMyAuthorInfo(Transaction txn) throws DbException {
		AttachmentHeader avatar = avatarManager.getMyAvatarHeader(txn);
		return new AuthorInfo(OURSELVES, null, avatar);
	}

}
