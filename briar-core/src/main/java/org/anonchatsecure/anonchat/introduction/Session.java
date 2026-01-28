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

import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.introduction.Role;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
abstract class Session<S extends State> {

	private final SessionId sessionId;
	private final S state;
	private final long requestTimestamp;

	Session(SessionId sessionId, S state, long requestTimestamp) {
		this.sessionId = sessionId;
		this.state = state;
		this.requestTimestamp = requestTimestamp;
	}

	abstract Role getRole();

	public SessionId getSessionId() {
		return sessionId;
	}

	S getState() {
		return state;
	}

	long getRequestTimestamp() {
		return requestTimestamp;
	}

}
