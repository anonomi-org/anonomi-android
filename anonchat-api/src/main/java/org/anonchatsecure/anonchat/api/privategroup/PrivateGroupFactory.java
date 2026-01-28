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

package org.anonchatsecure.anonchat.api.privategroup;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface PrivateGroupFactory {

	/**
	 * Creates a private group with the given name and author.
	 */
	PrivateGroup createPrivateGroup(String name, Author creator);

	/**
	 * Creates a private group with the given name, author and salt.
	 */
	PrivateGroup createPrivateGroup(String name, Author creator, byte[] salt);

	/**
	 * Parses a group and returns the corresponding PrivateGroup.
	 */
	PrivateGroup parsePrivateGroup(Group group) throws FormatException;

}
