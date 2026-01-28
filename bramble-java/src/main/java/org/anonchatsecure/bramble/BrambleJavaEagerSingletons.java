package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.network.JavaNetworkModule;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface BrambleJavaEagerSingletons {

	void inject(JavaNetworkModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(BrambleJavaEagerSingletons c) {
			c.inject(new JavaNetworkModule.EagerSingletons());
		}
	}
}
