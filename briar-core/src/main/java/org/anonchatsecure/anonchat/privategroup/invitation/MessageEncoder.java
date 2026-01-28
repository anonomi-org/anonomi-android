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
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

@NotNullByDefault
interface MessageEncoder {

	BdfDictionary encodeMetadata(MessageType type, GroupId privateGroupId,
			long timestamp, boolean local, boolean read, boolean visible,
			boolean available, boolean accepted, long autoDeleteTimer,
			boolean isAutoDecline);

	BdfDictionary encodeMetadata(MessageType type, GroupId privateGroupId,
			long timestamp, long autoDeleteTimer);

	void setVisibleInUi(BdfDictionary meta, boolean visible);

	void setAvailableToAnswer(BdfDictionary meta, boolean available);

	void setInvitationAccepted(BdfDictionary meta, boolean accepted);

	/**
	 * Encodes an invite message without an auto-delete timer.
	 */
	Message encodeInviteMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, String groupName, Author creator, byte[] salt,
			@Nullable String text, byte[] signature);

	/**
	 * Encodes an invite message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeInviteMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, String groupName, Author creator, byte[] salt,
			@Nullable String text, byte[] signature, long autoDeleteTimer);

	/**
	 * Encodes a join message without an auto-delete timer.
	 */
	Message encodeJoinMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, @Nullable MessageId previousMessageId);

	/**
	 * Encodes a join message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeJoinMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, @Nullable MessageId previousMessageId,
			long autoDeleteTimer);

	/**
	 * Encodes a leave message without an auto-delete timer.
	 */
	Message encodeLeaveMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, @Nullable MessageId previousMessageId);

	/**
	 * Encodes a leave message with an optional auto-delete timer. This
	 * requires the contact to support client version 0.1 or higher.
	 */
	Message encodeLeaveMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp, @Nullable MessageId previousMessageId,
			long autoDeleteTimer);

	Message encodeAbortMessage(GroupId contactGroupId, GroupId privateGroupId,
			long timestamp);
}
