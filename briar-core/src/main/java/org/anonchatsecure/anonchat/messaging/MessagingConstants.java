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

package org.anonchatsecure.anonchat.messaging;

import static java.util.concurrent.TimeUnit.DAYS;

interface MessagingConstants {

	// Metadata keys for messages
	String MSG_KEY_TIMESTAMP = "timestamp";
	String MSG_KEY_LOCAL = "local";
	String MSG_KEY_MSG_TYPE = "messageType";
	String MSG_KEY_HAS_TEXT = "hasText";
	String MSG_KEY_ATTACHMENT_HEADERS = "attachmentHeaders";
	String MSG_KEY_AUTO_DELETE_TIMER = "autoDeleteTimer";

	/**
	 * How long to keep incoming attachments that aren't listed by any private
	 * message before deleting them.
	 */
	long MISSING_ATTACHMENT_CLEANUP_DURATION_MS = DAYS.toMillis(28);
}
