package org.anonchatsecure.bramble.settings;

import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.settings.SettingsManager;

import dagger.Module;
import dagger.Provides;

@Module
public class SettingsModule {

	@Provides
	SettingsManager provideSettingsManager(DatabaseComponent db) {
		return new SettingsManagerImpl(db);
	}

}
