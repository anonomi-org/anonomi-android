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

package org.anonchatsecure.anonchat.messaging;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.Identity;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.sync.event.MessageStateChangedEvent;
import org.anonchatsecure.bramble.api.sync.event.MessagesSentEvent;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.bramble.test.TestTransportConnectionReader;
import org.anonchatsecure.bramble.test.TestTransportConnectionWriter;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.anonchatsecure.anonchat.api.messaging.event.AttachmentReceivedEvent;
import org.anonchatsecure.anonchat.api.messaging.event.PrivateMessageReceivedEvent;
import org.briarproject.nullsafety.NotNullByDefault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.anonchatsecure.bramble.api.sync.validation.MessageState.DELIVERED;
import static org.anonchatsecure.bramble.test.TestPluginConfigModule.SIMPLEX_TRANSPORT_ID;
import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.MIN_AUTO_DELETE_TIMER_MS;
import static org.junit.Assert.assertTrue;

public class SimplexMessagingIntegrationTest extends BrambleTestCase {

	private static final int TIMEOUT_MS = 5_000;

	private final File testDir = getTestDirectory();
	private final File aliceDir = new File(testDir, "alice");
	private final File bobDir = new File(testDir, "bob");

	private final SecretKey rootKey = getSecretKey();
	private final long timestamp = System.currentTimeMillis();

	private SimplexMessagingIntegrationTestComponent alice, bob;

