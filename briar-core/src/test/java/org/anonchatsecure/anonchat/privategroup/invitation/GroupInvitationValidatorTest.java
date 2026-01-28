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
import org.anonchatsecure.bramble.api.UniqueId;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.ValidatorTestCase;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.jmock.Expectations;
import org.junit.Test;

import java.security.GeneralSecurityException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MAX_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MIN_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.GROUP_SALT_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_INVITATION_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_NAME_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory.SIGNING_LABEL_INVITE;
import static org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationConstants.MSG_KEY_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.privategroup.invitation.MessageType.ABORT;
import static org.anonchatsecure.anonchat.privategroup.invitation.MessageType.INVITE;
import static org.anonchatsecure.anonchat.privategroup.invitation.MessageType.JOIN;
import static org.anonchatsecure.anonchat.privategroup.invitation.MessageType.LEAVE;
import static org.junit.Assert.assertEquals;

public class GroupInvitationValidatorTest extends ValidatorTestCase {

	private final PrivateGroupFactory privateGroupFactory =
			context.mock(PrivateGroupFactory.class);
	private final MessageEncoder messageEncoder =
			context.mock(MessageEncoder.class);

	private final Author creator = getAuthor();
	private final BdfList creatorList = BdfList.of(
			creator.getFormatVersion(),
			creator.getName(),
			creator.getPublicKey()
	);
	private final String groupName = getRandomString(MAX_GROUP_NAME_LENGTH);
	private final byte[] salt = getRandomBytes(GROUP_SALT_LENGTH);
	private final String text =
			getRandomString(MAX_GROUP_INVITATION_TEXT_LENGTH);
	private final byte[] signature = getRandomBytes(MAX_SIGNATURE_LENGTH);
	private final PrivateGroup privateGroup =
			new PrivateGroup(group, groupName, creator, salt);
	private final BdfDictionary meta = new BdfDictionary();
	private final MessageId previousMessageId = new MessageId(getRandomId());

	private final GroupInvitationValidator validator =
			new GroupInvitationValidator(clientHelper, metadataEncoder,
					clock, privateGroupFactory, messageEncoder);

