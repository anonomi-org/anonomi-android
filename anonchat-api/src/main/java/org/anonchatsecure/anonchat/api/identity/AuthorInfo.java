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

package org.anonchatsecure.anonchat.api.identity;

import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;
import org.briarproject.nullsafety.NullSafety;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class AuthorInfo {

	public enum Status {
		NONE, UNKNOWN, UNVERIFIED, VERIFIED, OURSELVES;

		public boolean isContact() {
			return this == UNVERIFIED || this == VERIFIED;
		}
	}

	private final Status status;
	@Nullable
	private final String alias;
	@Nullable
	private final AttachmentHeader avatarHeader;

	public AuthorInfo(Status status, @Nullable String alias,
			@Nullable AttachmentHeader avatarHeader) {
		this.status = status;
		this.alias = alias;
		this.avatarHeader = avatarHeader;
	}

	public AuthorInfo(Status status) {
		this(status, null, null);
	}

	public Status getStatus() {
		return status;
	}

	@Nullable
	public String getAlias() {
		return alias;
	}

	@Nullable
	public AttachmentHeader getAvatarHeader() {
		return avatarHeader;
	}

	@Override
	public int hashCode() {
		int hashCode = status.ordinal();
		if (alias != null) hashCode += alias.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AuthorInfo)) return false;
		AuthorInfo info = (AuthorInfo) o;
		return status == info.status &&
				// aliases are equal
				NullSafety.equals(alias, info.alias) &&
				// avatars are equal
				NullSafety.equals(avatarHeader, info.avatarHeader);
	}
}
