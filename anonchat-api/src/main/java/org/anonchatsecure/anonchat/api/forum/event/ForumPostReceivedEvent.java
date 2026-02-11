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

package org.anonchatsecure.anonchat.api.forum.event;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.forum.ForumPostHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a new forum post is received.
 */
@Immutable
@NotNullByDefault
public class ForumPostReceivedEvent extends Event {

	private final GroupId groupId;
	private final ForumPostHeader header;
	private final String text;
	@Nullable
	private final byte[] audioData;
	@Nullable
	private final String audioContentType;

	public ForumPostReceivedEvent(GroupId groupId, ForumPostHeader header,
			String text) {
		this(groupId, header, text, null, null);
	}

	public ForumPostReceivedEvent(GroupId groupId, ForumPostHeader header,
			String text, @Nullable byte[] audioData,
			@Nullable String audioContentType) {
		this.groupId = groupId;
		this.header = header;
		this.text = text;
		this.audioData = audioData;
		this.audioContentType = audioContentType;
	}

	public GroupId getGroupId() {
		return groupId;
	}

	public ForumPostHeader getHeader() {
		return header;
	}

	public String getText() {
		return text;
	}

	@Nullable
	public byte[] getAudioData() {
		return audioData;
	}

	@Nullable
	public String getAudioContentType() {
		return audioContentType;
	}
}
