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

interface IntroductionConstants {

	// Message metadata keys
	String MSG_KEY_MESSAGE_TYPE = "messageType";
	String MSG_KEY_SESSION_ID = "sessionId";
	String MSG_KEY_TIMESTAMP = "timestamp";
	String MSG_KEY_LOCAL = "local";
	String MSG_KEY_VISIBLE_IN_UI = "visibleInUi";
	String MSG_KEY_AVAILABLE_TO_ANSWER = "availableToAnswer";
	String MSG_KEY_AUTO_DELETE_TIMER = "autoDeleteTimer";
	String MSG_KEY_IS_AUTO_DECLINE = "isAutoDecline";

	// Session Keys
	String SESSION_KEY_SESSION_ID = "sessionId";
	String SESSION_KEY_ROLE = "role";
	String SESSION_KEY_STATE = "state";
	String SESSION_KEY_REQUEST_TIMESTAMP = "requestTimestamp";
	String SESSION_KEY_LOCAL_TIMESTAMP = "localTimestamp";
	String SESSION_KEY_LAST_LOCAL_MESSAGE_ID = "lastLocalMessageId";
	String SESSION_KEY_LAST_REMOTE_MESSAGE_ID = "lastRemoteMessageId";

	// Session Keys Introducer
	String SESSION_KEY_INTRODUCEE_A = "introduceeA";
	String SESSION_KEY_INTRODUCEE_B = "introduceeB";
	String SESSION_KEY_GROUP_ID = "groupId";
	String SESSION_KEY_AUTHOR = "author";

	// Session Keys Introducee
	String SESSION_KEY_INTRODUCER = "introducer";
	String SESSION_KEY_LOCAL = "local";
	String SESSION_KEY_REMOTE = "remote";

	String SESSION_KEY_MASTER_KEY = "masterKey";
	String SESSION_KEY_TRANSPORT_KEYS = "transportKeys";

	String SESSION_KEY_ALICE = "alice";
	String SESSION_KEY_EPHEMERAL_PUBLIC_KEY = "ephemeralPublicKey";
	String SESSION_KEY_EPHEMERAL_PRIVATE_KEY = "ephemeralPrivateKey";
	String SESSION_KEY_TRANSPORT_PROPERTIES = "transportProperties";
	String SESSION_KEY_ACCEPT_TIMESTAMP = "acceptTimestamp";
	String SESSION_KEY_MAC_KEY = "macKey";

	String SESSION_KEY_REMOTE_AUTHOR = "remoteAuthor";

}
