package org.anonchatsecure.bramble.network;

import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.network.NetworkManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaNetworkModule {

	public static class EagerSingletons {
		@Inject
		NetworkManager networkManager;
	}

	@Provides
	@Singleton
	NetworkManager provideNetworkManager(LifecycleManager lifecycleManager,
			JavaNetworkManager networkManager) {
		lifecycleManager.registerService(networkManager);
		return networkManager;
	}
}
