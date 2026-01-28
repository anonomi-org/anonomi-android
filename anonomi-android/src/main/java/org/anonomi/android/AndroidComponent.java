package org.anonomi.android;

import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.anonchatsecure.bramble.BrambleAndroidEagerSingletons;
import org.anonchatsecure.bramble.BrambleAndroidModule;
import org.anonchatsecure.bramble.BrambleAppComponent;
import org.anonchatsecure.bramble.BrambleCoreEagerSingletons;
import org.anonchatsecure.bramble.BrambleCoreModule;
import org.anonchatsecure.bramble.account.BriarAccountModule;
import org.anonchatsecure.bramble.api.FeatureFlags;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.connection.ConnectionRegistry;
import org.anonchatsecure.bramble.api.contact.ContactExchangeManager;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.crypto.CryptoExecutor;
import org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.keyagreement.KeyAgreementTask;
import org.anonchatsecure.bramble.api.keyagreement.PayloadEncoder;
import org.anonchatsecure.bramble.api.keyagreement.PayloadParser;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.plugin.PluginManager;
import org.anonchatsecure.bramble.api.settings.SettingsManager;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.mailbox.ModularMailboxModule;
import org.anonchatsecure.bramble.plugin.file.RemovableDriveModule;
import org.anonchatsecure.bramble.system.ClockModule;
import org.anonchatsecure.anonchat.BriarCoreEagerSingletons;
import org.anonchatsecure.anonchat.BriarCoreModule;
import org.anonomi.android.attachment.AttachmentModule;
import org.anonomi.android.attachment.media.MediaModule;
import org.anonomi.android.contact.connect.BluetoothIntroFragment;
import org.anonomi.android.conversation.glide.BriarModelLoader;
import org.anonomi.android.hotspot.AbstractTabsFragment;
import org.anonomi.android.hotspot.FallbackFragment;
import org.anonomi.android.hotspot.HotspotIntroFragment;
import org.anonomi.android.hotspot.ManualHotspotFragment;
import org.anonomi.android.hotspot.QrHotspotFragment;
import org.anonomi.android.logging.CachingLogHandler;
import org.anonomi.android.login.SignInReminderReceiver;
import org.anonomi.android.mailbox.ErrorFragment;
import org.anonomi.android.mailbox.ErrorWizardFragment;
import org.anonomi.android.mailbox.MailboxScanFragment;
import org.anonomi.android.mailbox.MailboxStatusFragment;
import org.anonomi.android.mailbox.OfflineFragment;
import org.anonomi.android.mailbox.SetupDownloadFragment;
import org.anonomi.android.mailbox.SetupIntroFragment;
import org.anonomi.android.removabledrive.ChooserFragment;
import org.anonomi.android.removabledrive.ReceiveFragment;
import org.anonomi.android.removabledrive.SendFragment;
import org.anonomi.android.settings.ConnectionsFragment;
import org.anonomi.android.settings.NotificationsFragment;
import org.anonomi.android.settings.SecurityFragment;
import org.anonomi.android.settings.SettingsFragment;
import org.anonomi.android.view.EmojiTextInputView;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.android.DozeWatchdog;
import org.anonchatsecure.anonchat.api.android.LockManager;
import org.anonchatsecure.anonchat.api.android.ScreenFilterMonitor;
import org.anonchatsecure.anonchat.api.attachment.AttachmentReader;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.BlogPostFactory;
import org.anonchatsecure.anonchat.api.blog.BlogSharingManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.feed.FeedManager;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.anonchatsecure.anonchat.api.introduction.IntroductionManager;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;
import org.anonchatsecure.anonchat.api.test.TestDataCreator;
import org.briarproject.onionwrapper.CircumventionProvider;
import org.briarproject.onionwrapper.LocationUtils;

import java.util.concurrent.Executor;

import javax.inject.Singleton;

import androidx.lifecycle.ViewModelProvider;
import dagger.Component;

