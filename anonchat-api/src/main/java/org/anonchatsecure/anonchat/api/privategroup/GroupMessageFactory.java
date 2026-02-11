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

package org.anonchatsecure.anonchat.api.privategroup;

import org.anonchatsecure.bramble.api.crypto.CryptoExecutor;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager.CLIENT_ID;

@NotNullByDefault
public interface GroupMessageFactory {

	String SIGNING_LABEL_JOIN = CLIENT_ID.getString() + "/JOIN";
	String SIGNING_LABEL_POST = CLIENT_ID.getString() + "/POST";
	String SIGNING_LABEL_AUDIO_POST = CLIENT_ID.getString() + "/AUDIO_POST";

	/**
	 * Creates a join announcement message for the creator of a group.
	 *
	 * @param groupId The ID of the private group that is being joined
	 * @param timestamp The timestamp to be used in the join announcement
	 * @param creator The creator's identity
	 */
	@CryptoExecutor
	GroupMessage createJoinMessage(GroupId groupId, long timestamp,
			LocalAuthor creator);

	/**
	 * Creates a join announcement message for a joining member.
	 *
	 * @param groupId The ID of the private group that is being joined
	 * @param timestamp The timestamp to be used in the join announcement,
	 * which must be greater than the timestamp of the invitation message
	 * @param member The member's identity
	 * @param inviteTimestamp The timestamp of the invitation message
	 * @param creatorSignature The creator's signature from the invitation
	 * message
	 */
	@CryptoExecutor
	GroupMessage createJoinMessage(GroupId groupId, long timestamp,
			LocalAuthor member, long inviteTimestamp, byte[] creatorSignature);

	/**
	 * Creates a private group post.
	 *
	 * @param groupId The ID of the private group
	 * @param timestamp Must be greater than the timestamps of the parent
	 * post, if any, and the member's previous message
	 * @param parentId The ID of the parent post, or null if the post has no
	 * parent
	 * @param author The author of the post
	 * @param text The text of the post
	 * @param previousMsgId The ID of the author's previous message
	 * in this group
	 */
	@CryptoExecutor
	GroupMessage createGroupMessage(GroupId groupId, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author, String text,
			MessageId previousMsgId);

	/**
	 * Creates a private group audio post.
	 *
	 * @param groupId The ID of the private group
	 * @param timestamp Must be greater than the timestamps of the parent
	 * post, if any, and the member's previous message
	 * @param parentId The ID of the parent post, or null if the post has no
	 * parent
	 * @param author The author of the post
	 * @param text The text of the post (may be empty)
	 * @param audioData The audio data bytes
	 * @param contentType The MIME type of the audio data
	 * @param previousMsgId The ID of the author's previous message
	 * in this group
	 */
	@CryptoExecutor
	GroupMessage createGroupAudioMessage(GroupId groupId, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author, String text,
			byte[] audioData, String contentType, MessageId previousMsgId);

}
