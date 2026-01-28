package org.anonchatsecure.bramble.plugin.file;

import org.anonchatsecure.bramble.BrambleCoreEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.plugin.file.RemovableDriveManager;
import org.anonchatsecure.bramble.battery.DefaultBatteryManagerModule;
import org.anonchatsecure.bramble.event.DefaultEventExecutorModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.system.DefaultThreadFactoryModule;
import org.anonchatsecure.bramble.system.DefaultWakefulIoExecutorModule;
import org.anonchatsecure.bramble.system.TimeTravelModule;
import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestFeatureFlagModule;
import org.anonchatsecure.bramble.test.TestMailboxDirectoryModule;
import org.anonchatsecure.bramble.test.TestSecureRandomModule;
import org.anonchatsecure.bramble.test.TestSocksModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreModule.class,
		DefaultBatteryManagerModule.class,
		DefaultEventExecutorModule.class,
		DefaultWakefulIoExecutorModule.class,
		DefaultThreadFactoryModule.class,
		TestDatabaseConfigModule.class,
		TestDnsModule.class,
		TestFeatureFlagModule.class,
		TestMailboxDirectoryModule.class,
		RemovableDriveIntegrationTestModule.class,
		RemovableDriveModule.class,
		ModularMailboxModule.class,
		TestSecureRandomModule.class,
		TimeTravelModule.class,
		TestSocksModule.class,
})
interface RemovableDriveIntegrationTestComponent
		extends BrambleCoreEagerSingletons {

	ContactManager getContactManager();

	EventBus getEventBus();

	IdentityManager getIdentityManager();

	LifecycleManager getLifecycleManager();

	RemovableDriveManager getRemovableDriveManager();

	class Helper {

		public static void injectEagerSingletons(
				RemovableDriveIntegrationTestComponent c) {
			BrambleCoreEagerSingletons.Helper.injectEagerSingletons(c);
		}
	}
}
