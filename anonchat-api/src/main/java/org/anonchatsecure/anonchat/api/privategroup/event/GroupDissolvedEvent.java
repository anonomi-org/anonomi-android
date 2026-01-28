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

package org.anonchatsecure.anonchat.api.privategroup.event;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a private group is dissolved by a remote
 * creator.
 */
@Immutable
@NotNullByDefault
public class GroupDissolvedEvent extends Event {

	private final GroupId groupId;

	public GroupDissolvedEvent(GroupId groupId) {
		this.groupId = groupId;
	}

	public GroupId getGroupId() {
		return groupId;
	}

}
