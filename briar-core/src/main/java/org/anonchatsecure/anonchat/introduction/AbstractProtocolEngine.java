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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.properties.TransportProperties;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse;
import org.anonchatsecure.anonchat.api.introduction.event.IntroductionResponseReceivedEvent;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.api.introduction.Role.INTRODUCEE;
import static org.anonchatsecure.anonchat.introduction.MessageType.ABORT;
import static org.anonchatsecure.anonchat.introduction.MessageType.ACCEPT;
import static org.anonchatsecure.anonchat.introduction.MessageType.ACTIVATE;
import static org.anonchatsecure.anonchat.introduction.MessageType.AUTH;
import static org.anonchatsecure.anonchat.introduction.MessageType.DECLINE;
import static org.anonchatsecure.anonchat.introduction.MessageType.REQUEST;

@Immutable
@NotNullByDefault
abstract class AbstractProtocolEngine<S extends Session<?>>
		implements ProtocolEngine<S> {

	protected final DatabaseComponent db;
	protected final ClientHelper clientHelper;
	protected final ContactManager contactManager;
	protected final ContactGroupFactory contactGroupFactory;
	protected final MessageTracker messageTracker;
	protected final IdentityManager identityManager;
	protected final AuthorManager authorManager;
	protected final MessageParser messageParser;
	protected final MessageEncoder messageEncoder;
	protected final ClientVersioningManager clientVersioningManager;
	protected final AutoDeleteManager autoDeleteManager;
	protected final ConversationManager conversationManager;
	protected final Clock clock;

	AbstractProtocolEngine(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ContactManager contactManager,
			ContactGroupFactory contactGroupFactory,
			MessageTracker messageTracker,
			IdentityManager identityManager,
			AuthorManager authorManager,
			MessageParser messageParser,
			MessageEncoder messageEncoder,
			ClientVersioningManager clientVersioningManager,
			AutoDeleteManager autoDeleteManager,
			ConversationManager conversationManager,
			Clock clock) {
		this.db = db;
		this.clientHelper = clientHelper;
		this.contactManager = contactManager;
		this.contactGroupFactory = contactGroupFactory;
		this.messageTracker = messageTracker;
		this.identityManager = identityManager;
		this.authorManager = authorManager;
		this.messageParser = messageParser;
		this.messageEncoder = messageEncoder;
		this.clientVersioningManager = clientVersioningManager;
		this.autoDeleteManager = autoDeleteManager;
		this.conversationManager = conversationManager;
		this.clock = clock;
	}

	Message sendRequestMessage(Transaction txn, PeerSession s,
			long timestamp, Author author, @Nullable String text)
			throws DbException {
		Message m;
		ContactId c = clientHelper.getContactId(txn, s.getContactGroupId());
		if (contactSupportsAutoDeletion(txn, c)) {
			long timer = autoDeleteManager.getAutoDeleteTimer(txn, c,
					timestamp);
			m = messageEncoder.encodeRequestMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), author, text, timer);
			sendMessage(txn, REQUEST, s.getSessionId(), m, true, timer);
			// Set the auto-delete timer duration on the local message
			if (timer != NO_AUTO_DELETE_TIMER) {
				db.setCleanupTimerDuration(txn, m.getId(), timer);
			}
		} else {
			m = messageEncoder.encodeRequestMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), author, text);
			sendMessage(txn, REQUEST, s.getSessionId(), m, true,
					NO_AUTO_DELETE_TIMER);
		}
		return m;
	}

	Message sendAcceptMessage(Transaction txn, PeerSession s, long timestamp,
			PublicKey ephemeralPublicKey, long acceptTimestamp,
			Map<TransportId, TransportProperties> transportProperties,
			boolean visible) throws DbException {
		Message m;
		ContactId c = clientHelper.getContactId(txn, s.getContactGroupId());
		if (contactSupportsAutoDeletion(txn, c)) {
			long timer = autoDeleteManager.getAutoDeleteTimer(txn, c,
					timestamp);
			m = messageEncoder.encodeAcceptMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), s.getSessionId(),
					ephemeralPublicKey, acceptTimestamp, transportProperties,
					timer);
			sendMessage(txn, ACCEPT, s.getSessionId(), m, visible, timer);
			// Set the auto-delete timer duration on the message
			if (timer != NO_AUTO_DELETE_TIMER) {
				db.setCleanupTimerDuration(txn, m.getId(), timer);
			}
		} else {
			m = messageEncoder.encodeAcceptMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), s.getSessionId(),
					ephemeralPublicKey, acceptTimestamp, transportProperties);
			sendMessage(txn, ACCEPT, s.getSessionId(), m, visible,
					NO_AUTO_DELETE_TIMER);
		}
		return m;
	}

	Message sendDeclineMessage(Transaction txn, PeerSession s, long timestamp,
			boolean visible, boolean isAutoDecline) throws DbException {
		if (!visible && isAutoDecline) throw new IllegalArgumentException();
		Message m;
		ContactId c = clientHelper.getContactId(txn, s.getContactGroupId());
		if (contactSupportsAutoDeletion(txn, c)) {
			long timer = autoDeleteManager.getAutoDeleteTimer(txn, c,
					timestamp);
			m = messageEncoder.encodeDeclineMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), s.getSessionId(),
					timer);
			sendMessage(txn, DECLINE, s.getSessionId(), m, visible, timer,
					isAutoDecline);
			// Set the auto-delete timer duration on the local message
			if (timer != NO_AUTO_DELETE_TIMER) {
				db.setCleanupTimerDuration(txn, m.getId(), timer);
			}
			if (isAutoDecline) {
				// Broadcast an event, so the auto-decline becomes visible
				IntroduceeSession session = (IntroduceeSession) s;
				Author author = session.getRemote().author;
				AuthorInfo authorInfo =
						authorManager.getAuthorInfo(txn, author.getId());
				IntroductionResponse response = new IntroductionResponse(
						m.getId(), s.getContactGroupId(), m.getTimestamp(),
						true, true, false, false, s.getSessionId(), false,
						author, authorInfo, INTRODUCEE, false, timer, true);
				Event e = new IntroductionResponseReceivedEvent(response, c);
				txn.attach(e);
			}
		} else {
			m = messageEncoder.encodeDeclineMessage(s.getContactGroupId(),
					timestamp, s.getLastLocalMessageId(), s.getSessionId());
			sendMessage(txn, DECLINE, s.getSessionId(), m, visible,
					NO_AUTO_DELETE_TIMER);
		}
		return m;
	}

	Message sendAuthMessage(Transaction txn, PeerSession s, long timestamp,
			byte[] mac, byte[] signature) throws DbException {
		Message m = messageEncoder
				.encodeAuthMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId(), mac,
						signature);
		sendMessage(txn, AUTH, s.getSessionId(), m, false,
				NO_AUTO_DELETE_TIMER);
		return m;
	}

	Message sendActivateMessage(Transaction txn, PeerSession s, long timestamp,
			byte[] mac) throws DbException {
		Message m = messageEncoder
				.encodeActivateMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId(), mac);
		sendMessage(txn, ACTIVATE, s.getSessionId(), m, false,
				NO_AUTO_DELETE_TIMER);
		return m;
	}

	Message sendAbortMessage(Transaction txn, PeerSession s, long timestamp)
			throws DbException {
		Message m = messageEncoder
				.encodeAbortMessage(s.getContactGroupId(), timestamp,
						s.getLastLocalMessageId(), s.getSessionId());
		sendMessage(txn, ABORT, s.getSessionId(), m, false,
				NO_AUTO_DELETE_TIMER);
		return m;
	}

	private void sendMessage(Transaction txn, MessageType type,
			SessionId sessionId, Message m, boolean visibleInConversation,
			long autoDeleteTimer) throws DbException {
		sendMessage(txn, type, sessionId, m, visibleInConversation,
				autoDeleteTimer, false);
	}

	private void sendMessage(Transaction txn, MessageType type,
			SessionId sessionId, Message m, boolean visibleInConversation,
			long autoDeleteTimer, boolean isAutoDecline) throws DbException {
		BdfDictionary meta = messageEncoder.encodeMetadata(type, sessionId,
				m.getTimestamp(), true, true, visibleInConversation,
				autoDeleteTimer, isAutoDecline);
		try {
			clientHelper.addLocalMessage(txn, m, meta, true, false);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	void broadcastIntroductionResponseReceivedEvent(Transaction txn,
			Session<?> s, AuthorId sender, Author otherAuthor,
			AbstractIntroductionMessage m, boolean canSucceed)
			throws DbException {
		AuthorId localAuthorId = identityManager.getLocalAuthor(txn).getId();
		Contact c = contactManager.getContact(txn, sender, localAuthorId);
		AuthorInfo otherAuthorInfo =
				authorManager.getAuthorInfo(txn, otherAuthor.getId());
		IntroductionResponse response =
				new IntroductionResponse(m.getMessageId(), m.getGroupId(),
						m.getTimestamp(), false, false, false, false,
						s.getSessionId(), m instanceof AcceptMessage,
						otherAuthor, otherAuthorInfo, s.getRole(), canSucceed,
						m.getAutoDeleteTimer(), false);
		IntroductionResponseReceivedEvent e =
				new IntroductionResponseReceivedEvent(response, c.getId());
		txn.attach(e);
	}

	void markMessageVisibleInUi(Transaction txn, MessageId m)
			throws DbException {
		BdfDictionary meta = new BdfDictionary();
		messageEncoder.setVisibleInUi(meta, true);
		try {
			clientHelper.mergeMessageMetadata(txn, m, meta);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	boolean isInvalidDependency(@Nullable MessageId lastRemoteMessageId,
			@Nullable MessageId dependency) {
		if (dependency == null) return lastRemoteMessageId != null;
		return !dependency.equals(lastRemoteMessageId);
	}

	long getTimestampForOutgoingMessage(Transaction txn, GroupId contactGroupId)
			throws DbException {
		ContactId c = clientHelper.getContactId(txn, contactGroupId);
		return conversationManager.getTimestampForOutgoingMessage(txn, c);
	}

	void receiveAutoDeleteTimer(Transaction txn, AbstractIntroductionMessage m)
			throws DbException {
		ContactId c = clientHelper.getContactId(txn, m.getGroupId());
		autoDeleteManager.receiveAutoDeleteTimer(txn, c, m.getAutoDeleteTimer(),
				m.getTimestamp());
	}

	private boolean contactSupportsAutoDeletion(Transaction txn, ContactId c)
			throws DbException {
		int minorVersion = clientVersioningManager.getClientMinorVersion(txn, c,
				CLIENT_ID, MAJOR_VERSION);
		// Auto-delete was added in client version 0.1
		return minorVersion >= 1;
	}
}
