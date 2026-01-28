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

package org.anonchatsecure.anonchat.api.test;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface TestDataCreator {

	/**
	 * Create fake test data on the IoExecutor
	 *
	 * @param numContacts Number of contacts to create. Must be >= 1
	 * @param numPrivateMsgs Number of private messages to create for each
	 * contact.
	 * @param avatarPercent Percentage of contacts
	 * that will use a random profile image. Between 0 and 100.
	 * @param numBlogPosts Number of blog posts to create.
	 * @param numForums Number of forums to create.
	 * @param numForumPosts Number of forum posts to create per forum.
	 * @param numPrivateGroups Number of private groups to create.
	 * @param numPrivateGroupMessages Number of messages to create per private group.
	 */
	void createTestData(int numContacts, int numPrivateMsgs, int avatarPercent,
			int numBlogPosts, int numForums, int numForumPosts,
			int numPrivateGroups, int numPrivateGroupMessages);

	@IoExecutor
	Contact addContact(String name, boolean alias, boolean avatar)
			throws DbException;
}
