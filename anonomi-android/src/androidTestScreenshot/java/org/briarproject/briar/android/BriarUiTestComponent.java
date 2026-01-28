package org.anonomi.android;

import org.briarproject.bramble.BrambleAndroidModule;
import org.briarproject.bramble.BrambleCoreModule;
import org.briarproject.bramble.account.BriarAccountModule;
import org.briarproject.bramble.mailbox.ModularMailboxModule;
import org.briarproject.bramble.plugin.file.RemovableDriveModule;
import org.briarproject.bramble.system.ClockModule;
import org.anonchatsecure.anonchat.BriarCoreModule;
import org.anonomi.android.attachment.AttachmentModule;
import org.anonomi.android.attachment.media.MediaModule;
import org.anonomi.android.conversation.ConversationActivityScreenshotTest;
import org.anonomi.android.settings.SettingsActivityScreenshotTest;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		AppModule.class,
		AttachmentModule.class,
		ClockModule.class,
		MediaModule.class,
		RemovableDriveModule.class,
		BriarCoreModule.class,
		BrambleAndroidModule.class,
		BriarAccountModule.class,
		BrambleCoreModule.class,
		ModularMailboxModule.class
})
public interface BriarUiTestComponent extends AndroidComponent {

	void inject(SetupDataTest test);

	void inject(ConversationActivityScreenshotTest test);

	void inject(SettingsActivityScreenshotTest test);

	void inject(PromoVideoTest test);

}
