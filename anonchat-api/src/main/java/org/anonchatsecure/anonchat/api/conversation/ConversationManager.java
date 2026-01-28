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

package org.anonchatsecure.anonchat.api.conversation;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.Set;

@NotNullByDefault
public interface ConversationManager {

	int DELETE_SESSION_INTRODUCTION_INCOMPLETE = 1;
	int DELETE_SESSION_INVITATION_INCOMPLETE = 1 << 1;
	int DELETE_SESSION_INTRODUCTION_IN_PROGRESS = 1 << 2;
	int DELETE_SESSION_INVITATION_IN_PROGRESS = 1 << 3;

	/**
	 * Clients that present messages in a private conversation need to
	 * register themselves here.
	 */
	void registerConversationClient(ConversationClient client);

	/**
	 * Returns the headers of all messages in the given private conversation.
	 * <p>
	 * Only {@link MessagingManager} returns only headers.
	 * The others also return the message text.
	 */
	Collection<ConversationMessageHeader> getMessageHeaders(ContactId c)
			throws DbException;

	/**
	 * Returns the headers of all messages in the given private conversation.
	 * <p>
	 * Only {@link MessagingManager} returns only headers.
	 * The others also return the message text.
	 */
	Collection<ConversationMessageHeader> getMessageHeaders(Transaction txn, ContactId c)
			throws DbException;

	/**
	 * Returns the unified group count for all private conversation messages.
	 */
	GroupCount getGroupCount(ContactId c) throws DbException;

	/**
	 * Returns the unified group count for all private conversation messages.
	 */
	GroupCount getGroupCount(Transaction txn, ContactId c) throws DbException;

	/**
	 * Updates the group count for the given incoming private conversation message
	 * and broadcasts a corresponding event.
	 */
	void trackIncomingMessage(Transaction txn, Message m)
			throws DbException;

	/**
	 * Updates the group count for the given outgoing private conversation message
	 * and broadcasts a corresponding event.
	 */
	void trackOutgoingMessage(Transaction txn, Message m)
			throws DbException;

	/**
	 * Updates the group count for the given private conversation message
	 * and broadcasts a corresponding event.
	 */
	void trackMessage(Transaction txn, GroupId g, long timestamp, boolean read)
			throws DbException;

	void setReadFlag(GroupId g, MessageId m, boolean read)
			throws DbException;

	void setReadFlag(Transaction txn, GroupId g, MessageId m, boolean read)
			throws DbException;

	/**
	 * Returns a timestamp for an outgoing message, which is later than the
	 * timestamp of any message in the conversation with the given contact.
	 */
	long getTimestampForOutgoingMessage(Transaction txn, ContactId c)
			throws DbException;

	/**
	 * Deletes all messages exchanged with the given contact.
	 */
	DeletionResult deleteAllMessages(ContactId c) throws DbException;

	/**
	 * Deletes all messages exchanged with the given contact.
	 */
	DeletionResult deleteAllMessages(Transaction txn, ContactId c)
			throws DbException;

	/**
	 * Deletes the given set of messages associated with the given contact.
	 */
	DeletionResult deleteMessages(ContactId c, Collection<MessageId> messageIds)
			throws DbException;

	/**
	 * Deletes the given set of messages associated with the given contact.
	 */
	DeletionResult deleteMessages(Transaction txn, ContactId c,
			Collection<MessageId> messageIds) throws DbException;

	@NotNullByDefault
	interface ConversationClient {

		Group getContactGroup(Contact c);

		Collection<ConversationMessageHeader> getMessageHeaders(Transaction txn,
				ContactId contactId) throws DbException;

		/**
		 * Returns all conversation {@link MessageId}s for the given contact
		 * this client is responsible for.
		 */
		Set<MessageId> getMessageIds(Transaction txn, ContactId contactId)
				throws DbException;

		GroupCount getGroupCount(Transaction txn, ContactId c)
				throws DbException;

		/**
		 * Deletes all messages associated with the given contact.
		 */
		DeletionResult deleteAllMessages(Transaction txn,
				ContactId c) throws DbException;

		/**
		 * Deletes the given set of messages associated with the given contact.
		 * <p>
		 * The set of message IDs must only include message IDs returned by
		 * {@link #getMessageIds}.
		 */
		DeletionResult deleteMessages(Transaction txn, ContactId c,
				Set<MessageId> messageIds) throws DbException;
	}

}
