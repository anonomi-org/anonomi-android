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

package org.anonchatsecure.anonchat.api.sharing;

import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationRequest;

import javax.annotation.Nullable;

public abstract class InvitationRequest<S extends Shareable> extends
		ConversationRequest<S> {

	private final boolean canBeOpened;

	public InvitationRequest(MessageId messageId, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, S object, @Nullable String text,
			boolean available, boolean canBeOpened, long autoDeleteTimer) {
		super(messageId, groupId, time, local, read, sent, seen, sessionId,
				object, text, !available, autoDeleteTimer);
		this.canBeOpened = canBeOpened;
	}

	public boolean canBeOpened() {
		return canBeOpened;
	}
}
