package org.anonomi.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;

import com.vanniktech.emoji.RecentEmoji;

import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.KeyStrengthener;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.mailbox.MailboxDirectory;
import org.anonchatsecure.bramble.api.plugin.BluetoothConstants;
import org.anonchatsecure.bramble.api.plugin.LanTcpConstants;
import org.anonchatsecure.bramble.api.plugin.PluginConfig;
import org.anonchatsecure.bramble.api.plugin.TorControlPort;
import org.anonchatsecure.bramble.api.plugin.TorDirectory;
import org.anonchatsecure.bramble.api.plugin.TorSocksPort;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPluginFactory;
import org.anonchatsecure.bramble.api.reporting.DevConfig;
import org.anonchatsecure.bramble.plugin.bluetooth.AndroidBluetoothPluginFactory;
import org.anonchatsecure.bramble.plugin.file.AndroidRemovableDrivePluginFactory;
import org.anonchatsecure.bramble.plugin.file.MailboxPluginFactory;
import org.anonchatsecure.bramble.plugin.tcp.AndroidLanTcpPluginFactory;
import org.anonchatsecure.bramble.plugin.tor.AndroidTorPluginFactory;
import org.anonchatsecure.bramble.util.AndroidUtils;
import org.anonchatsecure.bramble.util.StringUtils;
import org.anonomi.android.account.DozeHelperModule;
import org.anonomi.android.account.LockManagerImpl;
import org.anonomi.android.account.SetupModule;
import org.anonomi.android.blog.BlogModule;
import org.anonomi.android.contact.ContactListModule;
import org.anonomi.android.contact.add.nearby.AddNearbyContactModule;
import org.anonomi.android.contact.connect.ConnectViaBluetoothModule;
import org.anonomi.android.forum.ForumModule;
import org.anonomi.android.hotspot.HotspotModule;
import org.anonomi.android.introduction.IntroductionModule;
import org.anonomi.android.logging.LoggingModule;
import org.anonomi.android.login.LoginModule;
import org.anonomi.android.mailbox.MailboxModule;
import org.anonomi.android.navdrawer.NavDrawerModule;
import org.anonomi.android.privategroup.conversation.GroupConversationModule;
import org.anonomi.android.privategroup.list.GroupListModule;
import org.anonomi.android.removabledrive.TransferDataModule;
import org.anonomi.android.reporting.DevReportModule;
import org.anonomi.android.settings.SettingsModule;
import org.anonomi.android.sharing.SharingModule;
import org.anonomi.android.test.TestAvatarCreatorImpl;
import org.anonomi.android.viewmodel.ViewModelModule;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.android.DozeWatchdog;
import org.anonchatsecure.anonchat.api.android.LockManager;
import org.anonchatsecure.anonchat.api.android.NetworkUsageMetrics;
import org.anonchatsecure.anonchat.api.android.ScreenFilterMonitor;
import org.anonchatsecure.anonchat.api.test.TestAvatarCreator;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;
import static android.os.Build.VERSION.SDK_INT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_CONTROL_PORT;
import static org.anonchatsecure.bramble.api.plugin.TorConstants.DEFAULT_SOCKS_PORT;
import static org.anonchatsecure.bramble.api.reporting.ReportingConstants.DEV_ONION_ADDRESS;
import static org.anonchatsecure.bramble.api.reporting.ReportingConstants.DEV_PUBLIC_KEY_HEX;
import static org.anonomi.android.TestingConstants.IS_DEBUG_BUILD;

@Module(includes = {
		SetupModule.class,
		DozeHelperModule.class,
		AddNearbyContactModule.class,
		LoggingModule.class,
		LoginModule.class,
		NavDrawerModule.class,
		ViewModelModule.class,
		SettingsModule.class,
		DevReportModule.class,
		ContactListModule.class,
		IntroductionModule.class,
		ConnectViaBluetoothModule.class,
		// below need to be within same scope as ViewModelProvider.Factory
		BlogModule.class,
		ForumModule.class,
		GroupListModule.class,
		GroupConversationModule.class,
		SharingModule.class,
		HotspotModule.class,
		TransferDataModule.class,
		MailboxModule.class,
})
public class AppModule {

	static class EagerSingletons {
		@Inject
		AndroidNotificationManager androidNotificationManager;
		@Inject
		ScreenFilterMonitor screenFilterMonitor;
		@Inject
		NetworkUsageMetrics networkUsageMetrics;
		@Inject
		DozeWatchdog dozeWatchdog;
		@Inject
		LockManager lockManager;
		@Inject
		RecentEmoji recentEmoji;
	}

	private final Application application;

	public AppModule(Application application) {
		this.application = application;
	}

	public static AndroidComponent getAndroidComponent(Context ctx) {
		AnonChatApplication app = (AnonChatApplication) ctx.getApplicationContext();
		return app.getApplicationComponent();
	}

	@Provides
	@Singleton
	Application providesApplication() {
		return application;
	}

	@Provides
	@Singleton
	DatabaseConfig provideDatabaseConfig(Application app) {
		//FIXME: StrictMode
		StrictMode.ThreadPolicy tp = StrictMode.allowThreadDiskReads();
		StrictMode.allowThreadDiskWrites();
		File dbDir = app.getApplicationContext().getDir("db", MODE_PRIVATE);
		File keyDir = app.getApplicationContext().getDir("key", MODE_PRIVATE);
		StrictMode.setThreadPolicy(tp);
		KeyStrengthener keyStrengthener = SDK_INT >= 23
				? new AndroidKeyStrengthener() : null;
		return new AndroidDatabaseConfig(dbDir, keyDir, keyStrengthener);
	}

	@Provides
	@Singleton
	@MailboxDirectory
	File provideMailboxDirectory(Application app) {
		return app.getDir("mailbox", MODE_PRIVATE);
	}

