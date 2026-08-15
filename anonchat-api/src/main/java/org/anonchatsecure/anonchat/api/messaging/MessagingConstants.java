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

package org.anonchatsecure.anonchat.api.messaging;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;

public interface MessagingConstants {

	/**
	 * The maximum length of a private message's text in UTF-8 bytes.
	 */
	int MAX_PRIVATE_MESSAGE_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 2048;

	/**
	 * The maximum number of attachments per private message.
	 */
	int MAX_ATTACHMENTS_PER_MESSAGE = 10;

	/**
	 * The maximum length of a location's label in UTF-8 bytes.
	 */
	int MAX_LOCATION_LABEL_LENGTH = 200;

	/**
	 * The range of zoom levels a location may ask to be shown at. The upper
	 * bound is the highest level the map library can address.
	 */
	double MIN_LOCATION_ZOOM = 0;
	double MAX_LOCATION_ZOOM = 22;

	/**
	 * The maximum length of a Monero address in UTF-8 bytes. We only send
	 * subaddresses, which are 95 characters, but an integrated address is
	 * 106 and rejecting one outright would destroy the message rather than
	 * report it.
	 */
	int MAX_MONERO_ADDRESS_LENGTH = 106;

	/**
	 * The maximum length of a payment request's description in UTF-8 bytes.
	 */
	int MAX_MONERO_DESCRIPTION_LENGTH = 255;

	/**
	 * The maximum length of a currency code in UTF-8 bytes. ISO 4217 codes
	 * are three characters, with room for anything a rate source quotes.
	 */
	int MAX_MONERO_CURRENCY_LENGTH = 8;

	/**
	 * The range a quoted exchange rate may fall in. The upper bound only
	 * has to leave room for a weak currency, not to be plausible.
	 */
	double MIN_MONERO_RATE = 0;
	double MAX_MONERO_RATE = 1e12;

	/**
	 * The largest amount a payment request may ask for, in atomic units.
	 * There is no tighter bound to apply: Monero's total supply does not fit
	 * in a signed 64-bit integer, so every representable value is below it.
	 */
	long MAX_MONERO_AMOUNT = Long.MAX_VALUE;

	/**
	 * Bounds on the dictionary a payment request carries its extra fields
	 * in. A key we do not recognise is ignored rather than rejected, which
	 * is what lets a later release add a field without older peers
	 * invalidating the message; these bound what such a key can cost us, so
	 * a later release may add keys but must keep them within these limits.
	 */
	int MAX_MONERO_EXTRAS = 16;
	int MAX_MONERO_EXTRA_KEY_LENGTH = 32;
	int MAX_MONERO_EXTRA_VALUE_LENGTH = 128;

}
