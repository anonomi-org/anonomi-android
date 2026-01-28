package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.system.SecureRandomProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class TestSecureRandomModule {

	@Provides
	SecureRandomProvider provideSecureRandomProvider() {
		return new TestSecureRandomProvider();
	}
}
