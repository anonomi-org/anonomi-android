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

package org.anonchatsecure.anonchat.client;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_LATEST_MSG;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_MSG_COUNT;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_STORED_MESSAGE_ID;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_UNREAD_COUNT;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.MSG_KEY_READ;

@Immutable
@NotNullByDefault
class MessageTrackerImpl implements MessageTracker {

	private final DatabaseComponent db;
	private final ClientHelper clientHelper;
	private final Clock clock;

	@Inject
	MessageTrackerImpl(DatabaseComponent db, ClientHelper clientHelper,
			Clock clock) {
		this.db = db;
		this.clientHelper = clientHelper;
		this.clock = clock;
	}

	@Override
	public void initializeGroupCount(Transaction txn, GroupId g)
			throws DbException {
		long now = clock.currentTimeMillis();
		GroupCount groupCount = new GroupCount(0, 0, now);
		storeGroupCount(txn, g, groupCount);
	}

	@Override
	public void trackIncomingMessage(Transaction txn, Message m)
			throws DbException {
		trackMessage(txn, m.getGroupId(), m.getTimestamp(), false);
	}

	@Override
	public void trackOutgoingMessage(Transaction txn, Message m)
			throws DbException {
		trackMessage(txn, m.getGroupId(), m.getTimestamp(), true);
	}

	@Override
	public void trackMessage(Transaction txn, GroupId g, long time,
			boolean read) throws DbException {
		GroupCount c = getGroupCount(txn, g);
		int msgCount = c.getMsgCount() + 1;
		int unreadCount = c.getUnreadCount() + (read ? 0 : 1);
		long latestMsgTime = Math.max(c.getLatestMsgTime(), time);
		storeGroupCount(txn, g, new GroupCount(msgCount, unreadCount,
				latestMsgTime));
	}

	@Nullable
	@Override
	public MessageId loadStoredMessageId(GroupId g) throws DbException {
		try {
			BdfDictionary d = clientHelper.getGroupMetadataAsDictionary(g);
			byte[] msgBytes = d.getOptionalRaw(GROUP_KEY_STORED_MESSAGE_ID);
			return msgBytes != null ? new MessageId(msgBytes) : null;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void storeMessageId(GroupId g, MessageId m) throws DbException {
		BdfDictionary d = BdfDictionary.of(
				new BdfEntry(GROUP_KEY_STORED_MESSAGE_ID, m)
		);
		try {
			clientHelper.mergeGroupMetadata(g, d);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public GroupCount getGroupCount(GroupId g) throws DbException {
		GroupCount count;
		Transaction txn = db.startTransaction(true);
		try {
			count = getGroupCount(txn, g);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return count;
	}

	@Override
	public GroupCount getGroupCount(Transaction txn, GroupId g)
			throws DbException {
		try {
			BdfDictionary d = clientHelper.getGroupMetadataAsDictionary(txn, g);
			return new GroupCount(
					d.getInt(GROUP_KEY_MSG_COUNT, 0),
					d.getInt(GROUP_KEY_UNREAD_COUNT, 0),
					d.getLong(GROUP_KEY_LATEST_MSG, 0L)
			);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private void storeGroupCount(Transaction txn, GroupId g, GroupCount c)
			throws DbException {
		try {
			BdfDictionary d = BdfDictionary.of(
					new BdfEntry(GROUP_KEY_MSG_COUNT, c.getMsgCount()),
					new BdfEntry(GROUP_KEY_UNREAD_COUNT, c.getUnreadCount()),
					new BdfEntry(GROUP_KEY_LATEST_MSG, c.getLatestMsgTime())
			);
			clientHelper.mergeGroupMetadata(txn, g, d);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean setReadFlag(Transaction txn, GroupId g, MessageId m,
			boolean read) throws DbException {
		try {
			// check current read status of message
			BdfDictionary old =
					clientHelper.getMessageMetadataAsDictionary(txn, m);
			boolean wasRead = old.getBoolean(MSG_KEY_READ, false);

			// if status changed
			if (wasRead != read) {
				// mark individual message as read
				BdfDictionary meta = new BdfDictionary();
				meta.put(MSG_KEY_READ, read);
				clientHelper.mergeMessageMetadata(txn, m, meta);

				// update unread counter in group metadata
				GroupCount c = getGroupCount(txn, g);
				int unreadCount = c.getUnreadCount() + (read ? -1 : 1);
				if (unreadCount < 0) throw new DbException();
				storeGroupCount(txn, g, new GroupCount(c.getMsgCount(),
						unreadCount, c.getLatestMsgTime()));
			}
			return wasRead;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void resetGroupCount(Transaction txn, GroupId g, int msgCount,
			int unreadCount) throws DbException {
		long now = clock.currentTimeMillis();
		GroupCount groupCount = new GroupCount(msgCount, unreadCount, now);
		storeGroupCount(txn, g, groupCount);
	}

}
