package org.anonchatsecure.bramble.system;

import org.anonchatsecure.bramble.api.system.ResourceProvider;
import org.briarproject.onionwrapper.JavaLocationUtilsFactory;
import org.briarproject.onionwrapper.LocationUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaSystemModule {

	@Provides
	@Singleton
	LocationUtils provideLocationUtils() {
		return JavaLocationUtilsFactory.createJavaLocationUtils();
	}

	@Provides
	@Singleton
	ResourceProvider provideResourceProvider(JavaResourceProvider provider) {
		return provider;
	}
}
