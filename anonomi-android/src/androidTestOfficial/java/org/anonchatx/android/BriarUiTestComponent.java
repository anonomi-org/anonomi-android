package org.anonomi.android;

import org.anonchatsecure.bramble.BrambleAndroidModule;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.account.BriarAccountModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.plugin.file.RemovableDriveModule;
import org.anonchatsecure.bramble.system.ClockModule;
import org.anonchatsecure.anonchat.BriarCoreModule;
import org.anonomi.android.account.SignInTestCreateAccount;
import org.anonomi.android.account.SignInTestSignIn;
import org.anonomi.android.attachment.AttachmentModule;
import org.anonomi.android.attachment.media.MediaModule;
import org.anonomi.android.navdrawer.NavDrawerActivityTest;

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

	void inject(NavDrawerActivityTest test);

	void inject(SignInTestCreateAccount test);

	void inject(SignInTestSignIn test);

}
