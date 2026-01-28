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

package org.anonchatsecure.anonchat.api.messaging;

import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import static java.util.Collections.emptyList;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_IMAGES_AUTO_DELETE;
import static org.anonchatsecure.anonchat.api.messaging.PrivateMessageFormat.TEXT_ONLY;

@Immutable
@NotNullByDefault
public class PrivateMessage {

	private final Message message;
	private final boolean hasText;
	private final List<AttachmentHeader> attachmentHeaders;
	private final long autoDeleteTimer;
	private final PrivateMessageFormat format;

	/**
	 * Constructor for private messages in the
	 * {@link PrivateMessageFormat#TEXT_ONLY TEXT_ONLY} format.
	 */
	public PrivateMessage(Message message) {
		this.message = message;
		hasText = true;
		attachmentHeaders = emptyList();
		autoDeleteTimer = NO_AUTO_DELETE_TIMER;
		format = TEXT_ONLY;
	}

	/**
	 * Constructor for private messages in the
	 * {@link PrivateMessageFormat#TEXT_IMAGES TEXT_IMAGES} format.
	 */
	public PrivateMessage(Message message, boolean hasText,
			List<AttachmentHeader> headers) {
		this.message = message;
		this.hasText = hasText;
		this.attachmentHeaders = headers;
		autoDeleteTimer = NO_AUTO_DELETE_TIMER;
		format = TEXT_IMAGES;
	}

	/**
	 * Constructor for private messages in the
	 * {@link PrivateMessageFormat#TEXT_IMAGES_AUTO_DELETE TEXT_IMAGES_AUTO_DELETE}
	 * format.
	 */
	public PrivateMessage(Message message, boolean hasText,
			List<AttachmentHeader> headers, long autoDeleteTimer) {
		this.message = message;
		this.hasText = hasText;
		this.attachmentHeaders = headers;
		this.autoDeleteTimer = autoDeleteTimer;
		format = TEXT_IMAGES_AUTO_DELETE;
	}

	public Message getMessage() {
		return message;
	}

	public PrivateMessageFormat getFormat() {
		return format;
	}

	public boolean hasText() {
		return hasText;
	}

	public List<AttachmentHeader> getAttachmentHeaders() {
		return attachmentHeaders;
	}

	public long getAutoDeleteTimer() {
		return autoDeleteTimer;
	}
}
