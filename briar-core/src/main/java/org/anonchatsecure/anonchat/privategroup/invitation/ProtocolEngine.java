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

package org.anonchatsecure.anonchat.privategroup.invitation;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
interface ProtocolEngine<S extends Session<?>> {

	S onInviteAction(Transaction txn, S session, @Nullable String text,
			long timestamp, byte[] signature, long autoDeleteTimer)
			throws DbException;

	S onJoinAction(Transaction txn, S session) throws DbException;

	/**
	 * Leaves the group or declines an invitation.
	 *
	 * @param isAutoDecline true if automatically declined due to deletion
	 * and false if initiated by the user.
	 */
	S onLeaveAction(Transaction txn, S session, boolean isAutoDecline)
			throws DbException;

	S onMemberAddedAction(Transaction txn, S session) throws DbException;

	S onInviteMessage(Transaction txn, S session, InviteMessage m)
			throws DbException, FormatException;

	S onJoinMessage(Transaction txn, S session, JoinMessage m)
			throws DbException, FormatException;

	S onLeaveMessage(Transaction txn, S session, LeaveMessage m)
			throws DbException, FormatException;

	S onAbortMessage(Transaction txn, S session, AbortMessage m)
			throws DbException, FormatException;

}
