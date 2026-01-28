package org.anonomi.android.logging;

import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.system.ClockModule;
import org.anonchatsecure.bramble.test.TestSecureRandomModule;

import java.security.SecureRandom;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		ClockModule.class,
		BrambleCoreModule.class,
		TestSecureRandomModule.class,
		LoggingModule.class,
		LoggingTestModule.class,
})
public interface LoggingComponent {

	SecureRandom random();

	CachingLogHandler cachingLogHandler();

	LogEncrypter logEncrypter();

	LogDecrypter logDecrypter();

}
