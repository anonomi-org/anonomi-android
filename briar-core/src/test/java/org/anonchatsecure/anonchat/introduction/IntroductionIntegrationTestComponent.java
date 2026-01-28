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

package org.anonchatsecure.anonchat.introduction;

import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestPluginConfigModule;
import org.anonchatsecure.bramble.test.TestSocksModule;
import org.anonchatsecure.anonchat.attachment.AttachmentModule;
import org.anonchatsecure.anonchat.autodelete.AutoDeleteModule;
import org.anonchatsecure.anonchat.avatar.AvatarModule;
import org.anonchatsecure.anonchat.blog.BlogModule;
import org.anonchatsecure.anonchat.client.BriarClientModule;
import org.anonchatsecure.anonchat.conversation.ConversationModule;
import org.anonchatsecure.anonchat.forum.ForumModule;
import org.anonchatsecure.anonchat.identity.IdentityModule;
import org.anonchatsecure.anonchat.messaging.MessagingModule;
import org.anonchatsecure.anonchat.privategroup.PrivateGroupModule;
import org.anonchatsecure.anonchat.privategroup.invitation.GroupInvitationModule;
import org.anonchatsecure.anonchat.sharing.SharingModule;
import org.anonchatsecure.anonchat.test.BriarIntegrationTestComponent;

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
interface IntroductionIntegrationTestComponent
		extends BriarIntegrationTestComponent {

	void inject(IntroductionIntegrationTest init);

	void inject(MessageEncoderParserIntegrationTest init);

	void inject(SessionEncoderParserIntegrationTest init);

	void inject(IntroductionCryptoIntegrationTest init);

	void inject(AutoDeleteIntegrationTest init);

	MessageEncoder getMessageEncoder();

	MessageParser getMessageParser();

	SessionParser getSessionParser();

	IntroductionCrypto getIntroductionCrypto();

}
