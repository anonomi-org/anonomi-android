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

package org.anonchatsecure.anonchat.api.identity;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.junit.Test;

import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MAX_CONTENT_TYPE_BYTES;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.NONE;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.VERIFIED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AuthorInfoTest extends BrambleTestCase {

	private final String contentType = getRandomString(MAX_CONTENT_TYPE_BYTES);
	private final AttachmentHeader avatarHeader =
			new AttachmentHeader(new GroupId(getRandomId()),
					new MessageId(getRandomId()), contentType);

	@Test
	public void testEquals() {
		assertEquals(
				new AuthorInfo(NONE),
				new AuthorInfo(NONE, null, null)
		);
		assertEquals(
				new AuthorInfo(NONE, "test", null),
				new AuthorInfo(NONE, "test", null)
		);
		assertEquals(
				new AuthorInfo(NONE, "test", avatarHeader),
				new AuthorInfo(NONE, "test", avatarHeader)
		);

		assertNotEquals(
				new AuthorInfo(NONE),
				new AuthorInfo(VERIFIED)
		);
		assertNotEquals(
				new AuthorInfo(NONE, "test", null),
				new AuthorInfo(NONE)
		);
		assertNotEquals(
				new AuthorInfo(NONE),
				new AuthorInfo(NONE, "test", null)
		);
		assertNotEquals(
				new AuthorInfo(NONE, "a", null),
				new AuthorInfo(NONE, "b", null)
		);
		assertNotEquals(
				new AuthorInfo(NONE, "a", null),
				new AuthorInfo(NONE, "a", avatarHeader)
		);
	}

}
