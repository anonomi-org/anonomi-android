package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.crypto.CryptoModule;
import org.anonchatsecure.bramble.data.DataModule;
import org.anonchatsecure.bramble.system.ClockModule;
import org.anonchatsecure.bramble.test.TestSecureRandomModule;
import org.anonchatsecure.bramble.transport.TransportModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		ClockModule.class,
		CryptoModule.class,
		DataModule.class,
		TestSecureRandomModule.class,
		TransportModule.class,
})
interface BackupContainerTestComponent {

	void inject(BackupContainerTest testCase);
}
