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
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_INVITE_TIMESTAMP;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_IS_SESSION;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_LAST_LOCAL_MESSAGE_ID;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_LAST_REMOTE_MESSAGE_ID;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_LOCAL_TIMESTAMP;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_SESSION_ID;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_SHAREABLE_ID;
import static org.anonchatsecure.anonchat.sharing.SharingConstants.SESSION_KEY_STATE;

@Immutable
@NotNullByDefault
class SessionParserImpl implements SessionParser {

	@Inject
	SessionParserImpl() {
	}

	@Override
	public BdfDictionary getSessionQuery(SessionId s) {
		return BdfDictionary.of(new BdfEntry(SESSION_KEY_SESSION_ID, s));
	}

	@Override
	public BdfDictionary getAllSessionsQuery() {
		return BdfDictionary.of(new BdfEntry(SESSION_KEY_IS_SESSION, true));
	}

	@Override
	public boolean isSession(BdfDictionary d) throws FormatException {
		return d.getBoolean(SESSION_KEY_IS_SESSION, false);
	}

	@Override
	public Session parseSession(GroupId contactGroupId,
			BdfDictionary d) throws FormatException {
		return new Session(State.fromValue(getState(d)), contactGroupId,
				getShareableId(d), getLastLocalMessageId(d),
				getLastRemoteMessageId(d), getLocalTimestamp(d),
				getInviteTimestamp(d));
	}

	private int getState(BdfDictionary d) throws FormatException {
		return d.getInt(SESSION_KEY_STATE);
	}

	private GroupId getShareableId(BdfDictionary d) throws FormatException {
		return new GroupId(d.getRaw(SESSION_KEY_SHAREABLE_ID));
	}

	@Nullable
	private MessageId getLastLocalMessageId(BdfDictionary d)
			throws FormatException {
		byte[] b = d.getOptionalRaw(SESSION_KEY_LAST_LOCAL_MESSAGE_ID);
		return b == null ? null : new MessageId(b);
	}

	@Nullable
	private MessageId getLastRemoteMessageId(BdfDictionary d)
			throws FormatException {
		byte[] b = d.getOptionalRaw(SESSION_KEY_LAST_REMOTE_MESSAGE_ID);
		return b == null ? null : new MessageId(b);
	}

	private long getLocalTimestamp(BdfDictionary d) throws FormatException {
		return d.getLong(SESSION_KEY_LOCAL_TIMESTAMP);
	}

	private long getInviteTimestamp(BdfDictionary d) throws FormatException {
		return d.getLong(SESSION_KEY_INVITE_TIMESTAMP);
	}

}
