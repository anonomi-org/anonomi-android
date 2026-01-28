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

package org.anonchatsecure.anonchat.api.introduction;

import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationMessageVisitor;
import org.anonchatsecure.anonchat.api.conversation.ConversationResponse;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.anonchat.api.introduction.Role.INTRODUCER;

@Immutable
@NotNullByDefault
public class IntroductionResponse extends ConversationResponse {

	private final Author introducedAuthor;
	private final AuthorInfo introducedAuthorInfo;
	private final Role ourRole;
	private final boolean canSucceed;

	public IntroductionResponse(MessageId messageId, GroupId groupId, long time,
			boolean local, boolean read, boolean sent, boolean seen,
			SessionId sessionId, boolean accepted, Author author,
			AuthorInfo introducedAuthorInfo, Role role, boolean canSucceed,
			long autoDeleteTimer, boolean isAutoDecline) {
		super(messageId, groupId, time, local, read, sent, seen, sessionId,
				accepted, autoDeleteTimer, isAutoDecline);
		this.introducedAuthor = author;
		this.introducedAuthorInfo = introducedAuthorInfo;
		this.ourRole = role;
		this.canSucceed = canSucceed;
	}

	public Author getIntroducedAuthor() {
		return introducedAuthor;
	}

	public AuthorInfo getIntroducedAuthorInfo() {
		return introducedAuthorInfo;
	}

	public boolean canSucceed() {
		return canSucceed;
	}

	public boolean isIntroducer() {
		return ourRole == INTRODUCER;
	}

	@Override
	public <T> T accept(ConversationMessageVisitor<T> v) {
		return v.visitIntroductionResponse(this);
	}

}
