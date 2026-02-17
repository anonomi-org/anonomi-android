package org.anonomi.android.activity;

import android.app.Activity;

import org.anonomi.android.AndroidComponent;
import org.anonomi.android.StartupFailureActivity;
import org.anonomi.android.account.SetupActivity;
import org.anonomi.android.account.SetupFragment;
import org.anonomi.android.account.UnlockActivity;
import org.anonomi.android.blog.BlogActivity;
import org.anonomi.android.blog.BlogFragment;
import org.anonomi.android.blog.BlogInteractionsActivity;
import org.anonomi.android.blog.BlogPostFragment;
import org.anonomi.android.blog.FeedFragment;
import org.anonomi.android.blog.ReblogActivity;
import org.anonomi.android.blog.ReblogFragment;
import org.anonomi.android.blog.RssFeedActivity;
import org.anonomi.android.blog.RssFeedDeleteFeedDialogFragment;
import org.anonomi.android.blog.RssFeedImportFailedDialogFragment;
import org.anonomi.android.blog.RssFeedImportFragment;
import org.anonomi.android.blog.RssFeedManageFragment;
import org.anonomi.android.blog.WriteBlogPostActivity;
import org.anonomi.android.contact.ContactListFragment;
import org.anonomi.android.contact.add.nearby.AddNearbyContactActivity;
import org.anonomi.android.contact.add.nearby.AddNearbyContactErrorFragment;
import org.anonomi.android.contact.add.nearby.AddNearbyContactFragment;
import org.anonomi.android.contact.add.nearby.AddNearbyContactIntroFragment;
import org.anonomi.android.contact.add.remote.AddContactActivity;
import org.anonomi.android.contact.add.remote.LinkExchangeFragment;
import org.anonomi.android.contact.add.remote.NicknameFragment;
import org.anonomi.android.contact.add.remote.PendingContactListActivity;
import org.anonomi.android.contact.connect.ConnectViaBluetoothActivity;
import org.anonomi.android.conversation.AliasDialogFragment;
import org.anonomi.android.conversation.ConversationActivity;
import org.anonomi.android.conversation.ConversationSettingsDialog;
import org.anonomi.android.conversation.ImageActivity;
import org.anonomi.android.conversation.ImageFragment;
import org.anonomi.android.forum.CreateForumActivity;
import org.anonomi.android.forum.ForumActivity;
import org.anonomi.android.forum.ForumListFragment;
import org.anonomi.android.fragment.ScreenFilterDialogFragment;
import org.anonomi.android.hotspot.HotspotActivity;
import org.anonomi.android.introduction.ContactChooserFragment;
import org.anonomi.android.introduction.IntroductionActivity;
import org.anonomi.android.introduction.IntroductionMessageFragment;
import org.anonomi.android.login.ChangePasswordActivity;
import org.anonomi.android.login.OpenDatabaseFragment;
import org.anonomi.android.login.PasswordFragment;
import org.anonomi.android.login.StartupActivity;
import org.anonomi.android.mailbox.MailboxActivity;
import org.anonomi.android.navdrawer.NavDrawerActivity;
import org.anonomi.android.navdrawer.TransportsActivity;
import org.anonomi.android.panic.PanicPreferencesActivity;
import org.anonomi.android.panic.PanicResponderActivity;
import org.anonomi.android.privategroup.conversation.GroupActivity;
import org.anonomi.android.privategroup.creation.CreateGroupActivity;
import org.anonomi.android.privategroup.creation.CreateGroupFragment;
import org.anonomi.android.privategroup.creation.CreateGroupModule;
import org.anonomi.android.privategroup.creation.GroupInviteActivity;
import org.anonomi.android.privategroup.creation.GroupInviteFragment;
import org.anonomi.android.privategroup.invitation.GroupInvitationActivity;
import org.anonomi.android.privategroup.invitation.GroupInvitationModule;
import org.anonomi.android.privategroup.list.GroupListFragment;
import org.anonomi.android.privategroup.memberlist.GroupMemberListActivity;
import org.anonomi.android.privategroup.memberlist.GroupMemberModule;
import org.anonomi.android.privategroup.reveal.GroupRevealModule;
import org.anonomi.android.privategroup.reveal.RevealContactsActivity;
import org.anonomi.android.privategroup.reveal.RevealContactsFragment;
import org.anonomi.android.removabledrive.RemovableDriveActivity;
import org.anonomi.android.reporting.CrashFragment;
import org.anonomi.android.reporting.CrashReportActivity;
import org.anonomi.android.reporting.ReportFormFragment;
import org.anonomi.android.settings.ConfirmAvatarDialogFragment;
import org.anonomi.android.settings.SettingsActivity;
import org.anonomi.android.settings.SettingsFragment;
import org.anonomi.android.sharing.BlogInvitationActivity;
import org.anonomi.android.sharing.BlogSharingStatusActivity;
import org.anonomi.android.sharing.ForumInvitationActivity;
import org.anonomi.android.sharing.ForumSharingStatusActivity;
import org.anonomi.android.sharing.ShareBlogActivity;
import org.anonomi.android.sharing.ShareBlogFragment;
import org.anonomi.android.sharing.ShareForumActivity;
import org.anonomi.android.sharing.ShareForumFragment;
import org.anonomi.android.sharing.SharingModule;
import org.anonomi.android.splash.SplashScreenActivity;
import org.anonomi.android.test.TestDataActivity;
import org.anonomi.android.settings.MoneroSettingsActivity;
import org.anonomi.android.conversation.RequestXmrActivity;
import org.anonomi.android.map.MapLocationPickerActivity;
import org.anonomi.android.map.MapViewActivity;

