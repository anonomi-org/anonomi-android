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

interface GroupInvitationConstants {

	// Message metadata keys
	String MSG_KEY_MESSAGE_TYPE = "messageType";
	String MSG_KEY_PRIVATE_GROUP_ID = "privateGroupId";
	String MSG_KEY_TIMESTAMP = "timestamp";
	String MSG_KEY_LOCAL = "local";
	String MSG_KEY_VISIBLE_IN_UI = "visibleInUi";
	String MSG_KEY_AVAILABLE_TO_ANSWER = "availableToAnswer";
	String MSG_KEY_INVITATION_ACCEPTED = "invitationAccepted";
	String MSG_KEY_AUTO_DELETE_TIMER = "autoDeleteTimer";
	String MSG_KEY_IS_AUTO_DECLINE = "isAutoDecline";

	// Session keys
	String SESSION_KEY_IS_SESSION = "isSession";
	String SESSION_KEY_SESSION_ID = "sessionId";
	String SESSION_KEY_PRIVATE_GROUP_ID = "privateGroupId";
	String SESSION_KEY_LAST_LOCAL_MESSAGE_ID = "lastLocalMessageId";
	String SESSION_KEY_LAST_REMOTE_MESSAGE_ID = "lastRemoteMessageId";
	String SESSION_KEY_LOCAL_TIMESTAMP = "localTimestamp";
	String SESSION_KEY_INVITE_TIMESTAMP = "inviteTimestamp";
	String SESSION_KEY_ROLE = "role";
	String SESSION_KEY_STATE = "state";
}
