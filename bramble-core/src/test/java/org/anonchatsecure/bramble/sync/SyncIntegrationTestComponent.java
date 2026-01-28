package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestPluginConfigModule;
import org.anonchatsecure.bramble.test.TestSocksModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreIntegrationTestModule.class,
		BrambleCoreModule.class,
		ModularMailboxModule.class,
		TestDnsModule.class,
		TestSocksModule.class,
		TestPluginConfigModule.class,
})
interface SyncIntegrationTestComponent extends
		BrambleCoreIntegrationTestEagerSingletons {

	void inject(SyncIntegrationTest testCase);
}
