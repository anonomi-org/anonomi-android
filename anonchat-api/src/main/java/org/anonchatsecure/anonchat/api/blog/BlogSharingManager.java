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

import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.anonchat.api.sharing.SharingManager;

public interface BlogSharingManager extends SharingManager<Blog> {

	/**
	 * The unique ID of the blog sharing client.
	 */
	ClientId CLIENT_ID = new ClientId("org.anonchatsecure.anonchat.blog.sharing");

	/**
	 * The current major version of the blog sharing client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the blog sharing client.
	 */
	int MINOR_VERSION = 1;
}
