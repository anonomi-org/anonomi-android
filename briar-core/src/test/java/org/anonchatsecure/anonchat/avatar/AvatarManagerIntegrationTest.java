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

package org.anonchatsecure.anonchat.avatar;

import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.anonchat.api.attachment.Attachment;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.attachment.AttachmentReader;
import org.anonchatsecure.anonchat.api.avatar.AvatarManager;
import org.anonchatsecure.anonchat.test.BriarIntegrationTest;
import org.anonchatsecure.anonchat.test.BriarIntegrationTestComponent;
import org.anonchatsecure.anonchat.test.DaggerBriarIntegrationTestComponent;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.util.IoUtils.copyAndClose;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AvatarManagerIntegrationTest
		extends BriarIntegrationTest<BriarIntegrationTestComponent> {

	private AvatarManager avatarManager0, avatarManager1;
	private AttachmentReader attachmentReader0, attachmentReader1;

	private final String contentType = getRandomString(MAX_CONTENT_TYPE_BYTES);

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		avatarManager0 = c0.getAvatarManager();
		avatarManager1 = c1.getAvatarManager();
		attachmentReader0 = c0.getAttachmentReader();
		attachmentReader1 = c1.getAttachmentReader();
	}

	@Override
	protected void createComponents() {
		BriarIntegrationTestComponent component =
				DaggerBriarIntegrationTestComponent.builder().build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(component);
		component.inject(this);

		c0 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t0Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c0);

		c1 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t1Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c1);

		c2 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t2Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c2);
	}

	@Test
	public void testAddingAndSyncAvatars() throws Exception {
		// Both contacts don't have avatars
		assertNull(db0.transactionWithNullableResult(true,
				txn -> avatarManager0.getMyAvatarHeader(txn)));
		assertNull(db1.transactionWithNullableResult(true,
				txn -> avatarManager1.getMyAvatarHeader(txn)));

		// Both contacts don't see avatars for each other
		assertNull(db0.transactionWithNullableResult(true,
				txn -> avatarManager0.getAvatarHeader(txn, contact1From0)));
		assertNull(db1.transactionWithNullableResult(true,
				txn -> avatarManager1.getAvatarHeader(txn, contact0From1)));

		// 0 adds avatar
		byte[] avatar0bytes = getRandomBytes(42);
		InputStream avatar0inputStream = new ByteArrayInputStream(avatar0bytes);
		AttachmentHeader header0 =
				avatarManager0.addAvatar(contentType, avatar0inputStream);
		assertEquals(contentType, header0.getContentType());

		// 0 sees their own avatar
		header0 = db0.transactionWithResult(true,
				txn -> avatarManager0.getMyAvatarHeader(txn));
		assertNotNull(header0);
		assertEquals(contentType, header0.getContentType());
		assertNotNull(header0.getMessageId());

		// 0 can retrieve their own avatar
		Attachment attachment0 = attachmentReader0.getAttachment(header0);
		assertEquals(contentType, attachment0.getHeader().getContentType());
		assertStreamMatches(avatar0bytes, attachment0.getStream());

		// send the avatar from 0 to 1
		sync0To1(1, true);

		// 1 also sees 0's avatar now
		AttachmentHeader header0From1 = db1.transactionWithResult(true,
				txn -> avatarManager1.getAvatarHeader(txn, contact0From1));
		assertNotNull(header0From1);
		assertEquals(contentType, header0From1.getContentType());
		assertNotNull(header0From1.getMessageId());

		// 1 can retrieve 0's avatar
		Attachment attachment0From1 =
				attachmentReader1.getAttachment(header0From1);
		assertEquals(contentType,
				attachment0From1.getHeader().getContentType());
		assertStreamMatches(avatar0bytes, attachment0From1.getStream());

		// 1 also adds avatar
		String contentType1 = getRandomString(MAX_CONTENT_TYPE_BYTES);
		byte[] avatar1bytes = getRandomBytes(42);
		InputStream avatar1inputStream = new ByteArrayInputStream(avatar1bytes);
		avatarManager1.addAvatar(contentType1, avatar1inputStream);

		// send the avatar from 1 to 0
		sync1To0(1, true);

		// 0 sees 1's avatar now
		AttachmentHeader header1From0 = db0.transactionWithResult(true,
				txn -> avatarManager0.getAvatarHeader(txn, contact1From0));
		assertNotNull(header1From0);
		assertEquals(contentType1, header1From0.getContentType());
		assertNotNull(header1From0.getMessageId());

		// 0 can retrieve 1's avatar
		Attachment attachment1From0 =
				attachmentReader0.getAttachment(header1From0);
		assertEquals(contentType1,
				attachment1From0.getHeader().getContentType());
		assertStreamMatches(avatar1bytes, attachment1From0.getStream());
	}

	@Test
	public void testUpdatingAvatars() throws Exception {
		// 0 adds avatar
		byte[] avatar0bytes = getRandomBytes(42);
		InputStream avatar0inputStream = new ByteArrayInputStream(avatar0bytes);
		avatarManager0.addAvatar(contentType, avatar0inputStream);

		// 0 can retrieve their own avatar
		AttachmentHeader header0 = db0.transactionWithResult(true,
				txn -> avatarManager0.getMyAvatarHeader(txn));
		assertNotNull(header0);
		Attachment attachment0 = attachmentReader0.getAttachment(header0);
		assertStreamMatches(avatar0bytes, attachment0.getStream());

		// send the avatar from 0 to 1
		sync0To1(1, true);

		// 1 only sees 0's avatar
		AttachmentHeader header0From1 = db1.transactionWithNullableResult(true,
				txn -> avatarManager1.getAvatarHeader(txn, contact0From1));
		assertNotNull(header0From1);
		Attachment attachment0From1 =
				attachmentReader1.getAttachment(header0From1);
		assertStreamMatches(avatar0bytes, attachment0From1.getStream());

		// 0 adds a new avatar
		byte[] avatar0bytes2 = getRandomBytes(42);
		InputStream avatar0inputStream2 =
				new ByteArrayInputStream(avatar0bytes2);
		avatarManager0.addAvatar(contentType, avatar0inputStream2);

		// 0 now only sees their new avatar
		header0 = db0.transactionWithResult(true,
				txn -> avatarManager0.getMyAvatarHeader(txn));
		assertNotNull(header0);
		attachment0 = attachmentReader0.getAttachment(header0);
		assertStreamMatches(avatar0bytes2, attachment0.getStream());

		// send the new avatar from 0 to 1
		sync0To1(1, true);

		// 1 only sees 0's new avatar
		header0From1 = db1.transactionWithNullableResult(true,
				txn -> avatarManager1.getAvatarHeader(txn, contact0From1));
		assertNotNull(header0From1);
		attachment0From1 = attachmentReader1.getAttachment(header0From1);
		assertStreamMatches(avatar0bytes2, attachment0From1.getStream());
	}

	private void assertStreamMatches(byte[] bytes, InputStream inputStream) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		copyAndClose(inputStream, outputStream);
		assertArrayEquals(bytes, outputStream.toByteArray());
	}

}
