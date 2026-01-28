package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.cleanup.CleanupModule;
import org.anonchatsecure.bramble.client.ClientModule;
import org.anonchatsecure.bramble.connection.ConnectionModule;
import org.anonchatsecure.bramble.contact.ContactModule;
import org.anonchatsecure.bramble.crypto.CryptoExecutorModule;
import org.anonchatsecure.bramble.crypto.CryptoModule;
import org.anonchatsecure.bramble.data.DataModule;
import org.anonchatsecure.bramble.db.DatabaseExecutorModule;
import org.anonchatsecure.bramble.db.DatabaseModule;
import org.anonchatsecure.bramble.event.EventModule;
import org.anonchatsecure.bramble.identity.IdentityModule;
import org.anonchatsecure.bramble.io.IoModule;
import org.anonchatsecure.bramble.keyagreement.KeyAgreementModule;
import org.anonchatsecure.bramble.lifecycle.LifecycleModule;
import org.anonchatsecure.bramble.mailbox.MailboxModule;
import org.anonchatsecure.bramble.plugin.PluginModule;
import org.anonchatsecure.bramble.properties.PropertiesModule;
import org.anonchatsecure.bramble.qrcode.QrCodeModule;
import org.anonchatsecure.bramble.record.RecordModule;
import org.anonchatsecure.bramble.reliability.ReliabilityModule;
import org.anonchatsecure.bramble.rendezvous.RendezvousModule;
import org.anonchatsecure.bramble.settings.SettingsModule;
import org.anonchatsecure.bramble.sync.SyncModule;
import org.anonchatsecure.bramble.sync.validation.ValidationModule;
import org.anonchatsecure.bramble.transport.TransportModule;
import org.anonchatsecure.bramble.transport.agreement.TransportKeyAgreementModule;
import org.anonchatsecure.bramble.versioning.VersioningModule;

import dagger.Module;

@Module(includes = {
		CleanupModule.class,
		ClientModule.class,
		ConnectionModule.class,
		ContactModule.class,
		CryptoModule.class,
		CryptoExecutorModule.class,
		DataModule.class,
		DatabaseModule.class,
		DatabaseExecutorModule.class,
		EventModule.class,
		IdentityModule.class,
		IoModule.class,
		KeyAgreementModule.class,
		LifecycleModule.class,
		MailboxModule.class,
		PluginModule.class,
		PropertiesModule.class,
		QrCodeModule.class,
		RecordModule.class,
		ReliabilityModule.class,
		RendezvousModule.class,
		SettingsModule.class,
		SyncModule.class,
		TransportKeyAgreementModule.class,
		TransportModule.class,
		ValidationModule.class,
		VersioningModule.class
})
public class BrambleCoreModule {
}
