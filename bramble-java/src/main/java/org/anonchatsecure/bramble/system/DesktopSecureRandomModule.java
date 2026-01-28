package org.anonchatsecure.bramble.system;

import org.anonchatsecure.bramble.api.system.SecureRandomProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.anonchatsecure.bramble.util.OsUtils.isLinux;

@Module
public class DesktopSecureRandomModule {

	@Provides
	@Singleton
	SecureRandomProvider provideSecureRandomProvider() {
		if (isLinux()) return new UnixSecureRandomProvider();
		return () -> null; // Use system default
	}
}
