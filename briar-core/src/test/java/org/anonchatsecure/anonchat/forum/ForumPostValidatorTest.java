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

package org.anonchatsecure.anonchat.forum;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.UniqueId;
import org.anonchatsecure.bramble.api.client.BdfMessageContext;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.ValidatorTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.Collection;

import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_SIGNATURE_LENGTH;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.blog.BlogConstants.KEY_READ;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_AUTHOR;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_PARENT;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_POST_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.forum.ForumPostFactory.SIGNING_LABEL_POST;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ForumPostValidatorTest extends ValidatorTestCase {

	private final MessageId parentId = new MessageId(getRandomId());
	private final String text = getRandomString(MAX_FORUM_POST_TEXT_LENGTH);
	private final byte[] signature = getRandomBytes(MAX_SIGNATURE_LENGTH);
	private final Author author = getAuthor();
	private final String authorName = author.getName();
	private final PublicKey authorPublicKey = author.getPublicKey();
	private final BdfList authorList = BdfList.of(author.getFormatVersion(),
			authorName, authorPublicKey.getEncoded());
	private final BdfList signedWithParent = BdfList.of(groupId, timestamp,
			parentId.getBytes(), authorList, text);
	private final BdfList signedWithoutParent = BdfList.of(groupId, timestamp,
			null, authorList, text);

	private final ForumPostValidator v = new ForumPostValidator(clientHelper,
			metadataEncoder, clock);

	@Test(expected = FormatException.class)
	public void testRejectsTooShortBody() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongBody() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, signature, 123));
	}

	@Test
	public void testAcceptsNullParentId() throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithoutParent, authorPublicKey);
		}});

		BdfMessageContext messageContext = v.validateMessage(message, group,
				BdfList.of(null, authorList, text, signature));
		assertExpectedContext(messageContext, false);
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonRawParentId() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(123, authorList, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooShortParentId() throws Exception {
		byte[] invalidParentId = getRandomBytes(UniqueId.LENGTH - 1);
		v.validateMessage(message, group,
				BdfList.of(invalidParentId, authorList, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongParentId() throws Exception {
		byte[] invalidParentId = getRandomBytes(UniqueId.LENGTH + 1);
		v.validateMessage(message, group,
				BdfList.of(invalidParentId, authorList, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullAuthorList() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, null, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonListAuthorList() throws Exception {
		v.validateMessage(message, group,
				BdfList.of(parentId, 123, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsInvalidAuthor() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(throwException(new FormatException()));
		}});
		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullText() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, null, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonStringText() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, 123, signature));
	}

	@Test
	public void testAcceptsMinLengthText() throws Exception {
		String shortText = "";
		BdfList signedWithShortText = BdfList.of(groupId, timestamp,
				parentId.getBytes(), authorList, shortText);

		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithShortText, authorPublicKey);
		}});

		BdfMessageContext messageContext = v.validateMessage(message, group,
				BdfList.of(parentId, authorList, shortText, signature));
		assertExpectedContext(messageContext, true);
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongText() throws Exception {
		String invalidText = getRandomString(MAX_FORUM_POST_TEXT_LENGTH + 1);

		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, invalidText, signature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNullSignature() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, null));
	}

	@Test(expected = FormatException.class)
	public void testRejectsNonRawSignature() throws Exception {
		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, 123));
	}

	@Test(expected = FormatException.class)
	public void testRejectsTooLongSignature() throws Exception {
		byte[] invalidSignature = getRandomBytes(MAX_SIGNATURE_LENGTH + 1);

		expectCreateAuthor();

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, invalidSignature));
	}

	@Test(expected = FormatException.class)
	public void testRejectsIfVerifyingSignatureThrowsFormatException()
			throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithParent, authorPublicKey);
			will(throwException(new FormatException()));
		}});

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, signature));
	}

	@Test(expected = InvalidMessageException.class)
	public void testRejectsIfVerifyingSignatureThrowsGeneralSecurityException()
			throws Exception {
		expectCreateAuthor();
		context.checking(new Expectations() {{
			oneOf(clientHelper).verifySignature(signature, SIGNING_LABEL_POST,
					signedWithParent, authorPublicKey);
			will(throwException(new GeneralSecurityException()));
		}});

		v.validateMessage(message, group,
				BdfList.of(parentId, authorList, text, signature));
	}

	private void expectCreateAuthor() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(author));
		}});
	}

	private void assertExpectedContext(BdfMessageContext messageContext,
			boolean hasParent) throws FormatException {
		BdfDictionary meta = messageContext.getDictionary();
		Collection<MessageId> dependencies = messageContext.getDependencies();
		if (hasParent) {
			assertEquals(4, meta.size());
			assertArrayEquals(parentId.getBytes(), meta.getRaw(KEY_PARENT));
			assertEquals(1, dependencies.size());
			assertEquals(parentId, dependencies.iterator().next());
		} else {
			assertEquals(3, meta.size());
			assertEquals(0, dependencies.size());
		}
		assertEquals(timestamp, meta.getLong(KEY_TIMESTAMP).longValue());
		assertFalse(meta.getBoolean(KEY_READ));
		assertEquals(authorList, meta.getList(KEY_AUTHOR));
	}
}
