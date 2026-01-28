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

package org.anonchatsecure.anonchat.test;

import org.anonchatsecure.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.identity.AuthorFactory;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.properties.TransportPropertyManager;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.BrambleIntegrationTestComponent;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestPluginConfigModule;
import org.anonchatsecure.bramble.test.TestSocksModule;
import org.anonchatsecure.bramble.test.TimeTravel;
import org.anonchatsecure.anonchat.api.attachment.AttachmentReader;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.avatar.AvatarManager;
import org.anonchatsecure.anonchat.api.blog.BlogFactory;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogSharingManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.introduction.IntroductionManager;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;
import org.anonchatsecure.anonchat.attachment.AttachmentModule;
import org.anonchatsecure.anonchat.autodelete.AutoDeleteModule;
import org.anonchatsecure.anonchat.avatar.AvatarModule;
import org.anonchatsecure.anonchat.blog.BlogModule;
import org.anonchatsecure.anonchat.client.BriarClientModule;
import org.anonchatsecure.anonchat.conversation.ConversationModule;
import org.anonchatsecure.anonchat.forum.ForumModule;
import org.anonchatsecure.anonchat.identity.IdentityModule;
import org.anonchatsecure.anonchat.introduction.IntroductionModule;
import org.anonchatsecure.anonchat.messaging.MessagingModule;
import org.anonchatsecure.anonchat.privategroup.PrivateGroupModule;
import org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationModule;
import org.anonchatsecure.anonchat.sharing.SharingModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		AttachmentModule.class,
		AutoDeleteModule.class,
		AvatarModule.class,
		BlogModule.class,
		BriarClientModule.class,
		ConversationModule.class,
		ForumModule.class,
		GroupInvitationModule.class,
		IdentityModule.class,
		IntroductionModule.class,
		MessagingModule.class,
		PrivateGroupModule.class,
		SharingModule.class,
		ModularMailboxModule.class,
		TestDnsModule.class,
		TestSocksModule.class,
		TestPluginConfigModule.class,
})
public interface BriarIntegrationTestComponent
		extends BrambleIntegrationTestComponent {

	void inject(BriarIntegrationTest<BriarIntegrationTestComponent> init);

	void inject(AutoDeleteModule.EagerSingletons init);

	void inject(AvatarModule.EagerSingletons init);

	void inject(BlogModule.EagerSingletons init);

	void inject(ConversationModule.EagerSingletons init);

	void inject(ForumModule.EagerSingletons init);

	void inject(GroupInvitationModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(IntroductionModule.EagerSingletons init);

	void inject(MessagingModule.EagerSingletons init);

	void inject(PrivateGroupModule.EagerSingletons init);

	void inject(SharingModule.EagerSingletons init);

	LifecycleManager getLifecycleManager();

	AttachmentReader getAttachmentReader();

	AvatarManager getAvatarManager();

	ContactManager getContactManager();

	ConversationManager getConversationManager();

	DatabaseComponent getDatabaseComponent();

	BlogManager getBlogManager();

	BlogSharingManager getBlogSharingManager();

	ForumSharingManager getForumSharingManager();

	ForumManager getForumManager();

	GroupInvitationManager getGroupInvitationManager();

	GroupInvitationFactory getGroupInvitationFactory();

	IntroductionManager getIntroductionManager();

	MessageTracker getMessageTracker();

	MessagingManager getMessagingManager();

	PrivateGroupManager getPrivateGroupManager();

	PrivateMessageFactory getPrivateMessageFactory();

	TransportPropertyManager getTransportPropertyManager();

	AuthorFactory getAuthorFactory();

	BlogFactory getBlogFactory();

	AutoDeleteManager getAutoDeleteManager();

	Clock getClock();

	TimeTravel getTimeTravel();

	class Helper {

		public static void injectEagerSingletons(
				BriarIntegrationTestComponent c) {
			BrambleCoreIntegrationTestEagerSingletons.Helper
					.injectEagerSingletons(c);
			c.inject(new AutoDeleteModule.EagerSingletons());
			c.inject(new AvatarModule.EagerSingletons());
			c.inject(new BlogModule.EagerSingletons());
			c.inject(new ConversationModule.EagerSingletons());
			c.inject(new ForumModule.EagerSingletons());
			c.inject(new GroupInvitationModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new IntroductionModule.EagerSingletons());
			c.inject(new MessagingModule.EagerSingletons());
			c.inject(new PrivateGroupModule.EagerSingletons());
			c.inject(new SharingModule.EagerSingletons());
		}
	}
}
