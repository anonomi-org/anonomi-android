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

package org.anonchatsecure.anonchat.api.client;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
public interface MessageTracker {

	/**
	 * Initializes the group count with zero messages,
	 * but uses the current time as latest message time for sorting.
	 */
	void initializeGroupCount(Transaction txn, GroupId g) throws DbException;

	/**
	 * Gets the number of visible and unread messages in the group
	 * as well as the timestamp of the latest message
	 **/
	GroupCount getGroupCount(GroupId g) throws DbException;

	/**
	 * Gets the number of visible and unread messages in the group
	 * as well as the timestamp of the latest message
	 **/
	GroupCount getGroupCount(Transaction txn, GroupId g) throws DbException;

	/**
	 * Updates the group count for the given incoming message.
	 * <p>
	 * For messages that are part of a conversation (private chat),
	 * use the corresponding function inside
	 * {@link ConversationManager} instead.
	 */
	void trackIncomingMessage(Transaction txn, Message m) throws DbException;

	/**
	 * Updates the group count for the given outgoing message.
	 * <p>
	 * For messages that are part of a conversation (private chat),
	 * use the corresponding function inside
	 * {@link ConversationManager} instead.
	 */
	void trackOutgoingMessage(Transaction txn, Message m) throws DbException;

	/**
	 * Updates the group count for the given message.
	 * <p>
	 * For messages that are part of a conversation (private chat),
	 * use the corresponding function inside
	 * {@link ConversationManager} instead.
	 */
	void trackMessage(Transaction txn, GroupId g, long timestamp, boolean read)
			throws DbException;

	/**
	 * Loads the stored message id for the respective group id or returns null
	 * if none is available.
	 */
	@Nullable
	MessageId loadStoredMessageId(GroupId g) throws DbException;

	/**
	 * Stores the message id for the respective group id. Exactly one message id
	 * can be stored for any group id at any time, older values are overwritten.
	 */
	void storeMessageId(GroupId g, MessageId m) throws DbException;

	/**
	 * Marks a message as read or unread and updates the group count.
	 *
	 * @return True if the message was previously marked as read
	 */
	boolean setReadFlag(Transaction txn, GroupId g, MessageId m, boolean read)
			throws DbException;

	/**
	 * Resets the {@link GroupCount} to the given msgCount and unreadCount.
	 * The latestMsgTime will be set to the current time.
	 * <p>
	 * Such reset is needed when recalculating the counts
	 * after deleting messages from a group.
	 */
	void resetGroupCount(Transaction txn, GroupId g, int msgCount,
			int unreadCount) throws DbException;

	class GroupCount {

		private final int msgCount, unreadCount;
		private final long latestMsgTime;

		public GroupCount(int msgCount, int unreadCount, long latestMsgTime) {
			this.msgCount = msgCount;
			this.unreadCount = unreadCount;
			this.latestMsgTime = latestMsgTime;
		}

		public int getMsgCount() {
			return msgCount;
		}

		public int getUnreadCount() {
			return unreadCount;
		}

		public long getLatestMsgTime() {
			return latestMsgTime;
		}
	}

}
