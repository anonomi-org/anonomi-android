package org.anonchatsecure.bramble;

import org.anonchatsecure.bramble.io.DnsModule;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.network.JavaNetworkModule;
import org.anonchatsecure.bramble.plugin.tor.CircumventionModule;
import org.anonchatsecure.bramble.socks.SocksModule;
import org.anonchatsecure.bramble.system.JavaSystemModule;

import dagger.Module;

@Module(includes = {
		CircumventionModule.class,
		DnsModule.class,
		JavaNetworkModule.class,
		JavaSystemModule.class,
		ModularMailboxModule.class,
		SocksModule.class
})
public class BrambleJavaModule {

}
