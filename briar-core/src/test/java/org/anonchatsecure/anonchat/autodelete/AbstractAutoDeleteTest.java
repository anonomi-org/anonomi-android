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

package org.anonchatsecure.anonchat.autodelete;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.system.TimeTravelModule;
import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager.ConversationClient;
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageHeader;
import org.anonchatsecure.anonchat.test.BriarIntegrationTest;
import org.anonchatsecure.anonchat.test.BriarIntegrationTestComponent;
import org.anonchatsecure.anonchat.test.DaggerBriarIntegrationTestComponent;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.sort;
import static org.anonchatsecure.bramble.api.cleanup.CleanupManager.BATCH_DELAY_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class AbstractAutoDeleteTest extends
		BriarIntegrationTest<BriarIntegrationTestComponent> {

	protected final long startTime = System.currentTimeMillis();

	protected abstract ConversationClient getConversationClient(
			BriarIntegrationTestComponent component);

	@Override
	protected void createComponents() {
		BriarIntegrationTestComponent component =
				DaggerBriarIntegrationTestComponent.builder().build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(component);
		component.inject(this);

		c0 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t0Dir))
				.timeTravelModule(new TimeTravelModule(true))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c0);

		c1 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t1Dir))
				.timeTravelModule(new TimeTravelModule(true))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c1);

		c2 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t2Dir))
				.timeTravelModule(new TimeTravelModule(true))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c2);

		// Use different times to avoid creating identical messages that are
		// treated as redundant copies of the same message (#1907)
		try {
			c0.getTimeTravel().setCurrentTimeMillis(startTime);
			c1.getTimeTravel().setCurrentTimeMillis(startTime + 1);
			c2.getTimeTravel().setCurrentTimeMillis(startTime + 2);
		} catch (InterruptedException e) {
			fail();
		}
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		// Run the initial cleanup task that was scheduled at startup
		c0.getTimeTravel().addCurrentTimeMillis(BATCH_DELAY_MS);
		c1.getTimeTravel().addCurrentTimeMillis(BATCH_DELAY_MS);
		c2.getTimeTravel().addCurrentTimeMillis(BATCH_DELAY_MS);
	}

	protected List<ConversationMessageHeader> getMessageHeaders(
			BriarIntegrationTestComponent component, ContactId contactId)
			throws Exception {
		DatabaseComponent db = component.getDatabaseComponent();
		ConversationClient conversationClient =
				getConversationClient(component);
		return sortHeaders(db.transactionWithResult(true, txn ->
				conversationClient.getMessageHeaders(txn, contactId)));
	}

	@SuppressWarnings({"UseCompareMethod", "Java8ListSort"}) // Animal Sniffer
	protected List<ConversationMessageHeader> sortHeaders(
			Collection<ConversationMessageHeader> in) {
		List<ConversationMessageHeader> out = new ArrayList<>(in);
		sort(out, (a, b) ->
				Long.valueOf(a.getTimestamp()).compareTo(b.getTimestamp()));
		return out;
	}

	@FunctionalInterface
	protected interface HeaderConsumer {
		void accept(ConversationMessageHeader header) throws DbException;
	}

	protected void forEachHeader(BriarIntegrationTestComponent component,
			ContactId contactId, int size, HeaderConsumer consumer)
			throws Exception {
		List<ConversationMessageHeader> headers =
				getMessageHeaders(component, contactId);
		assertEquals(size, headers.size());
		for (ConversationMessageHeader h : headers) consumer.accept(h);
	}

	protected long getAutoDeleteTimer(BriarIntegrationTestComponent component,
			ContactId contactId) throws DbException {
		DatabaseComponent db = component.getDatabaseComponent();
		AutoDeleteManager autoDeleteManager = component.getAutoDeleteManager();
		return db.transactionWithResult(false,
				txn -> autoDeleteManager.getAutoDeleteTimer(txn, contactId));
	}

	protected void markMessageRead(BriarIntegrationTestComponent component,
			Contact contact, MessageId messageId) throws Exception {
		ConversationManager conversationManager =
				component.getConversationManager();
		ConversationClient conversationClient =
				getConversationClient(component);
		GroupId groupId = conversationClient.getContactGroup(contact).getId();
		conversationManager.setReadFlag(groupId, messageId, true);
		waitForEvents(component);
	}

	protected void assertGroupCount(BriarIntegrationTestComponent component,
			ContactId contactId, int messageCount, int unreadCount)
			throws DbException {
		DatabaseComponent db = component.getDatabaseComponent();
		ConversationClient conversationClient =
				getConversationClient(component);

		GroupCount gc = db.transactionWithResult(true, txn ->
				conversationClient.getGroupCount(txn, contactId));
		assertEquals("messageCount", messageCount, gc.getMsgCount());
		assertEquals("unreadCount", unreadCount, gc.getUnreadCount());
	}
}
