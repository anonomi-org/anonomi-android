package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.battery.AndroidBatteryModule;
import org.anonchatsecure.bramble.network.AndroidNetworkModule;
import org.anonchatsecure.bramble.reporting.ReportingModule;

public interface BrambleAndroidEagerSingletons {

	void inject(AndroidBatteryModule.EagerSingletons init);

	void inject(AndroidNetworkModule.EagerSingletons init);

	void inject(ReportingModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(
				BrambleAndroidEagerSingletons c) {
			c.inject(new AndroidBatteryModule.EagerSingletons());
			c.inject(new AndroidNetworkModule.EagerSingletons());
			c.inject(new ReportingModule.EagerSingletons());
		}
	}
}
