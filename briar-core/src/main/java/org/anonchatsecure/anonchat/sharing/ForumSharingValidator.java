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

package org.anonchatsecure.anonchat.sharing;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.anonchatsecure.anonchat.api.forum.ForumFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.util.ValidationUtils.checkLength;
import static org.anonchatsecure.bramble.util.ValidationUtils.checkSize;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.FORUM_SALT_LENGTH;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_NAME_LENGTH;

@Immutable
@NotNullByDefault
class ForumSharingValidator extends SharingValidator {

	private final ForumFactory forumFactory;

	@Inject
	ForumSharingValidator(MessageEncoder messageEncoder,
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, ForumFactory forumFactory) {
		super(messageEncoder, clientHelper, metadataEncoder, clock);
		this.forumFactory = forumFactory;
	}

	@Override
	protected GroupId validateDescriptor(BdfList descriptor)
			throws FormatException {
		// Name, salt
		checkSize(descriptor, 2);
		String name = descriptor.getString(0);
		checkLength(name, 1, MAX_FORUM_NAME_LENGTH);
		byte[] salt = descriptor.getRaw(1);
		checkLength(salt, FORUM_SALT_LENGTH);
		Forum forum = forumFactory.createForum(name, salt);
		return forum.getId();
	}

}
