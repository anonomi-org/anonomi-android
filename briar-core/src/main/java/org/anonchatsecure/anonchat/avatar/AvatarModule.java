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

package org.anonchatsecure.anonchat.avatar;

import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.validation.ValidationManager;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.avatar.AvatarManager;
import org.anonchatsecure.anonchat.api.avatar.AvatarMessageEncoder;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.anonchatsecure.anonchat.api.avatar.AvatarManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.avatar.AvatarManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.api.avatar.AvatarManager.MINOR_VERSION;

@Module
public class AvatarModule {

	public static class EagerSingletons {
		@Inject
		AvatarValidator avatarValidator;
		@Inject
		AvatarManager avatarManager;
	}

	@Provides
	@Singleton
	AvatarValidator provideAvatarValidator(ValidationManager validationManager,
			BdfReaderFactory bdfReaderFactory, MetadataEncoder metadataEncoder,
			Clock clock) {
		AvatarValidator avatarValidator =
				new AvatarValidator(bdfReaderFactory, metadataEncoder, clock);
		validationManager.registerMessageValidator(CLIENT_ID, MAJOR_VERSION,
				avatarValidator);
		return avatarValidator;
	}

	@Provides
	@Singleton
	AvatarMessageEncoder provideMessageEncoder(
			AvatarMessageEncoderImpl messageEncoder) {
		return messageEncoder;
	}

	@Provides
	@Singleton
	AvatarManager provideAvatarManager(
			LifecycleManager lifecycleManager,
			ContactManager contactManager,
			ValidationManager validationManager,
			ClientVersioningManager clientVersioningManager,
			AvatarManagerImpl avatarManager) {
		lifecycleManager.registerOpenDatabaseHook(avatarManager);
		contactManager.registerContactHook(avatarManager);
		validationManager.registerIncomingMessageHook(CLIENT_ID,
				MAJOR_VERSION, avatarManager);
		clientVersioningManager.registerClient(CLIENT_ID,
				MAJOR_VERSION, MINOR_VERSION, avatarManager);
		return avatarManager;
	}

}
