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

package org.anonchatsecure.anonchat.privategroup.invitation;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.sync.Group.Visibility;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.bramble.api.sync.Group.Visibility.INVISIBLE;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.SHARED;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.VISIBLE;

@Immutable
@NotNullByDefault
enum InviteeState implements State {

	START(0, INVISIBLE),
	INVITED(1, INVISIBLE),
	ACCEPTED(2, VISIBLE),
	JOINED(3, SHARED),
	LEFT(4, INVISIBLE),
	DISSOLVED(5, INVISIBLE),
	ERROR(6, INVISIBLE);

	private final int value;
	private final Visibility visibility;

	InviteeState(int value, Visibility visibility) {
		this.value = value;
		this.visibility = visibility;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public Visibility getVisibility() {
		return visibility;
	}

	@Override
	public boolean isAwaitingResponse() {
		return this == INVITED;
	}

	static InviteeState fromValue(int value) throws FormatException {
		for (InviteeState s : values()) if (s.value == value) return s;
		throw new FormatException();
	}
}
