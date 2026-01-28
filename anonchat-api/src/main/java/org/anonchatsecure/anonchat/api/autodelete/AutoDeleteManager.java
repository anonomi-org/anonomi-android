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

package org.anonchatsecure.anonchat.api.autodelete;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.briarproject.nullsafety.NotNullByDefault;

import static java.util.concurrent.TimeUnit.DAYS;

@NotNullByDefault
public interface AutoDeleteManager {

	/**
	 * The unique ID of the auto-delete client.
	 */
	ClientId CLIENT_ID = new ClientId("org.anonchatsecure.anonchat.autodelete");

	/**
	 * The current major version of the auto-delete client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the auto-delete client.
	 */
	int MINOR_VERSION = 0;

	/**
	 * The default auto-delete timer duration.
	 */
	long DEFAULT_TIMER_DURATION = DAYS.toMillis(7);

	/**
	 * Returns the auto-delete timer duration for the given contact. Use
	 * {@link #getAutoDeleteTimer(Transaction, ContactId, long)} if the timer
	 * will be used in an outgoing message.
	 */
	long getAutoDeleteTimer(Transaction txn, ContactId c) throws DbException;

	/**
	 * Returns the auto-delete timer duration for the given contact, for use in
	 * a message with the given timestamp. The timestamp is stored. This method
	 * requires a read-write transaction.
	 */
	long getAutoDeleteTimer(Transaction txn, ContactId c, long timestamp)
			throws DbException;

	/**
	 * Sets the auto-delete timer duration for the given contact.
	 */
	void setAutoDeleteTimer(Transaction txn, ContactId c, long timer)
			throws DbException;

	/**
	 * Receives an auto-delete timer duration from the given contact, carried
	 * in a message with the given timestamp. The local timer is set to the
	 * same duration unless it has been
	 * {@link #setAutoDeleteTimer(Transaction, ContactId, long) changed} more
	 * recently than the remote timer.
	 */
	void receiveAutoDeleteTimer(Transaction txn, ContactId c, long timer,
			long timestamp) throws DbException;
}
