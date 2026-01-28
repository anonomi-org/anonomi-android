package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.battery.AndroidBatteryModule;
import org.anonchatsecure.bramble.io.DnsModule;
import org.anonchatsecure.bramble.network.AndroidNetworkModule;
import org.anonchatsecure.bramble.plugin.tor.CircumventionModule;
import org.anonchatsecure.bramble.reporting.ReportingModule;
import org.anonchatsecure.bramble.socks.SocksModule;
import org.anonchatsecure.bramble.system.AndroidSystemModule;
import org.anonchatsecure.bramble.system.AndroidTaskSchedulerModule;
import org.anonchatsecure.bramble.system.AndroidWakeLockModule;
import org.anonchatsecure.bramble.system.AndroidWakefulIoExecutorModule;
import org.anonchatsecure.bramble.system.DefaultThreadFactoryModule;

import dagger.Module;

@Module(includes = {
		AndroidBatteryModule.class,
		AndroidNetworkModule.class,
		AndroidSystemModule.class,
		AndroidTaskSchedulerModule.class,
		AndroidWakefulIoExecutorModule.class,
		AndroidWakeLockModule.class,
		DefaultThreadFactoryModule.class,
		CircumventionModule.class,
		DnsModule.class,
		ReportingModule.class,
		SocksModule.class
})
public class BrambleAndroidModule {
}
