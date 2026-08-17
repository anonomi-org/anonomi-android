package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.lifecycle.ShutdownManager;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public class NullShutdownManager implements ShutdownManager {

	@Override
	public int addShutdownHook(Runnable hook) {
		return 0;
	}

	@Override
	public boolean removeShutdownHook(int handle) {
		return true;
	}
}
