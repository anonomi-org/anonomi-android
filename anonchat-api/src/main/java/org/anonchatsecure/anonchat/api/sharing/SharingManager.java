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

package org.anonchatsecure.anonchat.api.sharing;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.client.ProtocolStateException;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager.ConversationClient;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

import javax.annotation.Nullable;

@NotNullByDefault
public interface SharingManager<S extends Shareable>
		extends ConversationClient {

	enum SharingStatus {
		/**
		 * The {@link Shareable} can be shared with the requested contact.
		 */
		SHAREABLE,
		/**
		 * The {@link Shareable} can not be shared with the requested contact,
		 * because the contact was already invited.
		 */
		INVITE_SENT,
		/**
		 * The {@link Shareable} can not be shared with the requested contact,
		 * because the contact has already invited us.
		 */
		INVITE_RECEIVED,
		/**
		 * The {@link Shareable} can not be shared with the requested contact,
		 * because it is already being shared.
		 */
		SHARING,
		/**
		 * The {@link Shareable} can not be shared with the requested contact,
		 * because it is not supported by that contact.
		 * This could be a missing or outdated client.
		 */
		NOT_SUPPORTED,
		/**
		 * The sharing session has encountered an error.
		 */
		ERROR
	}

	/**
	 * Sends an invitation to share the given group with the given contact,
	 * including optional text.
	 */
	void sendInvitation(GroupId shareableId, ContactId contactId,
			@Nullable String text) throws DbException;

	/**
	 * Sends an invitation to share the given group with the given contact,
	 * including optional text.
	 */
	void sendInvitation(Transaction txn, GroupId shareableId,
			ContactId contactId, @Nullable String text) throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(S s, Contact c, boolean accept)
			throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(Transaction txn, S s, Contact c, boolean accept)
			throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(ContactId c, SessionId id, boolean accept)
			throws DbException;

	/**
	 * Responds to a pending group invitation
	 */
	void respondToInvitation(Transaction txn, ContactId c, SessionId id,
			boolean accept) throws DbException;

	/**
	 * Returns all invitations to groups.
	 */
	Collection<SharingInvitationItem> getInvitations() throws DbException;

	/**
	 * Returns all invitations to groups.
	 */
	Collection<SharingInvitationItem> getInvitations(Transaction txn)
			throws DbException;

	/**
	 * Returns all contacts with whom the given group is shared.
	 */
	Collection<Contact> getSharedWith(GroupId g) throws DbException;

	/**
	 * Returns all contacts with whom the given group is shared.
	 */
	Collection<Contact> getSharedWith(Transaction txn, GroupId g)
			throws DbException;

	/**
	 * Returns the current {@link SharingStatus} for the given {@link Contact}
	 * and {@link Shareable} identified by the given {@link GroupId}.
	 * This indicates whether the {@link Shareable} can be shared
	 * with the contact.
	 *
	 * @throws ProtocolStateException if we already left the {@link Shareable}.
	 */
	SharingStatus getSharingStatus(GroupId g, Contact c) throws DbException;

	/**
	 * Returns the current {@link SharingStatus} for the given {@link Contact}
	 * and {@link Shareable} identified by the given {@link GroupId}.
	 * This indicates whether the {@link Shareable} can be shared
	 * with the contact.
	 *
	 * @throws ProtocolStateException if we already left the {@link Shareable}.
	 */
	SharingStatus getSharingStatus(Transaction txn, GroupId g, Contact c)
			throws DbException;

}
