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
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.ValidatorTestCase;
import org.anonchatsecure.anonchat.api.privategroup.MessageType;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.jmock.Expectations;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_JOIN;
import static org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory.SIGNING_LABEL_POST;
import static org.anonchatsecure.anonchat.api.privategroup.MessageType.JOIN;
import static org.anonchatsecure.anonchat.api.privategroup.MessageType.POST;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.GROUP_SALT_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_NAME_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupConstants.MAX_GROUP_POST_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory.SIGNING_LABEL_INVITE;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_INITIAL_JOIN_MSG;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_MEMBER;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_PARENT_MSG_ID;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_PREVIOUS_MSG_ID;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_READ;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.privategroup.GroupConstants.KEY_TYPE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupMessageValidatorTest extends ValidatorTestCase {

	private final PrivateGroupFactory privateGroupFactory =
			context.mock(PrivateGroupFactory.class);
	private final GroupInvitationFactory groupInvitationFactory =
			context.mock(GroupInvitationFactory.class);

	private final Author member = getAuthor();
	private final BdfList memberList = BdfList.of(
			member.getFormatVersion(),
			member.getName(),
			member.getPublicKey()
	);
	private final byte[] memberSignature = getRandomBytes(MAX_SIGNATURE_LENGTH);
	private final Author creator = getAuthor();
	private final BdfList creatorList = BdfList.of(
			creator.getFormatVersion(),
			creator.getName(),
			creator.getPublicKey()
	);
	private final byte[] creatorSignature =
			getRandomBytes(MAX_SIGNATURE_LENGTH);
	private final long inviteTimestamp = message.getTimestamp() - 1;
	private final BdfList invite =
			BdfList.of(inviteTimestamp, creatorSignature);
	private final PrivateGroup privateGroup = new PrivateGroup(group,
			getRandomString(MAX_GROUP_NAME_LENGTH), creator,
			getRandomBytes(GROUP_SALT_LENGTH));
	private final BdfList token = new BdfList();
	private final MessageId parentId = new MessageId(getRandomId());
	private final MessageId previousMsgId = new MessageId(getRandomId());
	private final String text = getRandomString(MAX_GROUP_POST_TEXT_LENGTH);

	private final GroupMessageValidator validator =
			new GroupMessageValidator(privateGroupFactory, clientHelper,
					metadataEncoder, clock, groupInvitationFactory);

	// JOIN message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortJoinMessage() throws Exception {
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongJoinMessage() throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				memberSignature, "");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinWithNullAuthor() throws Exception {
		BdfList body = BdfList.of(JOIN.getInt(), null, invite, memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinWithNonListAuthor() throws Exception {
		BdfList body = BdfList.of(JOIN.getInt(), 123, invite, memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinWithInvalidAuthor() throws Exception {
		expectRejectAuthor(memberList);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsJoinWithNonListInvitation() throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, "not a list",
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsCreatorJoinWithTooShortMemberSignature()
			throws Exception {
		expectParseAuthor(creatorList, creator);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, invite,
				new byte[0]);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsCreatorJoinWithTooLongMemberSignature()
			throws Exception {
		expectParseAuthor(creatorList, creator);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, invite,
				getRandomBytes(MAX_SIGNATURE_LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsCreatorJoinWithNullMemberSignature()
			throws Exception {
		expectParseAuthor(creatorList, creator);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, invite, null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsCreatorJoinWithNonRawMemberSignature()
			throws Exception {
		expectParseAuthor(creatorList, creator);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, invite,
				"not raw");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsCreatorJoinWithInvalidMemberSignature()
			throws Exception {
		expectCreatorJoinMessage(false);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, null,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsCreatorJoin() throws Exception {
		expectCreatorJoinMessage(true);
		BdfList body = BdfList.of(JOIN.getInt(), creatorList, null,
				memberSignature);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertExpectedMessageContext(messageContext, JOIN, creatorList,
				Collections.emptyList());
		assertTrue(messageContext.getDictionary()
				.getBoolean(KEY_INITIAL_JOIN_MSG));
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNullInvitation() throws Exception {
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, null,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooShortInvitation() throws Exception {
		BdfList invalidInvite = BdfList.of(inviteTimestamp);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooLongInvitation() throws Exception {
		BdfList invalidInvite =
				BdfList.of(inviteTimestamp, creatorSignature, "");
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithEqualInvitationTime()
			throws Exception {
		BdfList invalidInvite =
				BdfList.of(message.getTimestamp(), creatorSignature);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithLaterInvitationTime()
			throws Exception {
		BdfList invalidInvite =
				BdfList.of(message.getTimestamp() + 1, creatorSignature);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNullInvitationTime() throws Exception {
		BdfList invalidInvite = BdfList.of(null, creatorSignature);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNonLongInvitationTime()
			throws Exception {
		BdfList invalidInvite = BdfList.of("not long", creatorSignature);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooShortCreatorSignature()
			throws Exception {
		BdfList invalidInvite = BdfList.of(inviteTimestamp, new byte[0]);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooLongCreatorSignature()
			throws Exception {
		BdfList invalidInvite = BdfList.of(inviteTimestamp,
				getRandomBytes(MAX_SIGNATURE_LENGTH + 1));
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNullCreatorSignature()
			throws Exception {
		BdfList invalidInvite = BdfList.of(inviteTimestamp, null);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNonRawCreatorSignature()
			throws Exception {
		BdfList invalidInvite = BdfList.of(inviteTimestamp, "not raw");
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invalidInvite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithInvalidCreatorSignature()
			throws Exception {
		expectMemberJoinMessage(false, true);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooShortMemberSignature()
			throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				new byte[0]);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithTooLongMemberSignature()
			throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				getRandomBytes(MAX_SIGNATURE_LENGTH + 1));
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNullMemberSignature()
			throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite, null);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithNonRawMemberSignature()
			throws Exception {
		expectParseAuthor(memberList, member);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite, "not raw");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsMemberJoinWithInvalidMemberSignature()
			throws Exception {
		expectMemberJoinMessage(true, false);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsMemberJoin() throws Exception {
		expectMemberJoinMessage(true, true);
		BdfList body = BdfList.of(JOIN.getInt(), memberList, invite,
				memberSignature);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertExpectedMessageContext(messageContext, JOIN, memberList,
				Collections.emptyList());
		assertFalse(messageContext.getDictionary()
				.getBoolean(KEY_INITIAL_JOIN_MSG));
	}

	private void expectRejectAuthor(BdfList authorList) throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(throwException(new FormatException()));
		}});
	}

	private void expectParsePrivateGroup() throws Exception {
		context.checking(new Expectations() {{
			oneOf(privateGroupFactory).parsePrivateGroup(group);
			will(returnValue(privateGroup));
		}});
	}

	private void expectCreatorJoinMessage(boolean memberSigValid)
			throws Exception {
		BdfList signed = BdfList.of(
				group.getId(),
				message.getTimestamp(),
				creatorList,
				null
		);
		expectParseAuthor(creatorList, creator);
		expectParsePrivateGroup();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(memberSignature,
					SIGNING_LABEL_JOIN, signed, creator.getPublicKey());
			if (!memberSigValid)
				will(throwException(new GeneralSecurityException()));
		}});
	}

	private void expectMemberJoinMessage(boolean creatorSigValid,
			boolean memberSigValid) throws Exception {
		BdfList signed = BdfList.of(
				group.getId(),
				message.getTimestamp(),
				memberList,
				invite
		);
		expectParseAuthor(memberList, member);
		expectParsePrivateGroup();
		context.checking(new Expectations() {{
			oneOf(groupInvitationFactory).createInviteToken(creator.getId(),
					member.getId(), privateGroup.getId(), inviteTimestamp);
			will(returnValue(token));
			oneOf(clientHelper).verifySignature(creatorSignature,
					SIGNING_LABEL_INVITE, token, creator.getPublicKey());
			if (!creatorSigValid) {
				will(throwException(new GeneralSecurityException()));
			} else {
				oneOf(clientHelper).verifySignature(memberSignature,
						SIGNING_LABEL_JOIN, signed, member.getPublicKey());
				if (!memberSigValid)
					will(throwException(new GeneralSecurityException()));
			}
		}});
	}

	// POST Message

	@Test(expected = FormatException.class)
	public void testRejectsTooShortPost() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongPost() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, memberSignature, "");
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNullAuthor() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), null, parentId, previousMsgId,
				text, memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNonListAuthor() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), 123, parentId, previousMsgId,
				text, memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithInvalidAuthor() throws Exception {
		expectRejectAuthor(memberList);
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, memberSignature);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooShortParentId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList,
				getRandomBytes(MessageId.LENGTH - 1), previousMsgId, text,
				memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooLongParentId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList,
				getRandomBytes(MessageId.LENGTH + 1), previousMsgId, text,
				memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNonRawParentId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, "not raw",
				previousMsgId, text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooShortPreviousMsgId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				getRandomBytes(MessageId.LENGTH - 1), text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooLongPreviousMsgId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				getRandomBytes(MessageId.LENGTH + 1), text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNullPreviousMsgId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId, null,
				text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNonRawPreviousMsgId() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				"not raw", text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooShortText() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, "", memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooLongText() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, getRandomString(MAX_GROUP_POST_TEXT_LENGTH + 1),
				memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNullText() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, null, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNonStringText() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, getRandomBytes(5), memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooShortSignature() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, new byte[0]);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithTooLongSignature() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text,
				getRandomBytes(MAX_SIGNATURE_LENGTH + 1));
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNullSignature() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, null);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithNonRawSignature() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, "not raw");
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}

	@Test(expected = FormatException.class)
	public void testRejectsPostWithInvalidSignature() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, memberSignature);
		expectPostMessage(parentId, false);
		validator.validateMessage(message, group, body);
	}

	@Test
	public void testAcceptsPost() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, parentId,
				previousMsgId, text, memberSignature);
		expectPostMessage(parentId, true);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertExpectedMessageContext(messageContext, POST, memberList,
				Arrays.asList(parentId, previousMsgId));
		assertArrayEquals(previousMsgId.getBytes(),
				messageContext.getDictionary().getRaw(KEY_PREVIOUS_MSG_ID));
		assertArrayEquals(parentId.getBytes(),
				messageContext.getDictionary().getRaw(KEY_PARENT_MSG_ID));
	}

	@Test
	public void testAcceptsTopLevelPost() throws Exception {
		BdfList body = BdfList.of(POST.getInt(), memberList, null,
				previousMsgId, text, memberSignature);
		expectPostMessage(null, true);
		BdfMessageContext messageContext =
				validator.validateMessage(message, group, body);
		assertExpectedMessageContext(messageContext, POST, memberList,
				Collections.singletonList(previousMsgId));
		assertArrayEquals(previousMsgId.getBytes(),
				messageContext.getDictionary().getRaw(KEY_PREVIOUS_MSG_ID));
		assertFalse(
				messageContext.getDictionary().containsKey(KEY_PARENT_MSG_ID));
	}

	private void expectPostMessage(MessageId parentId, boolean sigValid)
			throws Exception {
		BdfList signed = BdfList.of(
				group.getId(),
				message.getTimestamp(),
				memberList,
				parentId == null ? null : parentId.getBytes(),
				previousMsgId.getBytes(),
				text
		);
		expectParseAuthor(memberList, member);
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(memberSignature,
					SIGNING_LABEL_POST, signed, member.getPublicKey());
			if (!sigValid)
				will(throwException(new GeneralSecurityException()));
		}});
	}

	private void assertExpectedMessageContext(BdfMessageContext c,
			MessageType type, BdfList member,
			Collection<MessageId> dependencies) throws FormatException {
		BdfDictionary d = c.getDictionary();
		assertEquals(type.getInt(), d.getInt(KEY_TYPE).intValue());
		assertEquals(message.getTimestamp(),
				d.getLong(KEY_TIMESTAMP).longValue());
		assertFalse(d.getBoolean(KEY_READ));
		assertEquals(member, d.getList(KEY_MEMBER));
		assertEquals(dependencies, c.getDependencies());
	}

	@Test(expected = InvalidMessageException.class)
	public void testRejectsMessageWithUnknownType() throws Exception {
		BdfList body = BdfList.of(POST.getInt() + 1, memberList,
				parentId, previousMsgId, text, memberSignature);
		expectParseAuthor(memberList, member);
		validator.validateMessage(message, group, body);
	}
}
