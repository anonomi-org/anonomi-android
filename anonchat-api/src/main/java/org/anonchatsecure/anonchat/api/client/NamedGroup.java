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

package org.anonchatsecure.anonchat.api.client;

import org.anonchatsecure.bramble.api.sync.Group;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public abstract class NamedGroup extends BaseGroup {

	private final String name;
	private final byte[] salt;

	public NamedGroup(Group group, String name, byte[] salt) {
		super(group);
		this.name = name;
		this.salt = salt;
	}

	public String getName() {
		return name;
	}

	public byte[] getSalt() {
		return salt;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof NamedGroup && super.equals(o);
	}

}
