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

import org.anonchatsecure.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestPluginConfigModule;
import org.anonchatsecure.bramble.test.TestSocksModule;
import org.anonchatsecure.anonchat.autodelete.AutoDeleteModule;
import org.anonchatsecure.anonchat.avatar.AvatarModule;
import org.anonchatsecure.anonchat.client.BriarClientModule;
import org.anonchatsecure.anonchat.conversation.ConversationModule;
import org.anonchatsecure.anonchat.forum.ForumModule;
import org.anonchatsecure.anonchat.identity.IdentityModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		BriarClientModule.class,
		AutoDeleteModule.class,
		AvatarModule.class,
		ConversationModule.class,
		ForumModule.class,
		IdentityModule.class,
		MessagingModule.class,
		ModularMailboxModule.class,
		TestDnsModule.class,
		TestSocksModule.class,
		TestPluginConfigModule.class,
})
interface MessageSizeIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	void inject(MessageSizeIntegrationTest testCase);

	void inject(AvatarModule.EagerSingletons init);

	void inject(ForumModule.EagerSingletons init);

	void inject(MessagingModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(
				MessageSizeIntegrationTestComponent c) {
			BrambleCoreIntegrationTestEagerSingletons.Helper
					.injectEagerSingletons(c);
			c.inject(new AvatarModule.EagerSingletons());
			c.inject(new ForumModule.EagerSingletons());
			c.inject(new MessagingModule.EagerSingletons());
		}
	}
}
