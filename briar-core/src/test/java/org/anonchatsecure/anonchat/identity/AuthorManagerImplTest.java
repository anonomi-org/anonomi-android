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

package org.anonchatsecure.anonchat.identity;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.bramble.test.DbExpectations;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.avatar.AvatarManager;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.jmock.Expectations;
import org.junit.Test;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getContact;
import static org.anonchatsecure.bramble.test.TestUtils.getLocalAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.OURSELVES;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNKNOWN;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.VERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AuthorManagerImplTest extends BrambleMockTestCase {

	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final IdentityManager identityManager =
			context.mock(IdentityManager.class);
	private final AvatarManager avatarManager =
			context.mock(AvatarManager.class);

	private final Author remote = getAuthor();
	private final LocalAuthor localAuthor = getLocalAuthor();
	private final AuthorId local = localAuthor.getId();
	private final boolean verified = false;
	private final Contact contact = getContact(remote, local, verified);
	private final String contentType = getRandomString(MAX_CONTENT_TYPE_BYTES);
	private final AttachmentHeader avatarHeader =
			new AttachmentHeader(new GroupId(getRandomId()),
					new MessageId(getRandomId()), contentType);

	private final AuthorManagerImpl authorManager =
			new AuthorManagerImpl(db, identityManager, avatarManager);

	@Test
	public void testGetAuthorInfoUnverified() throws Exception {
		Transaction txn = new Transaction(null, true);

		checkAuthorInfoContext(txn, remote.getId(), singletonList(contact));
		context.checking(new DbExpectations() {{
			oneOf(avatarManager).getAvatarHeader(txn, contact);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(UNVERIFIED, authorInfo.getStatus());
		assertEquals(contact.getAlias(), authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoUnknown() throws DbException {
		Transaction txn = new Transaction(null, true);

		checkAuthorInfoContext(txn, remote.getId(), emptyList());

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(UNKNOWN, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertNull(authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoVerified() throws DbException {
		Transaction txn = new Transaction(null, true);

		Contact verified = getContact(remote, local, true);
		checkAuthorInfoContext(txn, remote.getId(), singletonList(verified));
		context.checking(new DbExpectations() {{
			oneOf(avatarManager).getAvatarHeader(txn, verified);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, remote.getId());
		assertEquals(VERIFIED, authorInfo.getStatus());
		assertEquals(verified.getAlias(), authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetAuthorInfoOurselves() throws DbException {
		Transaction txn = new Transaction(null, true);

		context.checking(new Expectations() {{
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localAuthor));
			never(db).getContactsByAuthorId(txn, remote.getId());
			oneOf(avatarManager).getMyAvatarHeader(txn);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getAuthorInfo(txn, localAuthor.getId());
		assertEquals(OURSELVES, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	@Test
	public void testGetMyAuthorInfo() throws DbException {
		Transaction txn = new Transaction(null, true);

		context.checking(new Expectations() {{
			oneOf(avatarManager).getMyAvatarHeader(txn);
			will(returnValue(avatarHeader));
		}});

		AuthorInfo authorInfo =
				authorManager.getMyAuthorInfo(txn);
		assertEquals(OURSELVES, authorInfo.getStatus());
		assertNull(authorInfo.getAlias());
		assertEquals(avatarHeader, authorInfo.getAvatarHeader());
	}

	private void checkAuthorInfoContext(Transaction txn, AuthorId authorId,
			Collection<Contact> contacts) throws DbException {
		context.checking(new Expectations() {{
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localAuthor));
			oneOf(db).getContactsByAuthorId(txn, authorId);
			will(returnValue(contacts));
		}});
	}

}
