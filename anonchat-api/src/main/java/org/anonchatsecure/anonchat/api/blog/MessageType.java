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

package org.anonchatsecure.anonchat.api.blog;

public enum MessageType {

	POST(0),
	COMMENT(1),
	WRAPPED_POST(2),
	WRAPPED_COMMENT(3);

	int value;

	MessageType(int value) {
		this.value = value;
	}

	public static MessageType valueOf(int value) {
		switch (value) {
			case 0:
				return POST;
			case 1:
				return COMMENT;
			case 2:
				return WRAPPED_POST;
			case 3:
				return WRAPPED_COMMENT;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int getInt() {
		return value;
	}
}