	// INVITE Message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortInviteMessage() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongInviteMessage() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, NO_AUTO_DELETE_TIMER, null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooShortGroupName()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, "", salt,
				text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooLongGroupName()
			throws Exception {
		String tooLongName = getRandomString(MAX_GROUP_NAME_LENGTH + 1);
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, tooLongName,
				salt, text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNullGroupName() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, null, salt,
				text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonStringGroupName()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList,
				getRandomBytes(5), salt, text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNullCreator() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), null, groupName, salt,
				text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonListCreator() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), 123, groupName, salt,
				text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithInvalidCreator() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(creatorList);
			will(throwException(new FormatException()));
		}});

		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooShortGroupSalt()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				getRandomBytes(GROUP_SALT_LENGTH - 1), text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooLongGroupSalt()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				getRandomBytes(GROUP_SALT_LENGTH + 1), text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNullGroupSalt() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				null, text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonRawGroupSalt() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				"not raw", text, signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooShortText() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, "", signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooLongText() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, getRandomString(MAX_GROUP_INVITATION_TEXT_LENGTH + 1),
				signature);
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsInviteMessageWithNullText() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, null, signature);
		testAcceptsInviteMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonStringText()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, getRandomBytes(5), signature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooShortSignature()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, new byte[0]);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooLongSignature()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, getRandomBytes(MAX_SIGNATURE_LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNullSignature() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonRawSignature() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, "not raw");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithInvalidSignature()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature);
		expectInviteMessage(true);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooBigAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, MAX_AUTO_DELETE_TIMER_MS + 1);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithTooSmallAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, MIN_AUTO_DELETE_TIMER_MS - 1);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInviteMessageWithNonLongAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, "foo");
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsValidInviteMessage() throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature);
		testAcceptsInviteMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsInviteMessageWithNullAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, null);
		testAcceptsInviteMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsInviteMessageWithMinAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, MIN_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MIN_AUTO_DELETE_TIMER_MS);
		testAcceptsInviteMessage(body, MIN_AUTO_DELETE_TIMER_MS, metadata);
	}

	@Test
	public void testAcceptsInviteMessageWithMaxAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(INVITE.getValue(), creatorList, groupName,
				salt, text, signature, MAX_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MAX_AUTO_DELETE_TIMER_MS);
		testAcceptsInviteMessage(body, MAX_AUTO_DELETE_TIMER_MS, metadata);
	}

	private void testAcceptsInviteMessage(BdfList body, long autoDeleteTimer,
			BdfDictionary metadata) throws Exception {
		expectInviteMessage(false);
		expectEncodeMetadata(INVITE, autoDeleteTimer, metadata);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), messageContext.getDependencies());
		assertEquals(metadata, messageContext.getDictionary());
	}

	private void expectInviteMessage(boolean exception) throws Exception {
		BdfList signed = BdfList.of(
				message.getTimestamp(),
				message.getGroupId(),
				privateGroup.getId()
		);
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(creatorList);
			will(returnValue(creator));
			oneOf(privateGroupFactory).createPrivateGroup(groupName, creator,
					salt);
			will(returnValue(privateGroup));
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_INVITE,
					signed, creator.getPublicKey());
			if (exception) {
				will(throwException(new GeneralSecurityException()));
			}
		}});
	}

	// JOIN Message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortJoinMessage() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId());
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongJoinMessage() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				previousMessageId, NO_AUTO_DELETE_TIMER, null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithTooShortGroupId() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(),
				getRandomBytes(GroupId.LENGTH - 1), previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithTooLongGroupId() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(),
				getRandomBytes(GroupId.LENGTH + 1), previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithNullGroupId() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), null, previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithNonRawGroupId() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), "not raw",
				previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithTooShortPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				getRandomBytes(UniqueId.LENGTH - 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithTooLongPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				getRandomBytes(UniqueId.LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinMessageWithNonRawPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				"not raw");
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsJoinMessageWithNullPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(), null);
		expectEncodeMetadata(JOIN, NO_AUTO_DELETE_TIMER, meta);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), messageContext.getDependencies());
		assertEquals(meta, messageContext.getDictionary());
	}

	@Test
	public void testAcceptsValidJoinMessage() throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				previousMessageId);
		testAcceptsJoinMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsJoinMessageWithNullAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				previousMessageId, null);
		testAcceptsJoinMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsJoinMessageWitMinAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				previousMessageId, MIN_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MIN_AUTO_DELETE_TIMER_MS);
		testAcceptsJoinMessage(body, MIN_AUTO_DELETE_TIMER_MS, metadata);
	}

	@Test
	public void testAcceptsJoinMessageWitMaxAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(JOIN.getValue(), privateGroup.getId(),
				previousMessageId, MAX_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MAX_AUTO_DELETE_TIMER_MS);
		testAcceptsJoinMessage(body, MAX_AUTO_DELETE_TIMER_MS, metadata);
	}

	private void testAcceptsJoinMessage(BdfList body, long autoDeleteTimer,
			BdfDictionary metadata) throws Exception {
		expectEncodeMetadata(JOIN, autoDeleteTimer, metadata);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(singletonList(previousMessageId),
				messageContext.getDependencies());
		assertEquals(metadata, messageContext.getDictionary());
	}

	// LEAVE message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortLeaveMessage() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId());
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongLeaveMessage() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				previousMessageId, "");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithTooShortGroupId() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(),
				getRandomBytes(GroupId.LENGTH - 1), previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithTooLongGroupId() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(),
				getRandomBytes(GroupId.LENGTH + 1), previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithNullGroupId() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), null, previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithNonRawGroupId() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), "not raw",
				previousMessageId);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithTooShortPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				getRandomBytes(UniqueId.LENGTH - 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithTooLongPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				getRandomBytes(UniqueId.LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsLeaveMessageWithNonRawPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				"not raw");
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsLeaveMessageWithNullPreviousMessageId()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(), null);
		expectEncodeMetadata(LEAVE, NO_AUTO_DELETE_TIMER, meta);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), messageContext.getDependencies());
		assertEquals(meta, messageContext.getDictionary());
	}

	@Test
	public void testAcceptsValidLeaveMessage() throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				previousMessageId);
		testAcceptsLeaveMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsLeaveMessageWithNullAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				previousMessageId, null);
		testAcceptsLeaveMessage(body, NO_AUTO_DELETE_TIMER, meta);
	}

	@Test
	public void testAcceptsLeaveMessageWithMinAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				previousMessageId, MIN_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MIN_AUTO_DELETE_TIMER_MS);
		testAcceptsLeaveMessage(body, MIN_AUTO_DELETE_TIMER_MS, metadata);
	}

	@Test
	public void testAcceptsLeaveMessageWithMaxAutoDeleteTimer()
			throws Exception {
		BdfList body = BdfList.of(LEAVE.getValue(), privateGroup.getId(),
				previousMessageId, MAX_AUTO_DELETE_TIMER_MS);
		BdfDictionary metadata = new BdfDictionary(meta);
		metadata.put(MSG_KEY_AUTO_DELETE_TIMER, MAX_AUTO_DELETE_TIMER_MS);
		testAcceptsLeaveMessage(body, MAX_AUTO_DELETE_TIMER_MS, metadata);
	}

	private void testAcceptsLeaveMessage(BdfList body, long autoDeleteTimer,
			BdfDictionary metadata) throws Exception {
		expectEncodeMetadata(LEAVE, autoDeleteTimer, metadata);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(singletonList(previousMessageId),
				messageContext.getDependencies());
		assertEquals(metadata, messageContext.getDictionary());
	}

	// ABORT message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortAbortMessage() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue());
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongAbortMessage() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(), privateGroup.getId(), "");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsAbortMessageWithTooShortGroupId() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(),
				getRandomBytes(GroupId.LENGTH - 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsAbortMessageWithTooLongGroupId() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(),
				getRandomBytes(GroupId.LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsAbortMessageWithNullGroupId() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(), null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsAbortMessageWithNonRawGroupId() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(), "not raw");
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsValidAbortMessage() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue(), privateGroup.getId());
		expectEncodeMetadata(ABORT, NO_AUTO_DELETE_TIMER, meta);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertEquals(emptyList(), messageContext.getDependencies());
		assertEquals(meta, messageContext.getDictionary());
	}

	@Test(expected = FormatException.class)
	public void testRejectsMessageWithUnknownType() throws Exception {
		BdfList body = BdfList.of(ABORT.getValue() + 1);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsEmptyMessage() throws Exception {
		BdfList body = new BdfList();
		validator.validateMessage(message, group, body);
	}

	private void expectEncodeMetadata(MessageType type,
			long autoDeleteTimer, BdfDictionary metadata) {
		context.checking(new Expectations() {{
			oneOf(messageEncoder).encodeMetadata(type, message.getGroupId(),
					message.getTimestamp(), autoDeleteTimer);
			will(returnValue(metadata));
		}});
	}
}
