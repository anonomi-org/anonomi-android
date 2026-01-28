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

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
abstract class GroupInvitationMessage {

	private final MessageId id;
	private final GroupId contactGroupId, privateGroupId;
	private final long timestamp;

	GroupInvitationMessage(MessageId id, GroupId contactGroupId,
			GroupId privateGroupId, long timestamp) {
		this.id = id;
		this.contactGroupId = contactGroupId;
		this.privateGroupId = privateGroupId;
		this.timestamp = timestamp;
	}

	MessageId getId() {
		return id;
	}

	GroupId getContactGroupId() {
		return contactGroupId;
	}

	GroupId getPrivateGroupId() {
		return privateGroupId;
	}

	long getTimestamp() {
		return timestamp;
	}
}
