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

package org.anonchatsecure.anonchat.api.introduction;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager.ConversationClient;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
public interface IntroductionManager extends ConversationClient {

	/**
	 * The unique ID of the introduction client.
	 */
	ClientId CLIENT_ID = new ClientId("org.anonchatsecure.anonchat.introduction");

	/**
	 * The current major version of the introduction client.
	 */
	int MAJOR_VERSION = 1;

	/**
	 * Returns true if both contacts can be introduced at this moment.
	 */
	boolean canIntroduce(Contact c1, Contact c2) throws DbException;

	/**
	 * Returns true if both contacts can be introduced at this moment.
	 */
	boolean canIntroduce(Transaction txn, Contact c1, Contact c2)
			throws DbException;

	/**
	 * The current minor version of the introduction client.
	 */
	int MINOR_VERSION = 1;

	/**
	 * Sends two initial introduction messages.
	 */
	void makeIntroduction(Contact c1, Contact c2, @Nullable String text)
			throws DbException;

	/**
	 * Sends two initial introduction messages.
	 */
	void makeIntroduction(Transaction txn, Contact c1, Contact c2,
			@Nullable String text) throws DbException;

	/**
	 * Responds to an introduction.
	 */
	void respondToIntroduction(ContactId contactId, SessionId sessionId,
			boolean accept) throws DbException;

	/**
	 * Responds to an introduction.
	 */
	void respondToIntroduction(Transaction txn, ContactId contactId,
			SessionId sessionId, boolean accept) throws DbException;

}
