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

package org.anonchatsecure.anonchat.messaging;

import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.cleanup.CleanupManager;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.validation.ValidationManager;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.anonchatsecure.anonchat.api.messaging.MessagingManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.messaging.MessagingManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.api.messaging.MessagingManager.MINOR_VERSION;

@Module
public class MessagingModule {

	public static class EagerSingletons {
		@Inject
		MessagingManager messagingManager;
		@Inject
		PrivateMessageValidator privateMessageValidator;
	}

	@Provides
	PrivateMessageFactory providePrivateMessageFactory(
			PrivateMessageFactoryImpl privateMessageFactory) {
		return privateMessageFactory;
	}

	@Provides
	@Singleton
	PrivateMessageValidator getValidator(ValidationManager validationManager,
			BdfReaderFactory bdfReaderFactory, MetadataEncoder metadataEncoder,
			Clock clock) {
		PrivateMessageValidator validator = new PrivateMessageValidator(
				bdfReaderFactory, metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				validator);
		return validator;
	}

	@Provides
	@Singleton
	MessagingManager getMessagingManager(LifecycleManager lifecycleManager,
			ContactManager contactManager, ValidationManager validationManager,
			ConversationManager conversationManager,
			ClientVersioningManager clientVersioningManager,
			CleanupManager cleanupManager, FeatureFlags featureFlags,
			MessagingManagerImpl messagingManager) {
		lifecycleManager.registerOpenDatabaseHook(messagingManager);
		contactManager.registerContactHook(messagingManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID, MAJOR_VERSION,
				messagingManager);
		conversationManager.registerConversationClient(messagingManager);
		// Don't advertise support for image attachments or disappearing
		// messages unless the respective feature flags are enabled
		boolean images = featureFlags.shouldEnableImageAttachments();
		boolean disappear = featureFlags.shouldEnableDisappearingMessages();
		int minorVersion = images ? (disappear ? MINOR_VERSION : 2) : 0;
		clientVersioningManager.registerClient(CLIENT_ID, MAJOR_VERSION,
				minorVersion, messagingManager);
		cleanupManager.registerCleanupHook(CLIENT_ID, MAJOR_VERSION,
				messagingManager);
		return messagingManager;
	}
}
