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

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.privategroup.invitation.PeerState.START;
import static org.anonchatsecure.anonchat.privategroup.invitation.Role.PEER;

@Immutable
@NotNullByDefault
class PeerSession extends Session<PeerState> {

	private final PeerState state;

	PeerSession(GroupId contactGroupId, GroupId privateGroupId,
			@Nullable MessageId lastLocalMessageId,
			@Nullable MessageId lastRemoteMessageId, long localTimestamp,
			PeerState state) {
		super(contactGroupId, privateGroupId, lastLocalMessageId,
				lastRemoteMessageId, localTimestamp, 0);
		this.state = state;
	}

	PeerSession(GroupId contactGroupId, GroupId privateGroupId) {
		this(contactGroupId, privateGroupId, null, null, 0, START);
	}

	@Override
	Role getRole() {
		return PEER;
	}

	@Override
	PeerState getState() {
		return state;
	}
}
