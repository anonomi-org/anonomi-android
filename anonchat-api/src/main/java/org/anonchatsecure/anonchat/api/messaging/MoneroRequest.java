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

import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A request to be paid, as shared in a conversation. The subaddress is
 * carried in full so the recipient can pay it without reading it out of an
 * image, and the description is written by whoever sent it, so it is
 * arbitrary text.
 * <p>
 * The rate is what the sender valued the currency at when they chose the
 * amount, which the recipient needs in order to tell a current amount from
 * one priced days ago. It is not a conversion the app performs.
 */
@Immutable
@NotNullByDefault
public class MoneroRequest {

	/**
	 * The atomic units ("piconero") in one XMR. Amounts travel as atomic
	 * units so that a decimal amount cannot be rounded on the way.
	 */
	public static final long ATOMIC_UNITS_PER_XMR = 1_000_000_000_000L;

	private final String subaddress;
	@Nullable
	private final Long amount;
	@Nullable
	private final String description;
	@Nullable
	private final String currency;
	@Nullable
	private final Double rate;

	public MoneroRequest(String subaddress, @Nullable Long amount,
			@Nullable String description, @Nullable String currency,
			@Nullable Double rate) {
		this.subaddress = subaddress;
		this.amount = amount;
		this.description = description;
		this.currency = currency;
		this.rate = rate;
	}

	public String getSubaddress() {
		return subaddress;
	}

	/**
	 * Returns the amount requested in atomic units, or null if the sender
	 * did not ask for a particular amount.
	 */
	@Nullable
	public Long getAmount() {
		return amount;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the currency the rate is quoted in, or null if the sender's
	 * release did not record one.
	 */
	@Nullable
	public String getCurrency() {
		return currency;
	}

	/**
	 * Returns units of {@link #getCurrency() the currency} per XMR, or null
	 * if the sender did not quote a rate.
	 */
	@Nullable
	public Double getRate() {
		return rate;
	}
}
