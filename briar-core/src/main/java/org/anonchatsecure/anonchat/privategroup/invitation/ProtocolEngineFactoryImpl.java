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

package org.anonchatsecure.anonchat.privategroup.invitation;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
class ProtocolEngineFactoryImpl implements ProtocolEngineFactory {

	private final DatabaseComponent db;
	private final ClientHelper clientHelper;
	private final ClientVersioningManager clientVersioningManager;
	private final PrivateGroupManager privateGroupManager;
	private final PrivateGroupFactory privateGroupFactory;
	private final GroupMessageFactory groupMessageFactory;
	private final IdentityManager identityManager;
	private final MessageParser messageParser;
	private final MessageEncoder messageEncoder;
	private final AutoDeleteManager autoDeleteManager;
	private final ConversationManager conversationManager;
	private final Clock clock;

	@Inject
	ProtocolEngineFactoryImpl(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			PrivateGroupManager privateGroupManager,
			PrivateGroupFactory privateGroupFactory,
			GroupMessageFactory groupMessageFactory,
			IdentityManager identityManager,
			MessageParser messageParser,
			MessageEncoder messageEncoder,
			AutoDeleteManager autoDeleteManager,
			ConversationManager conversationManager,
			Clock clock) {
		this.db = db;
		this.clientHelper = clientHelper;
		this.clientVersioningManager = clientVersioningManager;
		this.privateGroupManager = privateGroupManager;
		this.privateGroupFactory = privateGroupFactory;
		this.groupMessageFactory = groupMessageFactory;
		this.identityManager = identityManager;
		this.messageParser = messageParser;
		this.messageEncoder = messageEncoder;
		this.autoDeleteManager = autoDeleteManager;
		this.conversationManager = conversationManager;
		this.clock = clock;
	}

	@Override
	public ProtocolEngine<CreatorSession> createCreatorEngine() {
		return new CreatorProtocolEngine(db, clientHelper,
				clientVersioningManager, privateGroupManager,
				privateGroupFactory, groupMessageFactory, identityManager,
				messageParser, messageEncoder,
				autoDeleteManager, conversationManager, clock);
	}

	@Override
	public ProtocolEngine<InviteeSession> createInviteeEngine() {
		return new InviteeProtocolEngine(db, clientHelper,
				clientVersioningManager, privateGroupManager,
				privateGroupFactory, groupMessageFactory, identityManager,
				messageParser, messageEncoder,
				autoDeleteManager, conversationManager, clock);
	}

	@Override
	public ProtocolEngine<PeerSession> createPeerEngine() {
		return new PeerProtocolEngine(db, clientHelper,
				clientVersioningManager, privateGroupManager,
				privateGroupFactory, groupMessageFactory, identityManager,
				messageParser, messageEncoder,
				autoDeleteManager, conversationManager, clock);
	}
}
