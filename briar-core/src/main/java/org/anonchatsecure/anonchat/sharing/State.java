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

package org.anonchatsecure.anonchat.sharing;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.sync.Group.Visibility;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.bramble.api.sync.Group.Visibility.INVISIBLE;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.SHARED;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.VISIBLE;

@Immutable
@NotNullByDefault
enum State {

	START(0, INVISIBLE),
	/**
	 * The local user has been invited to the shareable, but not yet responded.
	 */
	LOCAL_INVITED(1, INVISIBLE),
	/**
	 * The remote user has been invited to the shareable, but not yet responded.
	 */
	REMOTE_INVITED(2, VISIBLE),
	SHARING(3, SHARED),
	LOCAL_LEFT(4, INVISIBLE),
	/**
	 * The local user has left the shareable, but the remote user hasn't.
	 */
	REMOTE_HANGING(5, INVISIBLE);

	private final int value;
	private final Visibility visibility;

	State(int value, Visibility visibility) {
		this.value = value;
		this.visibility = visibility;
	}

	public int getValue() {
		return value;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public boolean isAwaitingResponse() {
		return this == LOCAL_INVITED || this == REMOTE_INVITED;
	}

	static State fromValue(int value) throws FormatException {
		for (State s : values()) if (s.value == value) return s;
		throw new FormatException();
	}

}
