package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.cleanup.CleanupModule;
import org.anonchatsecure.bramble.contact.ContactModule;
import org.anonchatsecure.bramble.crypto.CryptoExecutorModule;
import org.anonchatsecure.bramble.db.DatabaseExecutorModule;
import org.anonchatsecure.bramble.identity.IdentityModule;
import org.anonchatsecure.bramble.lifecycle.LifecycleModule;
import org.anonchatsecure.bramble.mailbox.MailboxModule;
import org.anonchatsecure.bramble.plugin.PluginModule;
import org.anonchatsecure.bramble.properties.PropertiesModule;
import org.anonchatsecure.bramble.rendezvous.RendezvousModule;
import org.anonchatsecure.bramble.sync.validation.ValidationModule;
import org.anonchatsecure.bramble.transport.TransportModule;
import org.anonchatsecure.bramble.transport.agreement.TransportKeyAgreementModule;
import org.anonchatsecure.bramble.versioning.VersioningModule;

public interface BrambleCoreEagerSingletons {

	void inject(CleanupModule.EagerSingletons init);

	void inject(ContactModule.EagerSingletons init);

	void inject(CryptoExecutorModule.EagerSingletons init);

	void inject(DatabaseExecutorModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(LifecycleModule.EagerSingletons init);

	void inject(MailboxModule.EagerSingletons init);

	void inject(PluginModule.EagerSingletons init);

	void inject(PropertiesModule.EagerSingletons init);

	void inject(RendezvousModule.EagerSingletons init);

	void inject(TransportKeyAgreementModule.EagerSingletons init);

	void inject(TransportModule.EagerSingletons init);

	void inject(ValidationModule.EagerSingletons init);

	void inject(VersioningModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(BrambleCoreEagerSingletons c) {
			c.inject(new CleanupModule.EagerSingletons());
			c.inject(new ContactModule.EagerSingletons());
			c.inject(new CryptoExecutorModule.EagerSingletons());
			c.inject(new DatabaseExecutorModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new LifecycleModule.EagerSingletons());
			c.inject(new MailboxModule.EagerSingletons());
			c.inject(new RendezvousModule.EagerSingletons());
			c.inject(new PluginModule.EagerSingletons());
			c.inject(new PropertiesModule.EagerSingletons());
			c.inject(new TransportKeyAgreementModule.EagerSingletons());
			c.inject(new TransportModule.EagerSingletons());
			c.inject(new ValidationModule.EagerSingletons());
			c.inject(new VersioningModule.EagerSingletons());
		}
	}
}