	@Before
	public void setUp() {
		assertTrue(testDir.mkdirs());
		alice = DaggerSimplexMessagingIntegrationTestComponent.builder()
				.testDatabaseConfigModule(
						new TestDatabaseConfigModule(aliceDir)).build();
		SimplexMessagingIntegrationTestComponent.Helper
				.injectEagerSingletons(alice);
		bob = DaggerSimplexMessagingIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(bobDir))
				.build();
		SimplexMessagingIntegrationTestComponent.Helper
				.injectEagerSingletons(bob);
	}

	@Test
	public void testWriteAndReadWithLazyRetransmission() throws Exception {
		testWriteAndRead(false);
	}

	@Test
	public void testWriteAndReadWithEagerRetransmission() throws Exception {
		testWriteAndRead(true);
	}

	private void testWriteAndRead(boolean eager) throws Exception {
		// Create the identities
		Identity aliceIdentity =
				alice.getIdentityManager().createIdentity("Alice");
		Identity bobIdentity = bob.getIdentityManager().createIdentity("Bob");
		// Set up the devices and get the contact IDs
		ContactId bobId = setUp(alice, aliceIdentity,
				bobIdentity.getLocalAuthor(), true);
		ContactId aliceId = setUp(bob, bobIdentity,
				aliceIdentity.getLocalAuthor(), false);
		// Add a private message listener
		PrivateMessageListener listener = new PrivateMessageListener();
		bob.getEventBus().addListener(listener);
		// Alice sends a private message to Bob
		sendMessage(alice, bobId);
		// Sync Alice's client versions
		read(bob, write(alice, bobId, eager, 1), 1);
		// Sync Bob's client versions
		read(alice, write(bob, aliceId, eager, 1), 1);
		// Sync Alice's second client versioning update (with the active flag
		// raised), the private message and the attachment
		read(bob, write(alice, bobId, eager, 3), 3);
		// Bob should have received the private message
		assertTrue(listener.messageAdded);
		// Bob should have received the attachment
		assertTrue(listener.attachmentAdded);
		// Sync messages from Alice to Bob again. If using eager
		// retransmission, the three unacked messages should be sent again.
		// They're all duplicates, so no further deliveries should occur
		read(bob, write(alice, bobId, eager, eager ? 3 : 0), 0);
	}

	private ContactId setUp(SimplexMessagingIntegrationTestComponent device,
			Identity local, Author remote, boolean alice) throws Exception {
		// Add an identity for the user
		IdentityManager identityManager = device.getIdentityManager();
		identityManager.registerIdentity(local);
		// Start the lifecycle manager
		LifecycleManager lifecycleManager = device.getLifecycleManager();
		lifecycleManager.startServices(getSecretKey());
		lifecycleManager.waitForStartup();
		// Add the other user as a contact
		ContactManager contactManager = device.getContactManager();
		return contactManager.addContact(remote, local.getId(), rootKey,
				timestamp, alice, true, true);
	}

	private void sendMessage(SimplexMessagingIntegrationTestComponent device,
			ContactId contactId) throws Exception {
		MessagingManager messagingManager = device.getMessagingManager();
		GroupId groupId = messagingManager.getConversationId(contactId);
		long timestamp = System.currentTimeMillis();
		InputStream in = new ByteArrayInputStream(new byte[] {0, 1, 2, 3});
		AttachmentHeader attachmentHeader = messagingManager.addLocalAttachment(
				groupId, timestamp, "image/png", in);
		PrivateMessageFactory privateMessageFactory =
				device.getPrivateMessageFactory();
		PrivateMessage message = privateMessageFactory.createPrivateMessage(
				groupId, timestamp, "Hi!", singletonList(attachmentHeader),
				MIN_AUTO_DELETE_TIMER_MS);
		messagingManager.addLocalMessage(message);
	}

	@SuppressWarnings("SameParameterValue")
	private void read(SimplexMessagingIntegrationTestComponent device,
			byte[] stream, int deliveries) throws Exception {
		// Listen for message deliveries
		MessageDeliveryListener listener =
				new MessageDeliveryListener(deliveries);
		device.getEventBus().addListener(listener);
		// Read the incoming stream
		ByteArrayInputStream in = new ByteArrayInputStream(stream);
		TestTransportConnectionReader reader =
				new TestTransportConnectionReader(in);
		device.getConnectionManager().manageIncomingConnection(
				SIMPLEX_TRANSPORT_ID, reader);
		// Wait for the messages to be delivered
		assertTrue(listener.delivered.await(TIMEOUT_MS, MILLISECONDS));
		// Clean up the listener
		device.getEventBus().removeListener(listener);
	}

	private byte[] write(SimplexMessagingIntegrationTestComponent device,
			ContactId contactId, boolean eager, int transmissions)
			throws Exception {
		// Listen for message transmissions
		MessageTransmissionListener listener =
				new MessageTransmissionListener(transmissions);
		device.getEventBus().addListener(listener);
		// Write the outgoing stream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TestTransportConnectionWriter writer =
				new TestTransportConnectionWriter(out, eager);
		device.getConnectionManager().manageOutgoingConnection(contactId,
				SIMPLEX_TRANSPORT_ID, writer);
		// Wait for the writer to be disposed
		writer.getDisposedLatch().await(TIMEOUT_MS, MILLISECONDS);
		// Check that the expected number of messages were sent
		assertTrue(listener.sent.await(TIMEOUT_MS, MILLISECONDS));
		// Clean up the listener
		device.getEventBus().removeListener(listener);
		// Return the contents of the stream
		return out.toByteArray();
	}

	private void tearDown(SimplexMessagingIntegrationTestComponent device)
			throws Exception {
		// Stop the lifecycle manager
		LifecycleManager lifecycleManager = device.getLifecycleManager();
		lifecycleManager.stopServices();
		lifecycleManager.waitForShutdown();
	}

	@After
	public void tearDown() throws Exception {
		// Tear down the devices
		tearDown(alice);
		tearDown(bob);
		deleteTestDirectory(testDir);
	}

	@NotNullByDefault
	private static class MessageTransmissionListener implements EventListener {

		private final CountDownLatch sent;

		private MessageTransmissionListener(int transmissions) {
			sent = new CountDownLatch(transmissions);
		}

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof MessagesSentEvent) {
				MessagesSentEvent m = (MessagesSentEvent) e;
				for (MessageId ignored : m.getMessageIds()) sent.countDown();
			}
		}
	}

	@NotNullByDefault
	private static class MessageDeliveryListener implements EventListener {

		private final CountDownLatch delivered;

		private MessageDeliveryListener(int deliveries) {
			delivered = new CountDownLatch(deliveries);
		}

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof MessageStateChangedEvent) {
				MessageStateChangedEvent m = (MessageStateChangedEvent) e;
				if (!m.isLocal() && m.getState().equals(DELIVERED)) {
					delivered.countDown();
				}
			}
		}
	}

	@NotNullByDefault
	private static class PrivateMessageListener implements EventListener {

		private volatile boolean messageAdded = false;
		private volatile boolean attachmentAdded = false;

		@Override
		public void eventOccurred(Event e) {
			if (e instanceof PrivateMessageReceivedEvent) {
				messageAdded = true;
			} else if (e instanceof AttachmentReceivedEvent) {
				attachmentAdded = true;
			}
		}
	}
}
