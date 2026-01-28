package org.anonchatsecure.bramble.system;

import org.anonchatsecure.bramble.api.system.Clock;

import dagger.Module;
import dagger.Provides;

@Module
public class ClockModule {

	@Provides
	Clock provideClock() {
		return new SystemClock();
	}
}
