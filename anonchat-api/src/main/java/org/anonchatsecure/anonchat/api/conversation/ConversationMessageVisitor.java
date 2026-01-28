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

package org.anonchatsecure.anonchat.api.conversation;

import org.anonchatsecure.anonchat.api.blog.BlogInvitationRequest;
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationRequest;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse;
import org.anonchatsecure.anonchat.api.introduction.IntroductionRequest;
import org.anonchatsecure.anonchat.api.introduction.IntroductionResponse;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageHeader;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationRequest;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface ConversationMessageVisitor<T> {

	T visitPrivateMessageHeader(PrivateMessageHeader h);

	T visitBlogInvitationRequest(BlogInvitationRequest r);

	T visitBlogInvitationResponse(BlogInvitationResponse r);

	T visitForumInvitationRequest(ForumInvitationRequest r);

	T visitForumInvitationResponse(ForumInvitationResponse r);

	T visitGroupInvitationRequest(GroupInvitationRequest r);

	T visitGroupInvitationResponse(GroupInvitationResponse r);

	T visitIntroductionRequest(IntroductionRequest r);

	T visitIntroductionResponse(IntroductionResponse r);
}
