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

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class MessageMetadata {

	private final MessageType type;
	private final GroupId shareableId;
	private final long timestamp, autoDeleteTimer;
	private final boolean local, read, visible, available, accepted;
	private final boolean isAutoDecline;

	MessageMetadata(MessageType type, GroupId shareableId, long timestamp,
			boolean local, boolean read, boolean visible, boolean available,
			boolean accepted, long autoDeleteTimer, boolean isAutoDecline) {
		this.shareableId = shareableId;
		this.type = type;
		this.timestamp = timestamp;
		this.local = local;
		this.read = read;
		this.visible = visible;
		this.available = available;
		this.accepted = accepted;
		this.autoDeleteTimer = autoDeleteTimer;
		this.isAutoDecline = isAutoDecline;
	}

	MessageType getMessageType() {
		return type;
	}

	GroupId getShareableId() {
		return shareableId;
	}

	long getTimestamp() {
		return timestamp;
	}

	boolean isLocal() {
		return local;
	}

	boolean isRead() {
		return read;
	}

	boolean isVisibleInConversation() {
		return visible;
	}

	boolean isAvailableToAnswer() {
		return available;
	}

	/**
	 * Returns true if the invitation was accepted.
	 * <p>
	 * Only applies to messages of type INVITE.
	 */
	public boolean wasAccepted() {
		return accepted;
	}

	public long getAutoDeleteTimer() {
		return autoDeleteTimer;
	}

	public boolean isAutoDecline() {
		return isAutoDecline;
	}
}
