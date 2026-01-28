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

package org.anonchatsecure.anonchat.api.privategroup.event;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.privategroup.Visibility;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class ContactRelationshipRevealedEvent extends Event {

	private final GroupId groupId;
	private final AuthorId memberId;
	private final ContactId contactId;
	private final Visibility visibility;

	public ContactRelationshipRevealedEvent(GroupId groupId, AuthorId memberId,
			ContactId contactId, Visibility visibility) {
		this.groupId = groupId;
		this.memberId = memberId;
		this.contactId = contactId;
		this.visibility = visibility;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public AuthorId getMemberId() {
		return memberId;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public Visibility getVisibility() {
		return visibility;
	}

}
