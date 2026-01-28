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

import org.anonchatsecure.bramble.api.Nameable;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class ConversationRequest<N extends Nameable>
		extends ConversationMessageHeader {

	private final SessionId sessionId;
	private final N nameable;
	@Nullable
	private final String text;
	private final boolean answered;

	public ConversationRequest(MessageId messageId, GroupId groupId,
			long timestamp, boolean local, boolean read, boolean sent,
			boolean seen, SessionId sessionId, N nameable,
			@Nullable String text, boolean answered, long autoDeleteTimer) {
		super(messageId, groupId, timestamp, local, read, sent, seen,
				autoDeleteTimer);
		this.sessionId = sessionId;
		this.nameable = nameable;
		this.text = text;
		this.answered = answered;
	}

	public SessionId getSessionId() {
		return sessionId;
	}

	public N getNameable() {
		return nameable;
	}

	public String getName() {
		return nameable.getName();
	}

	@Nullable
	public String getText() {
		return text;
	}

	public boolean wasAnswered() {
		return answered;
	}

}
