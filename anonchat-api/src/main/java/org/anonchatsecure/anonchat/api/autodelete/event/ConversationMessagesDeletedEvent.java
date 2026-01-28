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

package org.anonchatsecure.anonchat.api.autodelete.event;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when one or more messages
 * in the private conversation with a contact have been deleted.
 */
@Immutable
@NotNullByDefault
public class ConversationMessagesDeletedEvent extends Event {

	private final ContactId contactId;
	private final Collection<MessageId> messageIds;

	public ConversationMessagesDeletedEvent(ContactId contactId,
			Collection<MessageId> messageIds) {
		this.contactId = contactId;
		this.messageIds = messageIds;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public Collection<MessageId> getMessageIds() {
		return messageIds;
	}
}
