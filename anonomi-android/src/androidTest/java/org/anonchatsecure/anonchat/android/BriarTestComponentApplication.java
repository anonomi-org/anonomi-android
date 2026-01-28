package org.anonomi.android;

import org.anonchatsecure.bramble.BrambleAndroidEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreEagerSingletons;
import org.anonchatsecure.anonchat.BriarCoreEagerSingletons;

public class BriarTestComponentApplication extends AnonChatApplicationImpl {

	@Override
	protected AndroidComponent createApplicationComponent() {
		AndroidComponent component = DaggerBriarUiTestComponent.builder()
				.appModule(new AppModule(this)).build();
		// We need to load the eager singletons directly after making the
		// dependency graphs
		BrambleCoreEagerSingletons.Helper.injectEagerSingletons(component);
		BrambleAndroidEagerSingletons.Helper.injectEagerSingletons(component);
		BriarCoreEagerSingletons.Helper.injectEagerSingletons(component);
		AndroidEagerSingletons.Helper.injectEagerSingletons(component);
		return component;
	}

	@Override
	public boolean isInstrumentationTest() {
		return true;
	}

}
