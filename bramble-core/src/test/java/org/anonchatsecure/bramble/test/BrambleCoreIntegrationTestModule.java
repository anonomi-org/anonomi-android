package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.battery.DefaultBatteryManagerModule;
import org.anonchatsecure.bramble.event.DefaultEventExecutorModule;
import org.anonchatsecure.bramble.system.DefaultWakefulIoExecutorModule;
import org.anonchatsecure.bramble.system.TimeTravelModule;

import dagger.Module;

@Module(includes = {
		DefaultBatteryManagerModule.class,
		DefaultEventExecutorModule.class,
		DefaultWakefulIoExecutorModule.class,
		TestThreadFactoryModule.class,
		TestDatabaseConfigModule.class,
		TestFeatureFlagModule.class,
		TestMailboxDirectoryModule.class,
		TestSecureRandomModule.class,
		TimeTravelModule.class
})
public class BrambleCoreIntegrationTestModule {

}
