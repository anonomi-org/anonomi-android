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

package org.anonchatsecure.anonchat.introduction;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.crypto.PrivateKey;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.properties.TransportPropertyManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.transport.KeyManager;
import org.anonchatsecure.bramble.api.transport.KeySetId;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.jmock.Expectations;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.anonchatsecure.bramble.api.crypto.CryptoConstants.MAC_BYTES;
import static org.anonchatsecure.bramble.api.crypto.CryptoConstants.MAX_SIGNATURE_BYTES;
import static org.anonchatsecure.bramble.api.system.Clock.MIN_REASONABLE_TIME_MS;
import static org.anonchatsecure.bramble.test.TestUtils.getAgreementPrivateKey;
import static org.anonchatsecure.bramble.test.TestUtils.getAgreementPublicKey;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getContactId;
import static org.anonchatsecure.bramble.test.TestUtils.getLocalAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.introduction.IntroduceeState.AWAIT_ACTIVATE;
import static org.anonchatsecure.anonchat.introduction.IntroduceeState.AWAIT_AUTH;
import static org.anonchatsecure.anonchat.introduction.IntroduceeState.START;
import static org.anonchatsecure.anonchat.introduction.MessageType.ABORT;
import static org.anonchatsecure.anonchat.introduction.MessageType.ACTIVATE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntroduceeProtocolEngineTest extends BrambleMockTestCase {

	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final ContactManager contactManager =
			context.mock(ContactManager.class);
	private final ContactGroupFactory contactGroupFactory =
			context.mock(ContactGroupFactory.class);
	private final MessageTracker messageTracker =
			context.mock(MessageTracker.class);
	private final IdentityManager identityManager =
			context.mock(IdentityManager.class);
	private final AuthorManager authorManager =
			context.mock(AuthorManager.class);
	private final MessageParser messageParser =
			context.mock(MessageParser.class);
	private final MessageEncoder messageEncoder =
			context.mock(MessageEncoder.class);
	private final IntroductionCrypto crypto =
			context.mock(IntroductionCrypto.class);
	private final KeyManager keyManager = context.mock(KeyManager.class);
	private final TransportPropertyManager transportPropertyManager =
			context.mock(TransportPropertyManager.class);
	private final ClientVersioningManager clientVersioningManager =
			context.mock(ClientVersioningManager.class);
	private final AutoDeleteManager autoDeleteManager =
			context.mock(AutoDeleteManager.class);
	private final ConversationManager conversationManager =
			context.mock(ConversationManager.class);
	private final Clock clock = context.mock(Clock.class);

	private final Author introducer = getAuthor();
	private final Author remoteIntroducee = getAuthor();
	private final LocalAuthor localIntroducee = getLocalAuthor();
	private final ContactId contactId = getContactId();
	private final GroupId contactGroupId = new GroupId(getRandomId());
	private final SessionId sessionId = new SessionId(getRandomId());
	private final boolean alice = new Random().nextBoolean();
	private final long now = System.currentTimeMillis();
	private final long requestTimestamp = now - 12345;
	private final long localAcceptTimestamp = requestTimestamp + 123;
	private final long remoteAuthTimestamp = localAcceptTimestamp + 123;
	private final MessageId lastLocalMessageId = new MessageId(getRandomId());
	private final MessageId lastRemoteMessageId = new MessageId(getRandomId());
	private final PublicKey localPublicKey = getAgreementPublicKey();
	private final PrivateKey localPrivateKey = getAgreementPrivateKey();
	private final PublicKey remotePublicKey = getAgreementPublicKey();
	private final SecretKey localMacKey = getSecretKey();
	private final SecretKey remoteMacKey = getSecretKey();
	private final SecretKey masterKey = getSecretKey();
	private final byte[] remoteMac = getRandomBytes(MAC_BYTES);
	private final byte[] localMac = getRandomBytes(MAC_BYTES);
	private final byte[] remoteSignature = getRandomBytes(MAX_SIGNATURE_BYTES);

	private final IntroduceeProtocolEngine engine =
			new IntroduceeProtocolEngine(db, clientHelper, contactManager,
					contactGroupFactory, messageTracker, identityManager,
					authorManager, messageParser, messageEncoder, crypto,
					keyManager, transportPropertyManager,
					clientVersioningManager, autoDeleteManager,
					conversationManager, clock);

	@Test
	public void testDoesNotAbortSessionIfTimestampIsMaxAge() throws Exception {
		Transaction txn = new Transaction(null, false);

		long remoteAcceptTimestamp = MIN_REASONABLE_TIME_MS;
		IntroduceeSession session =
				createAwaitAuthSession(remoteAcceptTimestamp);

		AuthMessage authMessage = new AuthMessage(new MessageId(getRandomId()),
				contactGroupId, remoteAuthTimestamp, lastRemoteMessageId,
				sessionId, remoteMac, remoteSignature);

		Message activateMessage = getMessage(contactGroupId, 1234, now);
		BdfDictionary activateMeta = new BdfDictionary();

		context.checking(new Expectations() {{
			// Verify the auth message
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localIntroducee));
			oneOf(crypto).verifyAuthMac(remoteMac, session,
					localIntroducee.getId());
			oneOf(crypto).verifySignature(remoteSignature, session);
			// Add the contact
			oneOf(contactManager).addContact(txn, remoteIntroducee,
					localIntroducee.getId(), false);
			will(returnValue(contactId));
			oneOf(keyManager).addRotationKeys(txn, contactId, masterKey,
					remoteAcceptTimestamp, alice, false);
			will(returnValue(emptyMap()));
			oneOf(transportPropertyManager).addRemoteProperties(txn, contactId,
					emptyMap());
			// Send the activate message
			oneOf(crypto).activateMac(session);
			will(returnValue(localMac));
			oneOf(clock).currentTimeMillis();
			will(returnValue(now));
			oneOf(messageEncoder).encodeActivateMessage(contactGroupId, now,
					lastLocalMessageId, sessionId, localMac);
			will(returnValue(activateMessage));
			oneOf(messageEncoder).encodeMetadata(ACTIVATE, sessionId, now,
					true, true, false, NO_AUTO_DELETE_TIMER, false);
			will(returnValue(activateMeta));
			oneOf(clientHelper).addLocalMessage(txn, activateMessage,
					activateMeta, true, false);
		}});

		IntroduceeSession after =
				engine.onAuthMessage(txn, session, authMessage);

		assertEquals(AWAIT_ACTIVATE, after.getState());
		assertNull(after.getMasterKey());
		assertEquals(Collections.<TransportId, KeySetId>emptyMap(),
				after.getTransportKeys());

		IntroduceeSession.Local afterLocal = after.getLocal();
		assertEquals(activateMessage.getId(), afterLocal.lastMessageId);
		assertEquals(now, afterLocal.lastMessageTimestamp);
		assertNull(afterLocal.ephemeralPublicKey);
		assertNull(afterLocal.ephemeralPrivateKey);
		assertArrayEquals(localMacKey.getBytes(), afterLocal.macKey);

		IntroduceeSession.Remote afterRemote = after.getRemote();
		assertEquals(authMessage.getMessageId(), afterRemote.lastMessageId);
		assertNull(afterRemote.ephemeralPublicKey);
		assertArrayEquals(remoteMacKey.getBytes(), afterRemote.macKey);
	}

	@Test
	public void testAbortsSessionIfTimestampIsTooOld() throws Exception {
		Transaction txn = new Transaction(null, false);

		long remoteAcceptTimestamp = MIN_REASONABLE_TIME_MS - 1;
		IntroduceeSession session =
				createAwaitAuthSession(remoteAcceptTimestamp);

		AuthMessage authMessage = new AuthMessage(new MessageId(getRandomId()),
				contactGroupId, remoteAuthTimestamp, lastRemoteMessageId,
				sessionId, remoteMac, remoteSignature);

		BdfDictionary query = new BdfDictionary();
		Message abortMessage = getMessage(contactGroupId, 123, now);
		BdfDictionary abortMeta = new BdfDictionary();

		context.checking(new Expectations() {{
			// Verify the auth message
			oneOf(identityManager).getLocalAuthor(txn);
			will(returnValue(localIntroducee));
			oneOf(crypto).verifyAuthMac(remoteMac, session,
					localIntroducee.getId());
			oneOf(crypto).verifySignature(remoteSignature, session);
			// Abort the session
			oneOf(messageParser).getRequestsAvailableToAnswerQuery(sessionId);
			will(returnValue(query));
			oneOf(clientHelper).getMessageIds(txn, contactGroupId, query);
			will(returnValue(emptyList()));
			oneOf(clock).currentTimeMillis();
			will(returnValue(now));
			oneOf(messageEncoder).encodeAbortMessage(contactGroupId, now,
					lastLocalMessageId, sessionId);
			will(returnValue(abortMessage));
			oneOf(messageEncoder).encodeMetadata(ABORT, sessionId, now,
					true, true, false, NO_AUTO_DELETE_TIMER, false);
			will(returnValue(abortMeta));
			oneOf(clientHelper).addLocalMessage(txn, abortMessage, abortMeta,
					true, false);
		}});

		IntroduceeSession after =
				engine.onAuthMessage(txn, session, authMessage);

		assertEquals(START, after.getState());
		assertNull(after.getMasterKey());
		assertNull(after.getTransportKeys());

		IntroduceeSession.Local afterLocal = after.getLocal();
		assertEquals(abortMessage.getId(), afterLocal.lastMessageId);
		assertEquals(now, afterLocal.lastMessageTimestamp);
		assertNull(afterLocal.ephemeralPublicKey);
		assertNull(afterLocal.ephemeralPrivateKey);
		assertNull(afterLocal.macKey);

		IntroduceeSession.Remote afterRemote = after.getRemote();
		assertEquals(authMessage.getMessageId(), afterRemote.lastMessageId);
		assertNull(afterRemote.ephemeralPublicKey);
		assertNull(afterRemote.macKey);
	}

	private IntroduceeSession createAwaitAuthSession(
			long remoteAcceptTimestamp) {
		IntroduceeSession.Local local = new IntroduceeSession.Local(alice,
				lastLocalMessageId, localAcceptTimestamp, localPublicKey,
				localPrivateKey, emptyMap(), localAcceptTimestamp,
				localMacKey.getBytes());
		IntroduceeSession.Remote remote = new IntroduceeSession.Remote(!alice,
				remoteIntroducee, lastRemoteMessageId, remotePublicKey,
				emptyMap(), remoteAcceptTimestamp, remoteMacKey.getBytes());
		return new IntroduceeSession(sessionId,
				AWAIT_AUTH, requestTimestamp, contactGroupId, introducer,
				local, remote, masterKey.getBytes(), emptyMap());
	}
}
