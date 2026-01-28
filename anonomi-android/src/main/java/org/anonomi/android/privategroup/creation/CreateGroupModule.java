package org.anonomi.android.privategroup.creation;

import org.anonomi.android.activity.ActivityScope;

import dagger.Module;
import dagger.Provides;

@Module
public class CreateGroupModule {

	@ActivityScope
	@Provides
	CreateGroupController provideCreateGroupController(
			CreateGroupControllerImpl createGroupController) {
		return createGroupController;
	}

}
