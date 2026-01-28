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

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.data.MetadataParser;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumManager.RemoveForumHook;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.inject.Inject;

@NotNullByDefault
class ForumSharingManagerImpl extends SharingManagerImpl<Forum>
		implements ForumSharingManager, RemoveForumHook {

	@Inject
	ForumSharingManagerImpl(DatabaseComponent db, ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			MetadataParser metadataParser, MessageParser<Forum> messageParser,
			SessionEncoder sessionEncoder, SessionParser sessionParser,
			MessageTracker messageTracker,
			ContactGroupFactory contactGroupFactory,
			ProtocolEngine<Forum> engine,
			InvitationFactory<Forum, ForumInvitationResponse> invitationFactory) {
		super(db, clientHelper, clientVersioningManager, metadataParser,
				messageParser, sessionEncoder, sessionParser, messageTracker,
				contactGroupFactory, engine, invitationFactory);
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
		return ForumManager.CLIENT_ID;
	}

	@Override
	protected int getShareableMajorVersion() {
		return ForumManager.MAJOR_VERSION;
	}

	@Override
	public void removingForum(Transaction txn, Forum f) throws DbException {
		removingShareable(txn, f);
	}

}
