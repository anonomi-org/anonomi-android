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

package org.anonchatsecure.anonchat.api.conversation;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class ConversationResponse extends ConversationMessageHeader {

	private final SessionId sessionId;
	private final boolean accepted, isAutoDecline;

	public ConversationResponse(MessageId id, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, boolean accepted, long autoDeleteTimer,
			boolean isAutoDecline) {
		super(id, groupId, time, local, read, sent, seen, autoDeleteTimer);
		this.sessionId = sessionId;
		this.accepted = accepted;
		this.isAutoDecline = isAutoDecline;
	}

	public SessionId getSessionId() {
		return sessionId;
	}

	public boolean wasAccepted() {
		return accepted;
	}

	public boolean isAutoDecline() {
		return isAutoDecline;
	}
}
