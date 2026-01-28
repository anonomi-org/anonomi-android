package org.anonchatsecure.anonchat.headless

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.anonchatsecure.bramble.account.AccountModule
import org.anonchatsecure.bramble.api.db.DatabaseConfig
import org.anonchatsecure.bramble.api.mailbox.MailboxDirectory
import org.anonchatsecure.bramble.api.plugin.PluginConfig
import org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_CONTROL_PORT
import org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_SOCKS_PORT
import org.anonchatsecure.bramble.api.plugin.TorControlPort
import org.anonchatsecure.bramble.api.plugin.TorSocksPort
import org.anonchatsecure.bramble.api.plugin.TransportId
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPluginFactory
import org.anonchatsecure.bramble.event.DefaultEventExecutorModule
import org.anonchatsecure.bramble.system.ClockModule
import org.anonchatsecure.bramble.system.DefaultTaskSchedulerModule
import org.anonchatsecure.bramble.system.DefaultThreadFactoryModule
import org.anonchatsecure.bramble.system.DefaultWakefulIoExecutorModule
import org.anonchatsecure.bramble.test.TestFeatureFlagModule
import org.anonchatsecure.bramble.test.TestSecureRandomModule
import org.anonchatsecure.anonchat.api.test.TestAvatarCreator
import org.anonchatsecure.anonchat.headless.blogs.HeadlessBlogModule
import org.anonchatsecure.anonchat.headless.contact.HeadlessContactModule
import org.anonchatsecure.anonchat.headless.event.HeadlessEventModule
import org.anonchatsecure.anonchat.headless.forums.HeadlessForumModule
import org.anonchatsecure.anonchat.headless.messaging.HeadlessMessagingModule
import java.io.File
import java.util.Collections.emptyList
import javax.inject.Singleton

@Module(
    includes = [
        AccountModule::class,
        ClockModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        DefaultWakefulIoExecutorModule::class,
        DefaultThreadFactoryModule::class,
        TestFeatureFlagModule::class,
        TestSecureRandomModule::class,
        HeadlessBlogModule::class,
        HeadlessContactModule::class,
        HeadlessEventModule::class,
        HeadlessForumModule::class,
        HeadlessMessagingModule::class
    ]
)
internal class HeadlessTestModule(private val appDir: File) {

    @Provides
    @Singleton
    internal fun provideBriarService(anonchatService: BriarTestServiceImpl): AnonchatService =
        anonchatService

    @Provides
    @Singleton
    internal fun provideDatabaseConfig(): DatabaseConfig {
        val dbDir = File(appDir, "db")
        val keyDir = File(appDir, "key")
        return HeadlessDatabaseConfig(dbDir, keyDir)
    }

    @Provides
    @MailboxDirectory
    internal fun provideMailboxDirectory(): File {
        return File(appDir, "mailbox")
    }

    @Provides
    @TorSocksPort
    internal fun provideTorSocksPort(): Int = DEFAULT_SOCKS_PORT

    @Provides
    @TorControlPort
    internal fun provideTorControlPort(): Int = DEFAULT_CONTROL_PORT

    @Provides
    @Singleton
    internal fun providePluginConfig(): PluginConfig {
        return object : PluginConfig {
            override fun getDuplexFactories(): Collection<DuplexPluginFactory> = emptyList()
            override fun getSimplexFactories(): Collection<SimplexPluginFactory> = emptyList()
            override fun shouldPoll(): Boolean = false
            override fun getTransportPreferences(): Map<TransportId, List<TransportId>> = emptyMap()
        }
    }

    @Provides
    @Singleton
    internal fun provideObjectMapper() = ObjectMapper()

    @Provides
    internal fun provideTestAvatarCreator() = TestAvatarCreator { null }
}