	@Provides
	@Singleton
	@TorDirectory
	File provideTorDirectory(Application app) {
		return app.getDir("tor", MODE_PRIVATE);
	}

	@Provides
	@Singleton
	@TorSocksPort
	int provideTorSocksPort() {
		if (!IS_DEBUG_BUILD) {
			return DEFAULT_SOCKS_PORT;
		} else {
			return DEFAULT_SOCKS_PORT + 2;
		}
	}

	@Provides
	@Singleton
	@TorControlPort
	int provideTorControlPort() {
		if (!IS_DEBUG_BUILD) {
			return DEFAULT_CONTROL_PORT;
		} else {
			return DEFAULT_CONTROL_PORT + 2;
		}
	}

	@Provides
	@Singleton
	PluginConfig providePluginConfig(AndroidBluetoothPluginFactory bluetooth,
			AndroidTorPluginFactory tor, AndroidLanTcpPluginFactory lan,
			AndroidRemovableDrivePluginFactory drive,
			MailboxPluginFactory mailbox, FeatureFlags featureFlags) {
		@NotNullByDefault
		PluginConfig pluginConfig = new PluginConfig() {

			@Override
			public Collection<DuplexPluginFactory> getDuplexFactories() {
				return asList(bluetooth, tor, lan);
			}

			@Override
			public Collection<SimplexPluginFactory> getSimplexFactories() {
				List<SimplexPluginFactory> simplex = new ArrayList<>();
				simplex.add(mailbox);
				simplex.add(drive);
				return simplex;
			}

			@Override
			public boolean shouldPoll() {
				return true;
			}

			@Override
			public Map<TransportId, List<TransportId>> getTransportPreferences() {
				// Prefer LAN to Bluetooth
				return singletonMap(BluetoothConstants.ID,
						singletonList(LanTcpConstants.ID));
			}
		};
		return pluginConfig;
	}

	@Provides
	@Singleton
	DevConfig provideDevConfig(Application app, CryptoComponent crypto) {
		@NotNullByDefault
		DevConfig devConfig = new DevConfig() {

			@Override
			public PublicKey getDevPublicKey() {
				try {
					return crypto.getMessageKeyParser().parsePublicKey(
							StringUtils.fromHexString(DEV_PUBLIC_KEY_HEX));
				} catch (GeneralSecurityException | FormatException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getDevOnionAddress() {
				return DEV_ONION_ADDRESS;
			}

			@Override
			public File getReportDir() {
				return AndroidUtils.getReportDir(app.getApplicationContext());
			}

			@Override
			public File getLogcatFile() {
				return AndroidUtils.getLogcatFile(app.getApplicationContext());
			}
		};
		return devConfig;
	}

	@Provides
	TestAvatarCreator provideTestAvatarCreator(
			TestAvatarCreatorImpl testAvatarCreator) {
		return testAvatarCreator;
	}

	@Provides
	SharedPreferences provideSharedPreferences(Application app) {
		// FIXME unify this with getDefaultSharedPreferences()
		return app.getSharedPreferences("db", MODE_PRIVATE);
	}

	@Provides
	@Singleton
	AndroidNotificationManager provideAndroidNotificationManager(
			LifecycleManager lifecycleManager, EventBus eventBus,
			AndroidNotificationManagerImpl notificationManager) {
		lifecycleManager.registerService(notificationManager);
		eventBus.addListener(notificationManager);
		return notificationManager;
	}

	@Provides
	@Singleton
	ScreenFilterMonitor provideScreenFilterMonitor(
			LifecycleManager lifecycleManager,
			ScreenFilterMonitorImpl screenFilterMonitor) {
		if (SDK_INT <= 29) {
			// this keeps track of installed apps and does not work on API 30+
			lifecycleManager.registerService(screenFilterMonitor);
		}
		return screenFilterMonitor;
	}

	@Provides
	@Singleton
	NetworkUsageMetrics provideNetworkUsageMetrics(
			LifecycleManager lifecycleManager) {
		NetworkUsageMetrics networkUsageMetrics = new NetworkUsageMetricsImpl();
		lifecycleManager.registerService(networkUsageMetrics);
		return networkUsageMetrics;
	}

	@Provides
	@Singleton
	DozeWatchdog provideDozeWatchdog(LifecycleManager lifecycleManager) {
		DozeWatchdogImpl dozeWatchdog = new DozeWatchdogImpl(application);
		lifecycleManager.registerService(dozeWatchdog);
		return dozeWatchdog;
	}

	@Provides
	@Singleton
	LockManager provideLockManager(LifecycleManager lifecycleManager,
			EventBus eventBus, LockManagerImpl lockManager) {
		lifecycleManager.registerService(lockManager);
		eventBus.addListener(lockManager);
		return lockManager;
	}

	@Provides
	@Singleton
	RecentEmoji provideRecentEmoji(LifecycleManager lifecycleManager,
			RecentEmojiImpl recentEmoji) {
		lifecycleManager.registerOpenDatabaseHook(recentEmoji);
		return recentEmoji;
	}

	@Provides
	FeatureFlags provideFeatureFlags() {
		return new FeatureFlags() {

			@Override
			public boolean shouldEnableImageAttachments() {
				return true;
			}

			@Override
			public boolean shouldEnableProfilePictures() {
				return true;
			}

			@Override
			public boolean shouldEnableDisappearingMessages() {
				return true;
			}

			@Override
			public boolean shouldEnablePrivateGroupsInCore() {
				return true;
			}

			@Override
			public boolean shouldEnableForumsInCore() {
				return true;
			}

			@Override
			public boolean shouldEnableBlogsInCore() {
				return true;
			}
		};
	}
}
