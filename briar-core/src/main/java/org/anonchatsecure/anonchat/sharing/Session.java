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
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.sharing.State.START;

@Immutable
@NotNullByDefault
class Session {

	private final State state;
	private final GroupId contactGroupId, shareableId;
	@Nullable
	private final MessageId lastLocalMessageId, lastRemoteMessageId;
	private final long localTimestamp, inviteTimestamp;

	Session(State state, GroupId contactGroupId, GroupId shareableId,
			@Nullable MessageId lastLocalMessageId,
			@Nullable MessageId lastRemoteMessageId, long localTimestamp,
			long inviteTimestamp) {
		this.state = state;
		this.contactGroupId = contactGroupId;
		this.shareableId = shareableId;
		this.lastLocalMessageId = lastLocalMessageId;
		this.lastRemoteMessageId = lastRemoteMessageId;
		this.localTimestamp = localTimestamp;
		this.inviteTimestamp = inviteTimestamp;
	}

	Session(GroupId contactGroupId, GroupId shareableId) {
		this(START, contactGroupId, shareableId, null, null, 0, 0);
	}

	public State getState() {
		return state;
	}

	GroupId getContactGroupId() {
		return contactGroupId;
	}

	GroupId getShareableId() {
		return shareableId;
	}

	@Nullable
	MessageId getLastLocalMessageId() {
		return lastLocalMessageId;
	}

	@Nullable
	MessageId getLastRemoteMessageId() {
		return lastRemoteMessageId;
	}

	long getLocalTimestamp() {
		return localTimestamp;
	}

	long getInviteTimestamp() {
		return inviteTimestamp;
	}

}
