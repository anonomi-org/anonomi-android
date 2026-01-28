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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_INVITE_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_IS_SESSION;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LAST_LOCAL_MESSAGE_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LAST_REMOTE_MESSAGE_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LOCAL_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_PRIVATE_GROUP_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_ROLE;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_SESSION_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_STATE;
import static org.anonchatsecure.anonchat.privategroup.invitation.Role.CREATOR;
import static org.anonchatsecure.anonchat.privategroup.invitation.Role.INVITEE;
import static org.anonchatsecure.anonchat.privategroup.invitation.Role.PEER;

@Immutable
@NotNullByDefault
class SessionParserImpl implements SessionParser {

	@Inject
	SessionParserImpl() {
	}

	@Override
	public BdfDictionary getSessionQuery(SessionId s) {
		return BdfDictionary.of(new BdfEntry(SESSION_KEY_SESSION_ID, s));
	}

	@Override
	public BdfDictionary getAllSessionsQuery() {
		return BdfDictionary.of(new BdfEntry(SESSION_KEY_IS_SESSION, true));
	}

	@Override
	public Role getRole(BdfDictionary d) throws FormatException {
		return Role.fromValue(d.getInt(SESSION_KEY_ROLE));
	}

	@Override
	public boolean isSession(BdfDictionary d) throws FormatException {
		return d.getBoolean(SESSION_KEY_IS_SESSION, false);
	}

	@Override
	public Session parseSession(GroupId contactGroupId, BdfDictionary d)
			throws FormatException {
		Session session;
		Role role = getRole(d);
		if (role == CREATOR) {
			session = parseCreatorSession(contactGroupId, d);
		} else if (role == INVITEE) {
			session = parseInviteeSession(contactGroupId, d);
		} else if (role == PEER) {
			session = parsePeerSession(contactGroupId, d);
		} else throw new AssertionError("unknown role");
		return session;
	}

	@Override
	public CreatorSession parseCreatorSession(GroupId contactGroupId,
			BdfDictionary d) throws FormatException {
		if (getRole(d) != CREATOR) {
			throw new IllegalArgumentException(
					"Expected creator, but found " + getRole(d).name());
		}
		return new CreatorSession(contactGroupId, getPrivateGroupId(d),
				getLastLocalMessageId(d), getLastRemoteMessageId(d),
				getLocalTimestamp(d), getInviteTimestamp(d),
				CreatorState.fromValue(getState(d)));
	}

	@Override
	public InviteeSession parseInviteeSession(GroupId contactGroupId,
			BdfDictionary d) throws FormatException {
		if (getRole(d) != INVITEE) throw new IllegalArgumentException();
		return new InviteeSession(contactGroupId, getPrivateGroupId(d),
				getLastLocalMessageId(d), getLastRemoteMessageId(d),
				getLocalTimestamp(d), getInviteTimestamp(d),
				InviteeState.fromValue(getState(d)));
	}

	@Override
	public PeerSession parsePeerSession(GroupId contactGroupId,
			BdfDictionary d) throws FormatException {
		if (getRole(d) != PEER) throw new IllegalArgumentException();
		return new PeerSession(contactGroupId, getPrivateGroupId(d),
				getLastLocalMessageId(d), getLastRemoteMessageId(d),
				getLocalTimestamp(d), PeerState.fromValue(getState(d)));
	}

	private int getState(BdfDictionary d) throws FormatException {
		return d.getInt(SESSION_KEY_STATE);
	}

	private GroupId getPrivateGroupId(BdfDictionary d) throws FormatException {
		return new GroupId(d.getRaw(SESSION_KEY_PRIVATE_GROUP_ID));
	}

	@Nullable
	private MessageId getLastLocalMessageId(BdfDictionary d)
			throws FormatException {
		byte[] b = d.getOptionalRaw(SESSION_KEY_LAST_LOCAL_MESSAGE_ID);
		return b == null ? null : new MessageId(b);
	}

	@Nullable
	private MessageId getLastRemoteMessageId(BdfDictionary d)
			throws FormatException {
		byte[] b = d.getOptionalRaw(SESSION_KEY_LAST_REMOTE_MESSAGE_ID);
		return b == null ? null : new MessageId(b);
	}

	private long getLocalTimestamp(BdfDictionary d) throws FormatException {
		return d.getLong(SESSION_KEY_LOCAL_TIMESTAMP);
	}

	private long getInviteTimestamp(BdfDictionary d) throws FormatException {
		return d.getLong(SESSION_KEY_INVITE_TIMESTAMP);
	}
}