@Singleton
@Component(modules = {
		BrambleCoreModule.class,
		BriarCoreModule.class,
		BrambleAndroidModule.class,
		BriarAccountModule.class,
		AppModule.class,
		AttachmentModule.class,
		ClockModule.class,
		MediaModule.class,
		ModularMailboxModule.class,
		RemovableDriveModule.class
})
public interface AndroidComponent
		extends BrambleCoreEagerSingletons, BrambleAndroidEagerSingletons,
		BriarCoreEagerSingletons, AndroidEagerSingletons, BrambleAppComponent {

	// Exposed objects
	@CryptoExecutor
	Executor cryptoExecutor();

	PasswordStrengthEstimator passwordStrengthIndicator();

	@DatabaseExecutor
	Executor databaseExecutor();

	TransactionManager transactionManager();

	MessageTracker messageTracker();

	LifecycleManager lifecycleManager();

	IdentityManager identityManager();

	AttachmentReader attachmentReader();

	AuthorManager authorManager();

	PluginManager pluginManager();

	EventBus eventBus();

	AndroidNotificationManager androidNotificationManager();

	ScreenFilterMonitor screenFilterMonitor();

	ConnectionRegistry connectionRegistry();

	ContactManager contactManager();

	ConversationManager conversationManager();

	MessagingManager messagingManager();

	PrivateMessageFactory privateMessageFactory();

	PrivateGroupManager privateGroupManager();

	GroupInvitationFactory groupInvitationFactory();

	GroupInvitationManager groupInvitationManager();

	PrivateGroupFactory privateGroupFactory();

	GroupMessageFactory groupMessageFactory();

	ForumManager forumManager();

	ForumSharingManager forumSharingManager();

	BlogSharingManager blogSharingManager();

	BlogManager blogManager();

	BlogPostFactory blogPostFactory();

	SettingsManager settingsManager();

	ContactExchangeManager contactExchangeManager();

	KeyAgreementTask keyAgreementTask();

	PayloadEncoder payloadEncoder();

	PayloadParser payloadParser();

	IntroductionManager introductionManager();

	AndroidExecutor androidExecutor();

	FeedManager feedManager();

	Clock clock();

	TestDataCreator testDataCreator();

	DozeWatchdog dozeWatchdog();

	@IoExecutor
	Executor ioExecutor();

	AccountManager accountManager();

	LockManager lockManager();

	LocationUtils locationUtils();

	CircumventionProvider circumventionProvider();

	ViewModelProvider.Factory viewModelFactory();

	FeatureFlags featureFlags();

	AndroidWakeLockManager wakeLockManager();

	CachingLogHandler logHandler();

	Thread.UncaughtExceptionHandler exceptionHandler();

	AutoDeleteManager autoDeleteManager();

	void inject(SignInReminderReceiver anonchatService);

	void inject(AnonChatService anonchatService);

	void inject(NotificationCleanupService notificationCleanupService);

	void inject(EmojiTextInputView textInputView);

	void inject(BriarModelLoader briarModelLoader);

	void inject(SettingsFragment settingsFragment);

	void inject(ConnectionsFragment connectionsFragment);

	void inject(SecurityFragment securityFragment);

	void inject(NotificationsFragment notificationsFragment);

	void inject(HotspotIntroFragment hotspotIntroFragment);

	void inject(AbstractTabsFragment abstractTabsFragment);

	void inject(QrHotspotFragment qrHotspotFragment);

	void inject(ManualHotspotFragment manualHotspotFragment);

	void inject(FallbackFragment fallbackFragment);

	void inject(ChooserFragment chooserFragment);

	void inject(SendFragment sendFragment);

	void inject(ReceiveFragment receiveFragment);

	void inject(BluetoothIntroFragment bluetoothIntroFragment);

	void inject(SetupIntroFragment setupIntroFragment);

	void inject(SetupDownloadFragment setupDownloadFragment);

	void inject(MailboxScanFragment mailboxScanFragment);

	void inject(OfflineFragment offlineFragment);

	void inject(ErrorFragment errorFragment);

	void inject(MailboxStatusFragment mailboxStatusFragment);

	void inject(ErrorWizardFragment errorWizardFragment);
}
