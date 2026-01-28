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

package org.anonchatsecure.anonchat.sharing;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.sharing.Shareable;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface MessageParser<S extends Shareable> {

	BdfDictionary getMessagesVisibleInUiQuery();

	BdfDictionary getInvitesAvailableToAnswerQuery();

	BdfDictionary getInvitesAvailableToAnswerQuery(GroupId shareableId);

	MessageMetadata parseMetadata(BdfDictionary meta) throws FormatException;

	S createShareable(BdfList descriptor) throws FormatException;

	InviteMessage<S> getInviteMessage(Transaction txn, MessageId m)
			throws DbException, FormatException;

	InviteMessage<S> parseInviteMessage(Message m, BdfList body)
			throws FormatException;

	AcceptMessage parseAcceptMessage(Message m, BdfList body)
			throws FormatException;

	DeclineMessage parseDeclineMessage(Message m, BdfList body)
			throws FormatException;

	LeaveMessage parseLeaveMessage(Message m, BdfList body)
			throws FormatException;

	AbortMessage parseAbortMessage(Message m, BdfList body)
			throws FormatException;

}
