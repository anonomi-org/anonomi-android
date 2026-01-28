package org.anonchatsecure.anonchat.headless

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import org.anonchatsecure.bramble.account.AccountModule
import org.anonchatsecure.bramble.api.FeatureFlags
import org.anonchatsecure.bramble.api.db.DatabaseConfig
import org.anonchatsecure.bramble.api.mailbox.MailboxDirectory
import org.anonchatsecure.bramble.api.plugin.PluginConfig
import org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_CONTROL_PORT
import org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_SOCKS_PORT
import org.anonchatsecure.bramble.api.plugin.TorControlPort
import org.anonchatsecure.bramble.api.plugin.TorDirectory
import org.anonchatsecure.bramble.api.plugin.TorSocksPort
import org.anonchatsecure.bramble.api.plugin.TransportId
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPluginFactory
import org.anonchatsecure.bramble.battery.DefaultBatteryManagerModule
import org.anonchatsecure.bramble.event.DefaultEventExecutorModule
import org.anonchatsecure.bramble.plugin.tor.MacTorPluginFactory
import org.anonchatsecure.bramble.plugin.tor.UnixTorPluginFactory
import org.anonchatsecure.bramble.plugin.tor.WindowsTorPluginFactory
import org.anonchatsecure.bramble.system.ClockModule
import org.anonchatsecure.bramble.system.DefaultTaskSchedulerModule
import org.anonchatsecure.bramble.system.DefaultThreadFactoryModule
import org.anonchatsecure.bramble.system.DefaultWakefulIoExecutorModule
import org.anonchatsecure.bramble.system.DesktopSecureRandomModule
import org.anonchatsecure.bramble.util.OsUtils.isLinux
import org.anonchatsecure.bramble.util.OsUtils.isMac
import org.anonchatsecure.bramble.util.OsUtils.isWindows
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
        DefaultBatteryManagerModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        DefaultWakefulIoExecutorModule::class,
        DefaultThreadFactoryModule::class,
        DesktopSecureRandomModule::class,
        HeadlessBlogModule::class,
        HeadlessContactModule::class,
        HeadlessEventModule::class,
        HeadlessForumModule::class,
        HeadlessMessagingModule::class
    ]
)
internal class HeadlessModule(private val appDir: File) {

    @Provides
    @Singleton
    internal fun provideBriarService(anonchatService: BriarServiceImpl): AnonchatService = anonchatService

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
    @TorDirectory
    internal fun provideTorDirectory(): File {
        return File(appDir, "tor")
    }

    @Provides
    @TorSocksPort
    internal fun provideTorSocksPort(): Int = DEFAULT_SOCKS_PORT

    @Provides
    @TorControlPort
    internal fun provideTorControlPort(): Int = DEFAULT_CONTROL_PORT

    @Provides
    @Singleton
    internal fun providePluginConfig(
        unixTor: UnixTorPluginFactory,
        macTor: MacTorPluginFactory,
        winTor: WindowsTorPluginFactory
    ): PluginConfig {
        val duplex: List<DuplexPluginFactory> = when {
            isLinux() -> listOf(unixTor)
            isMac() -> listOf(macTor)
            isWindows() -> listOf(winTor)
            else -> emptyList()
        }
        return object : PluginConfig {
            override fun getDuplexFactories(): Collection<DuplexPluginFactory> = duplex
            override fun getSimplexFactories(): Collection<SimplexPluginFactory> = emptyList()
            override fun shouldPoll(): Boolean = true
            override fun getTransportPreferences(): Map<TransportId, List<TransportId>> = emptyMap()
        }
    }

    @Provides
    @Singleton
    internal fun provideObjectMapper() = ObjectMapper()

    @Provides
    internal fun provideFeatureFlags() = object :
        FeatureFlags {
        override fun shouldEnableImageAttachments() = false
        override fun shouldEnableProfilePictures() = false
        override fun shouldEnableDisappearingMessages() = false
        override fun shouldEnablePrivateGroupsInCore() = false
        override fun shouldEnableForumsInCore() = true
        override fun shouldEnableBlogsInCore() = true
    }
}
