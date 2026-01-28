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

import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.cleanup.CleanupManager;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.validation.ValidationManager;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager.MINOR_VERSION;

@Module
public class GroupInvitationModule {

	public static class EagerSingletons {
		@Inject
		GroupInvitationValidator groupInvitationValidator;
		@Inject
		GroupInvitationManager groupInvitationManager;
	}

	@Provides
	@Singleton
	GroupInvitationManager provideGroupInvitationManager(
			GroupInvitationManagerImpl groupInvitationManager,
			LifecycleManager lifecycleManager,
			ValidationManager validationManager, ContactManager contactManager,
			PrivateGroupManager privateGroupManager,
			ConversationManager conversationManager,
			ClientVersioningManager clientVersioningManager,
			CleanupManager cleanupManager, FeatureFlags featureFlags) {
		if (!featureFlags.shouldEnablePrivateGroupsInCore()) {
			return groupInvitationManager;
		}
		lifecycleManager.registerOpenDatabaseHook(groupInvitationManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				groupInvitationManager);
		contactManager.registerContactHook(groupInvitationManager);
		privateGroupManager.registerPrivateGroupHook(groupInvitationManager);
		conversationManager.registerConversationClient(groupInvitationManager);
		clientVersioningManager.registerClient(CLIENT_ID, MAJOR_VERSION,
				MINOR_VERSION, groupInvitationManager);
		// The group invitation manager handles client visibility changes for
		// the private group manager
		clientVersioningManager.registerClient(PrivateGroupManager.CLIENT_ID,
				PrivateGroupManager.MAJOR_VERSION,
				PrivateGroupManager.MINOR_VERSION,
				groupInvitationManager.getPrivateGroupClientVersioningHook());
		cleanupManager.registerCleanupHook(CLIENT_ID, MAJOR_VERSION,
				groupInvitationManager);
		return groupInvitationManager;
	}

	@Provides
	@Singleton
	GroupInvitationValidator provideGroupInvitationValidator(
			ClientHelper clientHelper, MetadataEncoder metadataEncoder,
			Clock clock, PrivateGroupFactory privateGroupFactory,
			MessageEncoder messageEncoder,
			ValidationManager validationManager,
			FeatureFlags featureFlags) {
		GroupInvitationValidator validator = new GroupInvitationValidator(
				clientHelper, metadataEncoder, clock, privateGroupFactory,
				messageEncoder);
		if (featureFlags.shouldEnablePrivateGroupsInCore()) {
			validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
					validator);
		}
		return validator;
	}

	@Provides
	GroupInvitationFactory provideGroupInvitationFactory(
			GroupInvitationFactoryImpl groupInvitationFactory) {
		return groupInvitationFactory;
	}

	@Provides
	MessageParser provideMessageParser(MessageParserImpl messageParser) {
		return messageParser;
	}

	@Provides
	MessageEncoder provideMessageEncoder(MessageEncoderImpl messageEncoder) {
		return messageEncoder;
	}

	@Provides
	SessionParser provideSessionParser(SessionParserImpl sessionParser) {
		return sessionParser;
	}

	@Provides
	SessionEncoder provideSessionEncoder(SessionEncoderImpl sessionEncoder) {
		return sessionEncoder;
	}

	@Provides
	ProtocolEngineFactory provideProtocolEngineFactory(
			ProtocolEngineFactoryImpl protocolEngineFactory) {
		return protocolEngineFactory;
	}
}
