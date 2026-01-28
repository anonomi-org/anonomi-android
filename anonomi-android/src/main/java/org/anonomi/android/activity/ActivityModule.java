package org.anonomi.android.activity;

import android.app.Activity;

import org.anonomi.android.controller.AnonChatController;
import org.anonomi.android.controller.AnonChatControllerImpl;
import org.anonomi.android.controller.DbController;
import org.anonomi.android.controller.DbControllerImpl;

import dagger.Module;
import dagger.Provides;

import static org.anonomi.android.AnonChatService.AnonchatServiceConnection;

@Module
public class ActivityModule {

	private final BaseActivity activity;

	public ActivityModule(BaseActivity activity) {
		this.activity = activity;
	}

	@ActivityScope
	@Provides
	BaseActivity provideBaseActivity() {
		return activity;
	}

	@ActivityScope
	@Provides
	Activity provideActivity() {
		return activity;
	}

	@ActivityScope
	@Provides
	protected AnonChatController provideAnonChatController(
			AnonChatControllerImpl briarController) {
		activity.addLifecycleController(briarController);
		return briarController;
	}

	@ActivityScope
	@Provides
	DbController provideDBController(DbControllerImpl dbController) {
		return dbController;
	}

	@ActivityScope
	@Provides
	AnonchatServiceConnection provideBriarServiceConnection() {
		return new AnonchatServiceConnection();
	}
}
