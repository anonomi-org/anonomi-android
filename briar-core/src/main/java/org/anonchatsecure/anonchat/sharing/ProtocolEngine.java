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
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.anonchat.api.sharing.Shareable;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
interface ProtocolEngine<S extends Shareable> {

	Session onInviteAction(Transaction txn, Session session,
			@Nullable String text) throws DbException;

	Session onAcceptAction(Transaction txn, Session session) throws DbException;

	Session onDeclineAction(Transaction txn, Session session,
			boolean isAutoDecline) throws DbException;

	Session onLeaveAction(Transaction txn, Session session) throws DbException;

	Session onInviteMessage(Transaction txn, Session session,
			InviteMessage<S> m) throws DbException, FormatException;

	Session onAcceptMessage(Transaction txn, Session session, AcceptMessage m)
			throws DbException, FormatException;

	Session onDeclineMessage(Transaction txn, Session session, DeclineMessage m)
			throws DbException, FormatException;

	Session onLeaveMessage(Transaction txn, Session session, LeaveMessage m)
			throws DbException, FormatException;

	Session onAbortMessage(Transaction txn, Session session, AbortMessage m)
			throws DbException, FormatException;

}
