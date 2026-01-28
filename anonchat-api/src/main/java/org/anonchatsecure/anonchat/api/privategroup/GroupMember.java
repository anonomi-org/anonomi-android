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

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class GroupMember {

	private final Author author;
	private final AuthorInfo authorInfo;
	private final boolean isCreator;
	@Nullable
	private final ContactId contactId;
	private final Visibility visibility;

	public GroupMember(Author author, AuthorInfo authorInfo, boolean isCreator,
			@Nullable ContactId contactId, Visibility visibility) {
		this.author = author;
		this.authorInfo = authorInfo;
		this.isCreator = isCreator;
		this.contactId = contactId;
		this.visibility = visibility;
	}

	public Author getAuthor() {
		return author;
	}

	public AuthorInfo getAuthorInfo() {
		return authorInfo;
	}

	public boolean isCreator() {
		return isCreator;
	}

	/**
	 * Returns the ContactId of a visible contact
	 * or null if the contact is not visible or the member is no contact.
	 */
	@Nullable
	public ContactId getContactId() {
		return contactId;
	}

	public Visibility getVisibility() {
		return visibility;
	}

}
