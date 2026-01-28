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

import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.NotThreadSafe;

import static org.anonchatsecure.anonchat.api.conversation.ConversationManager.DELETE_SESSION_INTRODUCTION_INCOMPLETE;
import static org.anonchatsecure.anonchat.api.conversation.ConversationManager.DELETE_SESSION_INTRODUCTION_IN_PROGRESS;
import static org.anonchatsecure.anonchat.api.conversation.ConversationManager.DELETE_SESSION_INVITATION_INCOMPLETE;
import static org.anonchatsecure.anonchat.api.conversation.ConversationManager.DELETE_SESSION_INVITATION_IN_PROGRESS;

@NotThreadSafe
@NotNullByDefault
public class DeletionResult {

	private int result = 0;

	public void addDeletionResult(DeletionResult deletionResult) {
		result |= deletionResult.result;
	}

	public void addInvitationNotAllSelected() {
		result |= DELETE_SESSION_INVITATION_INCOMPLETE;
	}

	public void addInvitationSessionInProgress() {
		result |= DELETE_SESSION_INVITATION_IN_PROGRESS;
	}

	public void addIntroductionNotAllSelected() {
		result |= DELETE_SESSION_INTRODUCTION_INCOMPLETE;
	}

	public void addIntroductionSessionInProgress() {
		result |= DELETE_SESSION_INTRODUCTION_IN_PROGRESS;
	}

	public boolean allDeleted() {
		return result == 0;
	}

	public boolean hasIntroductionSessionInProgress() {
		return (result & DELETE_SESSION_INTRODUCTION_IN_PROGRESS) != 0;
	}

	public boolean hasInvitationSessionInProgress() {
		return (result & DELETE_SESSION_INVITATION_IN_PROGRESS) != 0;
	}

	public boolean hasNotAllIntroductionSelected() {
		return (result & DELETE_SESSION_INTRODUCTION_INCOMPLETE) != 0;
	}

	public boolean hasNotAllInvitationSelected() {
		return (result & DELETE_SESSION_INVITATION_INCOMPLETE) != 0;
	}
}
