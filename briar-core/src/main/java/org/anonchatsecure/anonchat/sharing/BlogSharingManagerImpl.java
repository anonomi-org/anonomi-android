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
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.data.MetadataParser;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogInvitationResponse;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogManager.RemoveBlogHook;
import org.anonchatsecure.anonchat.api.blog.BlogSharingManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class BlogSharingManagerImpl extends SharingManagerImpl<Blog>
		implements BlogSharingManager, RemoveBlogHook {

	private final IdentityManager identityManager;
	private final BlogManager blogManager;

	@Inject
	BlogSharingManagerImpl(DatabaseComponent db, ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			MetadataParser metadataParser, MessageParser<Blog> messageParser,
			SessionEncoder sessionEncoder, SessionParser sessionParser,
			MessageTracker messageTracker,
			ContactGroupFactory contactGroupFactory,
			ProtocolEngine<Blog> engine,
			InvitationFactory<Blog, BlogInvitationResponse> invitationFactory,
			IdentityManager identityManager, BlogManager blogManager) {
		super(db, clientHelper, clientVersioningManager, metadataParser,
				messageParser, sessionEncoder, sessionParser, messageTracker,
				contactGroupFactory, engine, invitationFactory);
		this.identityManager = identityManager;
		this.blogManager = blogManager;
	}

	@Override
	protected ClientId getClientId() {
		return CLIENT_ID;
	}

	@Override
	protected int getMajorVersion() {
		return MAJOR_VERSION;
	}

	@Override
	protected ClientId getShareableClientId() {
		return BlogManager.CLIENT_ID;
	}

	@Override
	protected int getShareableMajorVersion() {
		return BlogManager.MAJOR_VERSION;
	}

	@Override
	public void addingContact(Transaction txn, Contact c) throws DbException {
		// Create a group to share with the contact
		super.addingContact(txn, c);

		// Get our blog and that of the contact
		LocalAuthor localAuthor = identityManager.getLocalAuthor(txn);
		Blog ourBlog = blogManager.getPersonalBlog(localAuthor);
		Blog theirBlog = blogManager.getPersonalBlog(c.getAuthor());

		// Pre-share both blogs, if they have not been shared already
		try {
			preShareGroup(txn, c, ourBlog.getGroup());
			preShareGroup(txn, c, theirBlog.getGroup());
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removingBlog(Transaction txn, Blog b) throws DbException {
		removingShareable(txn, b);
	}

}
