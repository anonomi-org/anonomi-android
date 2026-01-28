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
import org.anonchatsecure.bramble.api.crypto.AgreementPublicKey;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.properties.TransportProperties;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Map;

import javax.inject.Inject;

import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.MSG_KEY_READ;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_AVAILABLE_TO_ANSWER;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_IS_AUTO_DECLINE;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_LOCAL;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_MESSAGE_TYPE;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_SESSION_ID;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.introduction.IntroductionConstants.MSG_KEY_VISIBLE_IN_UI;
import static org.anonchatsecure.anonchat.introduction.MessageType.REQUEST;

@NotNullByDefault
class MessageParserImpl implements MessageParser {

	private final ClientHelper clientHelper;

	@Inject
	MessageParserImpl(ClientHelper clientHelper) {
		this.clientHelper = clientHelper;
	}

	@Override
	public BdfDictionary getMessagesVisibleInUiQuery() {
		return BdfDictionary.of(new BdfEntry(MSG_KEY_VISIBLE_IN_UI, true));
	}

	@Override
	public BdfDictionary getRequestsAvailableToAnswerQuery(
			SessionId sessionId) {
		return BdfDictionary.of(
				new BdfEntry(MSG_KEY_AVAILABLE_TO_ANSWER, true),
				new BdfEntry(MSG_KEY_MESSAGE_TYPE, REQUEST.getValue()),
				new BdfEntry(MSG_KEY_SESSION_ID, sessionId)
		);
	}

	@Override
	public MessageMetadata parseMetadata(BdfDictionary d)
			throws FormatException {
		MessageType type =
				MessageType.fromValue(d.getInt(MSG_KEY_MESSAGE_TYPE));
		byte[] sessionIdBytes = d.getOptionalRaw(MSG_KEY_SESSION_ID);
		SessionId sessionId =
				sessionIdBytes == null ? null : new SessionId(sessionIdBytes);
		long timestamp = d.getLong(MSG_KEY_TIMESTAMP);
		boolean local = d.getBoolean(MSG_KEY_LOCAL);
		boolean read = d.getBoolean(MSG_KEY_READ);
		boolean visible = d.getBoolean(MSG_KEY_VISIBLE_IN_UI);
		boolean available = d.getBoolean(MSG_KEY_AVAILABLE_TO_ANSWER, false);
		long timer = d.getLong(MSG_KEY_AUTO_DELETE_TIMER, NO_AUTO_DELETE_TIMER);
		boolean isAutoDecline = d.getBoolean(MSG_KEY_IS_AUTO_DECLINE, false);
		return new MessageMetadata(type, sessionId, timestamp, local, read,
				visible, available, timer, isAutoDecline);
	}

	@Override
	public RequestMessage parseRequestMessage(Message m, BdfList body)
			throws FormatException {
		byte[] previousMsgBytes = body.getOptionalRaw(1);
		MessageId previousMessageId = (previousMsgBytes == null ? null :
				new MessageId(previousMsgBytes));
		Author author = clientHelper.parseAndValidateAuthor(body.getList(2));
		String text = body.getOptionalString(3);
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 5) timer = body.getLong(4, NO_AUTO_DELETE_TIMER);
		return new RequestMessage(m.getId(), m.getGroupId(),
				m.getTimestamp(), previousMessageId, author, text, timer);
	}

	@Override
	public AcceptMessage parseAcceptMessage(Message m, BdfList body)
			throws FormatException {
		SessionId sessionId = new SessionId(body.getRaw(1));
		byte[] previousMsgBytes = body.getOptionalRaw(2);
		MessageId previousMessageId = (previousMsgBytes == null ? null :
				new MessageId(previousMsgBytes));
		PublicKey ephemeralPublicKey = new AgreementPublicKey(body.getRaw(3));
		long acceptTimestamp = body.getLong(4);
		Map<TransportId, TransportProperties> transportProperties = clientHelper
				.parseAndValidateTransportPropertiesMap(body.getDictionary(5));
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 7) timer = body.getLong(6, NO_AUTO_DELETE_TIMER);
		return new AcceptMessage(m.getId(), m.getGroupId(), m.getTimestamp(),
				previousMessageId, sessionId, ephemeralPublicKey,
				acceptTimestamp, transportProperties, timer);
	}

	@Override
	public DeclineMessage parseDeclineMessage(Message m, BdfList body)
			throws FormatException {
		SessionId sessionId = new SessionId(body.getRaw(1));
		byte[] previousMsgBytes = body.getOptionalRaw(2);
		MessageId previousMessageId = (previousMsgBytes == null ? null :
				new MessageId(previousMsgBytes));
		long timer = NO_AUTO_DELETE_TIMER;
		if (body.size() == 4) timer = body.getLong(3, NO_AUTO_DELETE_TIMER);
		return new DeclineMessage(m.getId(), m.getGroupId(), m.getTimestamp(),
				previousMessageId, sessionId, timer);
	}

	@Override
	public AuthMessage parseAuthMessage(Message m, BdfList body)
			throws FormatException {
		SessionId sessionId = new SessionId(body.getRaw(1));
		byte[] previousMsgBytes = body.getRaw(2);
		MessageId previousMessageId = new MessageId(previousMsgBytes);
		byte[] mac = body.getRaw(3);
		byte[] signature = body.getRaw(4);
		return new AuthMessage(m.getId(), m.getGroupId(), m.getTimestamp(),
				previousMessageId, sessionId, mac, signature);
	}

	@Override
	public ActivateMessage parseActivateMessage(Message m, BdfList body)
			throws FormatException {
		SessionId sessionId = new SessionId(body.getRaw(1));
		byte[] previousMsgBytes = body.getRaw(2);
		MessageId previousMessageId = new MessageId(previousMsgBytes);
		byte[] mac = body.getRaw(3);
		return new ActivateMessage(m.getId(), m.getGroupId(), m.getTimestamp(),
				previousMessageId, sessionId, mac);
	}

	@Override
	public AbortMessage parseAbortMessage(Message m, BdfList body)
			throws FormatException {
		SessionId sessionId = new SessionId(body.getRaw(1));
		byte[] previousMsgBytes = body.getOptionalRaw(2);
		MessageId previousMessageId = (previousMsgBytes == null ? null :
				new MessageId(previousMsgBytes));
		return new AbortMessage(m.getId(), m.getGroupId(), m.getTimestamp(),
				previousMessageId, sessionId);
	}

}
