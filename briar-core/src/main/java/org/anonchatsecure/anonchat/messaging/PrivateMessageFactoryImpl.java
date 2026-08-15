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

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.messaging.Location;
import org.anonchatsecure.anonchat.api.messaging.MoneroRequest;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.util.StringUtils.utf8IsTooLong;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_LOCATION_LABEL_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_MONERO_ADDRESS_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_MONERO_CURRENCY_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_MONERO_DESCRIPTION_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_MONERO_RATE;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.api.messaging.MessagingConstants.MIN_MONERO_RATE;
import static org.anonchatsecure.anonchat.messaging.MessageTypes.LOCATION;
import static org.anonchatsecure.anonchat.messaging.MessageTypes.MONERO_REQUEST;
import static org.anonchatsecure.anonchat.messaging.MessageTypes.PRIVATE_MESSAGE;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MONERO_EXTRA_CURRENCY;
import static org.anonchatsecure.anonchat.messaging.MessagingConstants.MONERO_EXTRA_RATE;

@Immutable
@NotNullByDefault
class PrivateMessageFactoryImpl implements PrivateMessageFactory {

	private final ClientHelper clientHelper;

	@Inject
	PrivateMessageFactoryImpl(ClientHelper clientHelper) {
		this.clientHelper = clientHelper;
	}

	@Override
	public PrivateMessage createLegacyPrivateMessage(GroupId groupId,
			long timestamp, String text) throws FormatException {
		// Validate the arguments
		if (utf8IsTooLong(text, MAX_PRIVATE_MESSAGE_TEXT_LENGTH))
			throw new IllegalArgumentException();
		// Serialise the message
		BdfList body = BdfList.of(text);
		Message m = clientHelper.createMessage(groupId, timestamp, body);
		return new PrivateMessage(m);
	}

	@Override
	public PrivateMessage createPrivateMessage(GroupId groupId, long timestamp,
			@Nullable String text, List<AttachmentHeader> headers)
			throws FormatException {
		validateTextAndAttachmentHeaders(text, headers);
		BdfList attachmentList = serialiseAttachmentHeaders(headers);
		// Serialise the message
		BdfList body = BdfList.of(PRIVATE_MESSAGE, text, attachmentList);
		Message m = clientHelper.createMessage(groupId, timestamp, body);
		return new PrivateMessage(m, text != null, headers);
	}

	@Override
	public PrivateMessage createPrivateMessage(GroupId groupId, long timestamp,
			@Nullable String text, List<AttachmentHeader> headers,
			long autoDeleteTimer) throws FormatException {
		validateTextAndAttachmentHeaders(text, headers);
		BdfList attachmentList = serialiseAttachmentHeaders(headers);
		// Serialise the message
		Long timer = autoDeleteTimer == NO_AUTO_DELETE_TIMER ?
				null : autoDeleteTimer;
		BdfList body = BdfList.of(PRIVATE_MESSAGE, text, attachmentList, timer);
		Message m = clientHelper.createMessage(groupId, timestamp, body);
		return new PrivateMessage(m, text != null, headers, autoDeleteTimer);
	}

	@Override
	public PrivateMessage createLocationMessage(GroupId groupId, long timestamp,
			Location location, long autoDeleteTimer) throws FormatException {
		if (utf8IsTooLong(location.getLabel(), MAX_LOCATION_LABEL_LENGTH)) {
			throw new IllegalArgumentException();
		}
		// Serialise the message
		Long timer = autoDeleteTimer == NO_AUTO_DELETE_TIMER ?
				null : autoDeleteTimer;
		BdfList body = BdfList.of(LOCATION, location.getLabel(),
				location.getLatitude(), location.getLongitude(),
				location.getZoom(), timer);
		Message m = clientHelper.createMessage(groupId, timestamp, body);
		return new PrivateMessage(m, location, autoDeleteTimer);
	}

	@Override
	public PrivateMessage createMoneroRequestMessage(GroupId groupId,
			long timestamp, MoneroRequest request, long autoDeleteTimer)
			throws FormatException {
		String subaddress = request.getSubaddress();
		if (subaddress.isEmpty()
				|| utf8IsTooLong(subaddress, MAX_MONERO_ADDRESS_LENGTH)) {
			throw new IllegalArgumentException();
		}
		Long amount = request.getAmount();
		if (amount != null && amount < 0) {
			throw new IllegalArgumentException();
		}
		String description = request.getDescription();
		if (description != null && utf8IsTooLong(description,
				MAX_MONERO_DESCRIPTION_LENGTH)) {
			throw new IllegalArgumentException();
		}
		String currency = request.getCurrency();
		if (currency != null
				&& utf8IsTooLong(currency, MAX_MONERO_CURRENCY_LENGTH)) {
			throw new IllegalArgumentException();
		}
		Double rate = request.getRate();
		// Excludes NaN with it, since every comparison against NaN is false
		if (rate != null && !(rate >= MIN_MONERO_RATE
				&& rate <= MAX_MONERO_RATE)) {
			throw new IllegalArgumentException();
		}
		// Serialise the message
		BdfDictionary extras = new BdfDictionary();
		if (currency != null && !currency.isEmpty()) {
			extras.put(MONERO_EXTRA_CURRENCY, currency);
		}
		if (rate != null) extras.put(MONERO_EXTRA_RATE, rate);
		Long timer = autoDeleteTimer == NO_AUTO_DELETE_TIMER ?
				null : autoDeleteTimer;
		BdfList body = BdfList.of(MONERO_REQUEST, subaddress, amount,
				description, extras, timer);
		Message m = clientHelper.createMessage(groupId, timestamp, body);
		return new PrivateMessage(m, request, autoDeleteTimer);
	}

	private void validateTextAndAttachmentHeaders(@Nullable String text,
			List<AttachmentHeader> headers) {
		if (text == null) {
			if (headers.isEmpty()) throw new IllegalArgumentException();
		} else if (utf8IsTooLong(text, MAX_PRIVATE_MESSAGE_TEXT_LENGTH)) {
			throw new IllegalArgumentException();
		}
	}

	private BdfList serialiseAttachmentHeaders(List<AttachmentHeader> headers) {
		BdfList attachmentList = new BdfList();
		for (AttachmentHeader a : headers) {
			attachmentList.add(
					BdfList.of(a.getMessageId(), a.getContentType()));
		}
		return attachmentList;
	}
}
