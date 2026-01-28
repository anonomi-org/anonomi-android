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

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.introduction.Role;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.api.introduction.Role.INTRODUCER;

@Immutable
@NotNullByDefault
class IntroducerSession extends Session<IntroducerState> {

	private final Introducee introduceeA, introduceeB;

	IntroducerSession(SessionId sessionId, IntroducerState state,
			long requestTimestamp, Introducee introduceeA,
			Introducee introduceeB) {
		super(sessionId, state, requestTimestamp);
		this.introduceeA = introduceeA;
		this.introduceeB = introduceeB;
	}

	IntroducerSession(SessionId sessionId, GroupId groupIdA, Author authorA,
			GroupId groupIdB, Author authorB) {
		this(sessionId, IntroducerState.START, -1,
				new Introducee(sessionId, groupIdA, authorA),
				new Introducee(sessionId, groupIdB, authorB));
	}

	@Override
	Role getRole() {
		return INTRODUCER;
	}

	Introducee getIntroduceeA() {
		return introduceeA;
	}

	Introducee getIntroduceeB() {
		return introduceeB;
	}

	@Immutable
	@NotNullByDefault
	static class Introducee implements PeerSession {
		final SessionId sessionId;
		final GroupId groupId;
		final Author author;
		final long localTimestamp;
		@Nullable
		final MessageId lastLocalMessageId, lastRemoteMessageId;

		Introducee(SessionId sessionId, GroupId groupId, Author author,
				long localTimestamp,
				@Nullable MessageId lastLocalMessageId,
				@Nullable MessageId lastRemoteMessageId) {
			this.sessionId = sessionId;
			this.groupId = groupId;
			this.localTimestamp = localTimestamp;
			this.author = author;
			this.lastLocalMessageId = lastLocalMessageId;
			this.lastRemoteMessageId = lastRemoteMessageId;
		}

		Introducee(Introducee i, Message sent) {
			this(i.sessionId, i.groupId, i.author, sent.getTimestamp(),
					sent.getId(), i.lastRemoteMessageId);
		}

		Introducee(Introducee i, MessageId remoteMessageId) {
			this(i.sessionId, i.groupId, i.author, i.localTimestamp,
					i.lastLocalMessageId, remoteMessageId);
		}

		Introducee(SessionId sessionId, GroupId groupId,
				Author author) {
			this(sessionId, groupId, author, -1, null, null);
		}

		@Override
		public SessionId getSessionId() {
			return sessionId;
		}

		@Override
		public GroupId getContactGroupId() {
			return groupId;
		}

		@Override
		public long getLocalTimestamp() {
			return localTimestamp;
		}

		@Nullable
		@Override
		public MessageId getLastLocalMessageId() {
			return lastLocalMessageId;
		}

		@Nullable
		@Override
		public MessageId getLastRemoteMessageId() {
			return lastRemoteMessageId;
		}

	}

}
