package org.anonchatsecure.bramble.client;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ClientModule {

	@Provides
	ClientHelper provideClientHelper(ClientHelperImpl clientHelper) {
		return clientHelper;
	}

	@Provides
	ContactGroupFactory provideContactGroupFactory(
			ContactGroupFactoryImpl contactGroupFactory) {
		return contactGroupFactory;
	}

}
