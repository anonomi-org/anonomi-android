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

package org.anonchatsecure.anonchat.introduction;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
interface ProtocolEngine<S extends Session<?>> {

	S onRequestAction(Transaction txn, S session, @Nullable String text)
			throws DbException;

	S onAcceptAction(Transaction txn, S session) throws DbException;

	/**
	 * Declines an introduction.
	 *
	 * @param isAutoDecline true if automatically declined due to deletion
	 * and false if initiated by the user.
	 */
	S onDeclineAction(Transaction txn, S session, boolean isAutoDecline)
			throws DbException;

	S onRequestMessage(Transaction txn, S session, RequestMessage m)
			throws DbException, FormatException;

	S onAcceptMessage(Transaction txn, S session, AcceptMessage m)
			throws DbException, FormatException;

	S onDeclineMessage(Transaction txn, S session, DeclineMessage m)
			throws DbException, FormatException;

	S onAuthMessage(Transaction txn, S session, AuthMessage m)
			throws DbException, FormatException;

	S onActivateMessage(Transaction txn, S session, ActivateMessage m)
			throws DbException, FormatException;

	S onAbortMessage(Transaction txn, S session, AbortMessage m)
			throws DbException, FormatException;

}
