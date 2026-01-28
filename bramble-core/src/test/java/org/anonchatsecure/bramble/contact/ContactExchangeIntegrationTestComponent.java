package org.anonchatsecure.bramble.contact;

import org.anonchatsecure.bramble.BrambleCoreIntegrationTestEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.api.connection.ConnectionManager;
import org.anonchatsecure.bramble.api.contact.ContactExchangeManager;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.TestDnsModule;
import org.anonchatsecure.bramble.test.TestPluginConfigModule;
import org.anonchatsecure.bramble.test.TestSocksModule;

import java.util.concurrent.Executor;

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
interface ContactExchangeIntegrationTestComponent
		extends BrambleCoreIntegrationTestEagerSingletons {

	ConnectionManager getConnectionManager();

	ContactExchangeManager getContactExchangeManager();

	ContactManager getContactManager();

	EventBus getEventBus();

	IdentityManager getIdentityManager();

	@IoExecutor
	Executor getIoExecutor();

	LifecycleManager getLifecycleManager();
}
