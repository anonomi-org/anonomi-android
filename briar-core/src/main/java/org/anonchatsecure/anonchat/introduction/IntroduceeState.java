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
enum IntroduceeState implements State {

	START(0),
	AWAIT_RESPONSES(1),
	LOCAL_DECLINED(2),
	REMOTE_DECLINED(3),
	LOCAL_ACCEPTED(4),
	REMOTE_ACCEPTED(5),
	AWAIT_AUTH(6),
	AWAIT_ACTIVATE(7);

	private final int value;

	IntroduceeState(int value) {
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

	static IntroduceeState fromValue(int value) throws FormatException {
		for (IntroduceeState s : values()) if (s.value == value) return s;
		throw new FormatException();
	}

}
