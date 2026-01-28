package org.anonchatsecure.bramble.transport.agreement;

import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.properties.TransportPropertyManager;
import org.anonchatsecure.bramble.api.transport.KeyManager;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.test.BrambleCoreIntegrationTestModule;
import org.anonchatsecure.bramble.test.BrambleIntegrationTestComponent;
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
interface TransportKeyAgreementTestComponent
		extends BrambleIntegrationTestComponent {

	KeyManager getKeyManager();

	TransportKeyAgreementManagerImpl getTransportKeyAgreementManager();

	ContactManager getContactManager();

	LifecycleManager getLifecycleManager();

	ContactGroupFactory getContactGroupFactory();

	SessionParser getSessionParser();

	TransportPropertyManager getTransportPropertyManager();

	DatabaseComponent getDatabaseComponent();
}
