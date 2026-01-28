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
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
enum IntroducerState implements State {

	START(0),
	AWAIT_RESPONSES(1),
	AWAIT_RESPONSE_A(2), AWAIT_RESPONSE_B(3),
	A_DECLINED(4), B_DECLINED(5),
	AWAIT_AUTHS(6),
	AWAIT_AUTH_A(7), AWAIT_AUTH_B(8),
	AWAIT_ACTIVATES(9),
	AWAIT_ACTIVATE_A(10), AWAIT_ACTIVATE_B(11);

	private final int value;

	IntroducerState(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public boolean isComplete() {
		return this == START;
	}

	static IntroducerState fromValue(int value) throws FormatException {
		for (IntroducerState s : values()) if (s.value == value) return s;
		throw new FormatException();
	}

}
