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

package org.anonchatsecure.anonchat.privategroup;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
import org.anonchatsecure.bramble.api.client.BdfMessageValidator;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkLength;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkSize;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_JOIN;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_AUDIO_POST;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_IMAGE_POST;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_POST;
import static org.anonchatsecure.anonchat.api.privategroup.MessageType.JOIN;
import static org.anonchatsecure.anonchat.api.privategroup.MessageType.POST;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_AUDIO_SIZE;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_IMAGE_SIZE;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_POST_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory.SIGNING_LABEL_INVITE;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_HAS_AUDIO;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_HAS_IMAGE;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_INITIAL_JOIN_MSG;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_MEMBER;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_PARENT_MSG_ID;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_PREVIOUS_MSG_ID;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_READ;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_TYPE;

@Immutable
@NotNullByDefault
class GroupMessageValidator extends BdfMessageValidator {

	private final PrivateGroupFactory privateGroupFactory;
	private final GroupInvitationFactory groupInvitationFactory;

	GroupMessageValidator(PrivateGroupFactory privateGroupFactory,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, GroupInvitationFactory groupInvitationFactory) {
		super(clientHelper, metadataEncoder, clock);
		this.privateGroupFactory = privateGroupFactory;
		this.groupInvitationFactory = groupInvitationFactory;
	}

	@Override
	protected BdfMessageContext validateMessage(Message m, Group g,
			BdfList body) throws InvalidMessageException, FormatException {

		checkSize(body, 4, 8);

		// Message type (int)
		int type = body.getInt(0);

		// Member (author)
		BdfList memberList = body.getList(1);
		Author member = clientHelper.parseAndValidateAuthor(memberList);

		BdfMessageContext c;
		if (type == JOIN.getInt()) {
			c = validateJoin(m, g, body, member);
			addMessageMetadata(c, memberList, m.getTimestamp());
		} else if (type == POST.getInt()) {
			c = validatePost(m, g, body, member);
			addMessageMetadata(c, memberList, m.getTimestamp());
		} else {
			throw new InvalidMessageException("Unknown Message Type");
		}
		c.getDictionary().put(KEY_TYPE, type);
		return c;
	}

	private BdfMessageContext validateJoin(Message m, Group g, BdfList body,
			Author member) throws FormatException {
		// Message type, member, optional invite, member's signature
		checkSize(body, 4);
		BdfList inviteList = body.getOptionalList(2);
		byte[] memberSignature = body.getRaw(3);
		checkLength(memberSignature, 1, MAX_SIGNATURE_LENGTH);

		// Invite is null if the member is the creator of the private group
		PrivateGroup pg = privateGroupFactory.parsePrivateGroup(g);
		Author creator = pg.getCreator();
		boolean isCreator = member.equals(creator);
		if (isCreator) {
			if (inviteList != null) throw new FormatException();
		} else {
			if (inviteList == null) throw new FormatException();
			// Timestamp, creator's signature
			checkSize(inviteList, 2);
			// Join timestamp must be greater than invite timestamp
			long inviteTimestamp = inviteList.getLong(0);
			if (m.getTimestamp() <= inviteTimestamp)
				throw new FormatException();
			byte[] creatorSignature = inviteList.getRaw(1);
			checkLength(creatorSignature, 1, MAX_SIGNATURE_LENGTH);
			// The invite token is signed by the creator of the private group
			BdfList token = groupInvitationFactory.createInviteToken(
					creator.getId(), member.getId(), g.getId(),
					inviteTimestamp);
			try {
				clientHelper.verifySignature(creatorSignature,
						SIGNING_LABEL_INVITE,
						token, creator.getPublicKey());
			} catch (GeneralSecurityException e) {
				throw new FormatException();
			}
		}

		// Verify the member's signature
		BdfList memberList = body.getList(1); // Already validated
		BdfList signed = BdfList.of(
				g.getId(),
				m.getTimestamp(),
				memberList,
				inviteList
		);
		try {
			clientHelper.verifySignature(memberSignature, SIGNING_LABEL_JOIN,
					signed, member.getPublicKey());
		} catch (GeneralSecurityException e) {
			throw new FormatException();
		}

		// Return the metadata and no dependencies
		BdfDictionary meta = new BdfDictionary();
		meta.put(KEY_INITIAL_JOIN_MSG, isCreator);
		return new BdfMessageContext(meta);
	}

	private BdfMessageContext validatePost(Message m, Group g, BdfList body,
			Author member) throws FormatException {
		// Message type, member, optional parent ID, previous message ID,
		// text, signature [, mediaData, contentType]
		boolean hasMedia = body.size() == 8;
		if (!hasMedia) checkSize(body, 6);

		byte[] parentId = body.getOptionalRaw(2);
		checkLength(parentId, MessageId.LENGTH);
		byte[] previousMessageId = body.getRaw(3);
		checkLength(previousMessageId, MessageId.LENGTH);
		String text = body.getString(4);
		checkLength(text, 0, MAX_GROUP_POST_TEXT_LENGTH);
		byte[] signature = body.getRaw(5);
		checkLength(signature, 1, MAX_SIGNATURE_LENGTH);

		byte[] mediaData = null;
		String contentType = null;
		boolean isAudio = false;
		boolean isImage = false;
		if (hasMedia) {
			mediaData = body.getRaw(6);
			contentType = body.getString(7);
			checkLength(contentType, 1, 50);
			isAudio = contentType.startsWith("audio/");
			isImage = contentType.startsWith("image/");
			if (!isAudio && !isImage)
				throw new FormatException();
			if (isAudio) {
				checkLength(mediaData, 1, MAX_GROUP_AUDIO_SIZE);
			} else {
				checkLength(mediaData, 1, MAX_GROUP_IMAGE_SIZE);
			}
		}

		// Verify the member's signature
		BdfList memberList = body.getList(1); // Already validated
		String signingLabel;
		BdfList signed;
		if (hasMedia) {
			signingLabel = isAudio ? SIGNING_LABEL_AUDIO_POST
					: SIGNING_LABEL_IMAGE_POST;
			signed = BdfList.of(
					g.getId(),
					m.getTimestamp(),
					memberList,
					parentId,
					previousMessageId,
					text,
					mediaData,
					contentType
			);
		} else {
			signingLabel = SIGNING_LABEL_POST;
			signed = BdfList.of(
					g.getId(),
					m.getTimestamp(),
					memberList,
					parentId,
					previousMessageId,
					text
			);
		}
		try {
			clientHelper.verifySignature(signature, signingLabel,
					signed, member.getPublicKey());
		} catch (GeneralSecurityException e) {
			throw new FormatException();
		}

		// The parent post, if any, and the member's previous message are
		// dependencies
		Collection<MessageId> dependencies = new ArrayList<>();
		if (parentId != null) dependencies.add(new MessageId(parentId));
		dependencies.add(new MessageId(previousMessageId));

		// Return the metadata and dependencies
		BdfDictionary meta = new BdfDictionary();
		if (parentId != null) meta.put(KEY_PARENT_MSG_ID, parentId);
		meta.put(KEY_PREVIOUS_MSG_ID, previousMessageId);
		if (isAudio) meta.put(KEY_HAS_AUDIO, true);
		if (isImage) meta.put(KEY_HAS_IMAGE, true);
		return new BdfMessageContext(meta, dependencies);
	}

	private void addMessageMetadata(BdfMessageContext c, BdfList member,
			long timestamp) {
		c.getDictionary().put(KEY_MEMBER, member);
		c.getDictionary().put(KEY_TIMESTAMP, timestamp);
		c.getDictionary().put(KEY_READ, false);
	}

}
