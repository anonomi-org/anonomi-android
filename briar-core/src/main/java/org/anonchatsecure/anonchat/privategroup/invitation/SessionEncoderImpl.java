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

import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.data.BdfDictionary.NULL_VALUE;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_INVITE_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_IS_SESSION;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LAST_LOCAL_MESSAGE_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LAST_REMOTE_MESSAGE_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_LOCAL_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_PRIVATE_GROUP_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_ROLE;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_SESSION_ID;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.SESSION_KEY_STATE;

@Immutable
@NotNullByDefault
class SessionEncoderImpl implements SessionEncoder {

	@Inject
	SessionEncoderImpl() {
	}

	@Override
	public BdfDictionary encodeSession(Session s) {
		BdfDictionary d = new BdfDictionary();
		d.put(SESSION_KEY_IS_SESSION, true);
		d.put(SESSION_KEY_SESSION_ID, s.getPrivateGroupId());
		d.put(SESSION_KEY_PRIVATE_GROUP_ID, s.getPrivateGroupId());
		MessageId lastLocalMessageId = s.getLastLocalMessageId();
		if (lastLocalMessageId == null)
			d.put(SESSION_KEY_LAST_LOCAL_MESSAGE_ID, NULL_VALUE);
		else d.put(SESSION_KEY_LAST_LOCAL_MESSAGE_ID, lastLocalMessageId);
		MessageId lastRemoteMessageId = s.getLastRemoteMessageId();
		if (lastRemoteMessageId == null)
			d.put(SESSION_KEY_LAST_REMOTE_MESSAGE_ID, NULL_VALUE);
		else d.put(SESSION_KEY_LAST_REMOTE_MESSAGE_ID, lastRemoteMessageId);
		d.put(SESSION_KEY_LOCAL_TIMESTAMP, s.getLocalTimestamp());
		d.put(SESSION_KEY_INVITE_TIMESTAMP, s.getInviteTimestamp());
		d.put(SESSION_KEY_ROLE, s.getRole().getValue());
		d.put(SESSION_KEY_STATE, s.getState().getValue());
		return d;
	}
}
