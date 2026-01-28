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

package org.anonchatsecure.anonchat.test;

import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.KeyPair;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorFactory;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.util.Base32;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;

import java.util.Locale;

import static java.lang.System.arraycopy;
import static org.anonchatsecure.bramble.api.contact.HandshakeLinkConstants.FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.contact.HandshakeLinkConstants.RAW_LINK_BYTES;
import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.junit.Assert.assertEquals;

public class BriarTestUtils {

	public static void assertGroupCount(MessageTracker tracker, GroupId g,
			long msgCount, long unreadCount, long latestMsgTime)
			throws DbException {
		GroupCount groupCount = tracker.getGroupCount(g);
		assertEquals(msgCount, groupCount.getMsgCount());
		assertEquals(unreadCount, groupCount.getUnreadCount());
		assertEquals(latestMsgTime, groupCount.getLatestMsgTime());
	}

	public static void assertGroupCount(MessageTracker tracker, GroupId g,
			long msgCount, long unreadCount) throws DbException {
		GroupCount c1 = tracker.getGroupCount(g);
		assertEquals(msgCount, c1.getMsgCount());
		assertEquals(unreadCount, c1.getUnreadCount());
	}

	public static Author getRealAuthor(AuthorFactory authorFactory) {
		String name = getRandomString(MAX_AUTHOR_NAME_LENGTH);
		return authorFactory.createLocalAuthor(name);
	}

	public static LocalAuthor getRealLocalAuthor(AuthorFactory authorFactory) {
		String name = getRandomString(MAX_AUTHOR_NAME_LENGTH);
		return authorFactory.createLocalAuthor(name);
	}

	public static String getRealHandshakeLink(CryptoComponent cryptoComponent) {
		KeyPair keyPair = cryptoComponent.generateAgreementKeyPair();
		byte[] linkBytes = new byte[RAW_LINK_BYTES];
		byte[] publicKey = keyPair.getPublic().getEncoded();
		linkBytes[0] = FORMAT_VERSION;
		arraycopy(publicKey, 0, linkBytes, 1, RAW_LINK_BYTES - 1);
		return ("briar://" + Base32.encode(linkBytes)).toLowerCase(Locale.US);
	}

}
