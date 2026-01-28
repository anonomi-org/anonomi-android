package org.anonchatsecure.anonchat.headless

import dagger.Component
import org.anonchatsecure.bramble.BrambleCoreEagerSingletons
import org.anonchatsecure.bramble.BrambleCoreModule
import org.anonchatsecure.bramble.BrambleJavaEagerSingletons
import org.anonchatsecure.bramble.BrambleJavaModule
import org.anonchatsecure.bramble.api.crypto.CryptoComponent
import org.anonchatsecure.anonchat.BriarCoreEagerSingletons
import org.anonchatsecure.anonchat.BriarCoreModule
import org.anonchatsecure.anonchat.api.test.TestDataCreator
import javax.inject.Singleton

@Component(
    modules = [
        BrambleCoreModule::class,
        BrambleJavaModule::class,
        BriarCoreModule::class,
        HeadlessTestModule::class
    ]
)
@Singleton
internal interface BriarHeadlessTestApp : BrambleCoreEagerSingletons, BriarCoreEagerSingletons,
    BrambleJavaEagerSingletons, HeadlessEagerSingletons {

    fun getRouter(): Router

    fun getCryptoComponent(): CryptoComponent

    fun getTestDataCreator(): TestDataCreator
}