import dagger.Component;

@ActivityScope
@Component(modules = {
		ActivityModule.class,
		CreateGroupModule.class,
		GroupInvitationModule.class,
		GroupMemberModule.class,
		GroupRevealModule.class,
		SharingModule.SharingLegacyModule.class
}, dependencies = AndroidComponent.class)
public interface ActivityComponent {

	Activity activity();

	void inject(SplashScreenActivity activity);

	void inject(StartupActivity activity);

	void inject(SetupActivity activity);

	void inject(NavDrawerActivity activity);

	void inject(MoneroSettingsActivity activity);

	void inject(PanicResponderActivity activity);

	void inject(PanicPreferencesActivity activity);

	void inject(AddNearbyContactActivity activity);

	void inject(ConversationActivity activity);

	void inject(ImageActivity activity);

	void inject(ForumInvitationActivity activity);

	void inject(BlogInvitationActivity activity);

	void inject(CreateGroupActivity activity);

	void inject(GroupActivity activity);

	void inject(GroupInviteActivity activity);

	void inject(GroupInvitationActivity activity);

	void inject(GroupMemberListActivity activity);

	void inject(RevealContactsActivity activity);

	void inject(CreateForumActivity activity);

	void inject(ShareForumActivity activity);

	void inject(ShareBlogActivity activity);

	void inject(ForumSharingStatusActivity activity);

	void inject(BlogSharingStatusActivity activity);

	void inject(BlogInteractionsActivity activity);

	void inject(ForumActivity activity);

	void inject(BlogActivity activity);

	void inject(WriteBlogPostActivity activity);

	void inject(BlogFragment fragment);

	void inject(BlogPostFragment fragment);

	void inject(ReblogFragment fragment);

	void inject(ReblogActivity activity);

	void inject(SettingsActivity activity);

	void inject(TransportsActivity activity);

	void inject(TestDataActivity activity);

	void inject(ChangePasswordActivity activity);

	void inject(IntroductionActivity activity);

	void inject(RssFeedActivity activity);

	void inject(StartupFailureActivity activity);

	void inject(UnlockActivity activity);

	void inject(AddContactActivity activity);

	void inject(PendingContactListActivity activity);

	void inject(CrashReportActivity crashReportActivity);

	void inject(HotspotActivity hotspotActivity);

	void inject(RemovableDriveActivity activity);

	// Fragments

	void inject(SetupFragment fragment);

	void inject(PasswordFragment imageFragment);

	void inject(OpenDatabaseFragment activity);

	void inject(ContactListFragment fragment);

	void inject(CreateGroupFragment fragment);

	void inject(GroupListFragment fragment);

	void inject(GroupInviteFragment fragment);

	void inject(RevealContactsFragment activity);

	void inject(ForumListFragment fragment);

	void inject(FeedFragment fragment);

	void inject(AddNearbyContactIntroFragment fragment);

	void inject(AddNearbyContactFragment fragment);

	void inject(LinkExchangeFragment fragment);

	void inject(NicknameFragment fragment);

	void inject(ContactChooserFragment fragment);

	void inject(ShareForumFragment fragment);

	void inject(ShareBlogFragment fragment);

	void inject(IntroductionMessageFragment fragment);

	void inject(SettingsFragment fragment);

	void inject(ScreenFilterDialogFragment fragment);

	void inject(AddNearbyContactErrorFragment fragment);

	void inject(AliasDialogFragment aliasDialogFragment);

	void inject(ImageFragment imageFragment);

	void inject(ReportFormFragment reportFormFragment);

	void inject(CrashFragment crashFragment);

	void inject(ConfirmAvatarDialogFragment fragment);

	void inject(ConversationSettingsDialog dialog);

	void inject(RssFeedImportFragment fragment);

	void inject(RssFeedManageFragment fragment);

	void inject(RssFeedImportFailedDialogFragment fragment);

	void inject(RssFeedDeleteFeedDialogFragment fragment);

	void inject(ConnectViaBluetoothActivity connectViaBluetoothActivity);

	void inject(MailboxActivity mailboxActivity);

	void inject(RequestXmrActivity activity);

	void inject(MapLocationPickerActivity activity);

	void inject(MapViewActivity activity);
}
