package org.anonchatsecure.anonchat.headless

import dagger.Component
import org.anonchatsecure.bramble.BrambleCoreEagerSingletons
import org.anonchatsecure.bramble.BrambleCoreModule
import org.anonchatsecure.bramble.BrambleJavaEagerSingletons
import org.anonchatsecure.bramble.BrambleJavaModule
import org.anonchatsecure.anonchat.BriarCoreEagerSingletons
import org.anonchatsecure.anonchat.BriarCoreModule
import java.security.SecureRandom
import javax.inject.Singleton

@Component(
    modules = [
        BrambleCoreModule::class,
        BrambleJavaModule::class,
        BriarCoreModule::class,
        HeadlessModule::class
    ]
)
@Singleton
internal interface BriarHeadlessApp : BrambleCoreEagerSingletons, BriarCoreEagerSingletons,
    BrambleJavaEagerSingletons, HeadlessEagerSingletons {

    fun getRouter(): Router

    fun getSecureRandom(): SecureRandom
}
