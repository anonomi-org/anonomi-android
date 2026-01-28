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
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.jmock.Expectations;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MAX_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MIN_AUTO_DELETE_TIMER_MS;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.sharing.SharingConstants.MAX_INVITATION_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.sharing.MessageType.INVITE;
import static org.junit.Assert.fail;

public class BlogSharingValidatorTest extends SharingValidatorTest {

	private final Author author = getAuthor();
	private final Blog blog = new Blog(group, author, false);
	private final BdfList authorList = BdfList.of(author.getFormatVersion(),
			author.getName(), author.getPublicKey());
	private final BdfList descriptor = BdfList.of(authorList, false);
	private final String text = getRandomString(MAX_INVITATION_TEXT_LENGTH);

	@Override
	SharingValidator getValidator() {
		return new BlogSharingValidator(messageEncoder, clientHelper,
				metadataEncoder, clock, blogFactory);
	}

	@Test
	public void testAcceptsInvitationWithText() throws Exception {
		expectCreateBlog();
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, text));
		assertExpectedContext(context, previousMsgId);
	}

	@Test
	public void testAcceptsInvitationWithNullText() throws Exception {
		expectCreateBlog();
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, null));
		assertExpectedContext(context, previousMsgId);
	}

	@Test
	public void testAcceptsInvitationWithNullPreviousMsgId() throws Exception {
		expectCreateBlog();
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), null, descriptor, text));
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
		expectCreateBlog();
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

	private void testRejectsInvitationWithAutoDeleteTimer(Long timer)
			throws Exception {
		expectCreateBlog();
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, text,
						timer));
		fail();
	}

	@Test
	public void testAcceptsInvitationForRssBlog() throws Exception {
		expectCreateRssBlog();
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfList rssDescriptor = BdfList.of(authorList, true);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, rssDescriptor,
						text));
		assertExpectedContext(context, previousMsgId);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullAuthor() throws Exception {
		BdfList invalidDescriptor = BdfList.of(null, false);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonListAuthor() throws Exception {
		BdfList invalidDescriptor = BdfList.of(123, false);
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, invalidDescriptor,
						null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringText() throws Exception {
		expectCreateBlog();
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor, 123));
	}

	@Test
	public void testAcceptsMinLengthText() throws Exception {
		String shortText = getRandomString(1);
		expectCreateBlog();
		expectEncodeMetadata(INVITE, NO_AUTO_DELETE_TIMER);
		BdfMessageContext context = validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor,
						shortText));
		assertExpectedContext(context, previousMsgId);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongText() throws Exception {
		String invalidText = getRandomString(MAX_INVITATION_TEXT_LENGTH + 1);
		expectCreateBlog();
		validator.validateMessage(message, group,
				BdfList.of(INVITE.getValue(), previousMsgId, descriptor,
						invalidText));
	}

	private void expectCreateBlog() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(author));
			oneOf(blogFactory).createBlog(author);
			will(returnValue(blog));
		}});
	}

	private void expectCreateRssBlog() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(author));
			oneOf(blogFactory).createFeedBlog(author);
			will(returnValue(blog));
		}});
	}
}
