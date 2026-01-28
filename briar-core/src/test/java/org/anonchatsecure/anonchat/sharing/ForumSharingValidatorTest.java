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

package org.anonchatsecure.anonchat.sharing;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.jmock.Expectations;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MAX_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MIN_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.FORUM_SALT_LENGTH;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_NAME_LENGTH;
import static org.anonchatsecure.anonchat.api.sharing.SharingConstants.MAX_INVITATION_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.sharing.MessageType.INVITE;
import static org.junit.Assert.fail;

public class ForumSharingValidatorTest extends SharingValidatorTest {

	private final String forumName = getRandomString(MAX_FORUM_NAME_LENGTH);
	private final byte[] salt = getRandomBytes(FORUM_SALT_LENGTH);
	private final Forum forum = new Forum(group, forumName, salt);
	private final BdfList descriptor = BdfList.of(forumName, salt);
	private final String text = getRandomString(MAX_INVITATION_TEXT_LENGTH);

	@Override
	SharingValidator getValidator() {
		return new ForumSharingValidator(messageEncoder, clientHelper,
				metadataEncoder, clock, forumFactory);
	}

	@Test
	public void testAcceptsInvitationWithText() throws Exception {
		expectCreateForum(forumName);
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, text));
		assertExpectedContext(context, previousMsgId);
	}

	@Test
	public void testAcceptsInvitationWithNullText() throws Exception {
		expectCreateForum(forumName);
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, null));
		assertExpectedContext(context, previousMsgId);
	}

	@Test
	public void testAcceptsInvitationWithNullPreviousMsgId() throws Exception {
		expectCreateForum(forumName);
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), null, descriptor, null));
		assertExpectedContext(context, null);
	}

	@Test
	public void testAcceptsInvitationWithMinAutoDeleteTimer() throws Exception {
		testAcceptsInvitationWithAutoDeleteTimer(MIN_AUTO_DELETE_TIMER_MS);
	}

	@Test
	public void testAcceptsInvitationWithMaxAutoDeleteTimer() throws Exception {
		testAcceptsInvitationWithAutoDeleteTimer(MAX_AUTO_DELETE_TIMER_MS);
	}

	@Test
	public void testAcceptsInvitationWithNullAutoDeleteTimer()
			throws Exception {
		testAcceptsInvitationWithAutoDeleteTimer(null);
	}

	private void testAcceptsInvitationWithAutoDeleteTimer(@Nullable Long timer)
			throws Exception {
		expectCreateForum(forumName);
		expectEncodeMetadata(INVITE,
				timer == null ? NO_AUTO_DELETE_TIMER : timer);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, text,
						timer));
		assertExpectedContext(context, previousMsgId);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInvitationWithTooBigAutoDeleteTimer()
			throws Exception {
		testRejectsInvitationWithAutoDeleteTimer(MAX_AUTO_DELETE_TIMER_MS + 1);
	}

	@Test(expected = FormatException.class)
	public void testRejectsInvitationWithTooSmallAutoDeleteTimer()
			throws Exception {
		testRejectsInvitationWithAutoDeleteTimer(MIN_AUTO_DELETE_TIMER_MS - 1);
	}

	private void testRejectsInvitationWithAutoDeleteTimer(long timer)
			throws FormatException {
		expectCreateForum(forumName);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, text,
						timer));
		fail();
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullForumName() throws Exception {
		BdfList invalidDescriptor = BdfList.of(null, salt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringForumName() throws Exception {
		BdfList invalidDescriptor = BdfList.of(123, salt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortForumName() throws Exception {
		BdfList invalidDescriptor = BdfList.of("", salt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test
	public void testAcceptsMinLengthForumName() throws Exception {
		String shortForumName = getRandomString(1);
		BdfList validDescriptor = BdfList.of(shortForumName, salt);
		expectCreateForum(shortForumName);
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, validDescriptor,
						null));
		assertExpectedContext(context, previousMsgId);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongForumName() throws Exception {
		String invalidForumName = getRandomString(MAX_FORUM_NAME_LENGTH + 1);
		BdfList invalidDescriptor = BdfList.of(invalidForumName, salt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullSalt() throws Exception {
		BdfList invalidDescriptor = BdfList.of(forumName, null);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonRawSalt() throws Exception {
		BdfList invalidDescriptor = BdfList.of(forumName, 123);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortSalt() throws Exception {
		byte[] invalidSalt = getRandomBytes(FORUM_SALT_LENGTH - 1);
		BdfList invalidDescriptor = BdfList.of(forumName, invalidSalt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongSalt() throws Exception {
		byte[] invalidSalt = getRandomBytes(FORUM_SALT_LENGTH + 1);
		BdfList invalidDescriptor = BdfList.of(forumName, invalidSalt);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringText() throws Exception {
		expectCreateForum(forumName);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, 123));
	}

	@Test
	public void testAcceptsMinLengthText() throws Exception {
		expectCreateForum(forumName);
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, "1"));
		assertExpectedContext(context, previousMsgId);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongText() throws Exception {
		String invalidText = getRandomString(MAX_INVITATION_TEXT_LENGTH + 1);
		expectCreateForum(forumName);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor,
						invalidText));
	}

	private void expectCreateForum(String name) {
		context.checking(new Expectations() {{
			oneOf(forumFactory).createForum(name, salt);
			will(returnValue(forum));
		}});
	}

}
