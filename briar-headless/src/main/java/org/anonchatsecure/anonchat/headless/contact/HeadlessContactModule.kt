package org.anonchatsecure.anonchat.headless.contact

import dagger.Module
import dagger.Provides
import org.anonchatsecure.bramble.api.event.EventBus
import javax.inject.Singleton

@Module
class HeadlessContactModule {

    @Provides
    @Singleton
    internal fun provideContactController(
        eventBus: EventBus,
        contactController: ContactControllerImpl
    ): ContactController {
        eventBus.addListener(contactController)
        return contactController
    }

}
