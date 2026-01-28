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

package org.anonchatsecure.anonchat.client;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.bramble.test.TestUtils;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.jmock.Expectations;
import org.junit.Test;

import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_LATEST_MSG;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_MSG_COUNT;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_STORED_MESSAGE_ID;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.GROUP_KEY_UNREAD_COUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessageTrackerTest extends BrambleMockTestCase {

	protected final GroupId groupId = new GroupId(TestUtils.getRandomId());
	protected final ClientHelper clientHelper =
			context.mock(ClientHelper.class);
	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final Clock clock = context.mock(Clock.class);
	private final MessageId messageId = new MessageId(TestUtils.getRandomId());
	private final MessageTracker messageTracker =
			new MessageTrackerImpl(db, clientHelper, clock);
	private final BdfDictionary dictionary = BdfDictionary.of(
			new BdfEntry(GROUP_KEY_STORED_MESSAGE_ID, messageId)
	);

	@Test
	public void testInitializeGroupCount() throws Exception {
		Transaction txn = new Transaction(null, false);
		long now = 42L;
		BdfDictionary dictionary = BdfDictionary.of(
				new BdfEntry(GROUP_KEY_MSG_COUNT, 0),
				new BdfEntry(GROUP_KEY_UNREAD_COUNT, 0),
				new BdfEntry(GROUP_KEY_LATEST_MSG, now)
		);
		context.checking(new Expectations() {{
			oneOf(clock).currentTimeMillis();
			will(returnValue(now));
			oneOf(clientHelper).mergeGroupMetadata(txn, groupId, dictionary);
		}});
		messageTracker.initializeGroupCount(txn, groupId);
	}

	@Test
	public void testMessageStore() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).mergeGroupMetadata(groupId, dictionary);
		}});
		messageTracker.storeMessageId(groupId, messageId);
	}

	@Test
	public void testMessageLoad() throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).getGroupMetadataAsDictionary(groupId);
			will(returnValue(dictionary));
		}});
		MessageId loadedId = messageTracker.loadStoredMessageId(groupId);
		assertNotNull(loadedId);
		assertEquals(messageId, loadedId);
	}

}
