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

import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class MessageMetadata {

	private final MessageType type;
	@Nullable
	private final SessionId sessionId;
	private final long timestamp, autoDeleteTimer;
	private final boolean local, read, visible, available, isAutoDecline;

	MessageMetadata(MessageType type, @Nullable SessionId sessionId,
			long timestamp, boolean local, boolean read, boolean visible,
			boolean available, long autoDeleteTimer, boolean isAutoDecline) {
		this.type = type;
		this.sessionId = sessionId;
		this.timestamp = timestamp;
		this.local = local;
		this.read = read;
		this.visible = visible;
		this.available = available;
		this.autoDeleteTimer = autoDeleteTimer;
		this.isAutoDecline = isAutoDecline;
	}

	MessageType getMessageType() {
		return type;
	}

	@Nullable
	public SessionId getSessionId() {
		return sessionId;
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

	public long getAutoDeleteTimer() {
		return autoDeleteTimer;
	}

	public boolean isAutoDecline() {
		return isAutoDecline;
	}
}
