package org.anonomi.android.forum;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.crypto.CryptoExecutor;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonomi.R;
import org.anonomi.android.sharing.SharingController;
import org.anonomi.android.threaded.ThreadListViewModel;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumPost;
import org.anonchatsecure.anonchat.api.forum.ForumPostHeader;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.forum.event.ForumInvitationResponseReceivedEvent;
import org.anonchatsecure.anonchat.api.forum.event.ForumPostReceivedEvent;
import org.anonchatsecure.anonchat.api.sharing.event.ContactLeftShareableEvent;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.anonomi.android.viewmodel.LiveEvent;
import org.anonomi.android.viewmodel.MutableLiveEvent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static android.widget.Toast.LENGTH_SHORT;
import static java.lang.Math.max;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.now;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class ForumViewModel extends ThreadListViewModel<ForumPostItem> {

	private static final Logger LOG = getLogger(ForumViewModel.class.getName());

	private final ForumManager forumManager;
	private final ForumSharingManager forumSharingManager;
	private final MutableLiveEvent<android.util.Pair<byte[], String>>
			autoPlayAudio = new MutableLiveEvent<>();

	@Inject
	ForumViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			IdentityManager identityManager,
			AndroidNotificationManager notificationManager,
			SharingController sharingController,
			@CryptoExecutor Executor cryptoExecutor,
			Clock clock,
			MessageTracker messageTracker,
			EventBus eventBus,
			ForumManager forumManager,
			ForumSharingManager forumSharingManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor,
				identityManager, notificationManager, sharingController,
				cryptoExecutor, clock, messageTracker, eventBus);
		this.forumManager = forumManager;
		this.forumSharingManager = forumSharingManager;
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ForumPostReceivedEvent) {
			ForumPostReceivedEvent f = (ForumPostReceivedEvent) e;
			if (f.getGroupId().equals(groupId)) {
				ForumPostItem item;
				if (f.getAudioData() != null &&
						f.getAudioContentType() != null) {
					if (f.getAudioContentType().startsWith("image/")) {
						item = new ForumPostItem(f.getHeader(), f.getText(),
								null, null,
								f.getAudioData(), f.getAudioContentType());
					} else {
						item = new ForumPostItem(f.getHeader(), f.getText(),
								f.getAudioData(), f.getAudioContentType());
					}
				} else {
					item = new ForumPostItem(f.getHeader(), f.getText());
				}
				addItem(item, false);
				// Fire auto-play event for walkie-talkie
				if (f.getAudioData() != null
						&& f.getAudioContentType() != null
						&& f.getAudioContentType()
								.startsWith("audio/")) {
					String name = f.getHeader().getAuthor().getName();
					autoPlayAudio.postEvent(
							new android.util.Pair<>(f.getAudioData(), name));
				}
			}
		} else if (e instanceof ForumInvitationResponseReceivedEvent) {
			ForumInvitationResponseReceivedEvent f =
					(ForumInvitationResponseReceivedEvent) e;
			ForumInvitationResponse r = f.getMessageHeader();
			if (r.getShareableId().equals(groupId) && r.wasAccepted()) {
				sharingController.add(f.getContactId());
			}
		} else if (e instanceof ContactLeftShareableEvent) {
			ContactLeftShareableEvent c = (ContactLeftShareableEvent) e;
			if (c.getGroupId().equals(groupId)) {
				sharingController.remove(c.getContactId());
			}
		} else {
			super.eventOccurred(e);
		}
	}

	@Override
	protected void clearNotifications() {
		notificationManager.clearForumPostNotification(groupId);
	}

	LiveEvent<android.util.Pair<byte[], String>> getAutoPlayAudio() {
		return autoPlayAudio;
	}

	LiveData<Forum> loadForum() {
		MutableLiveData<Forum> forum = new MutableLiveData<>();
		runOnDbThread(() -> {
			try {
				Forum f = forumManager.getForum(groupId);
				forum.postValue(f);
			} catch (DbException e) {
				handleException(e);
			}
		});
		return forum;
	}

	@Override
	public void loadItems() {
		loadFromDb(txn -> {
			long start = now();
			List<ForumPostHeader> headers =
					forumManager.getPostHeaders(txn, groupId);
			logDuration(LOG, "Loading headers", start);
			start = now();
			List<ForumPostItem> items = new ArrayList<>();
			for (ForumPostHeader header : headers) {
				items.add(loadItem(txn, header));
			}
			logDuration(LOG, "Loading bodies and creating items", start);
			return items;
		}, this::setItems);
	}

	private ForumPostItem loadItem(Transaction txn, ForumPostHeader header)
			throws DbException {
		String text = forumManager.getPostText(txn, header.getId());
		if (header.hasImage()) {
			byte[] imageData = forumManager
					.getPostImageData(txn, header.getId());
			String imageContentType = forumManager
					.getPostImageContentType(txn, header.getId());
			return new ForumPostItem(header, text, null, null,
					imageData, imageContentType);
		}
		if (header.hasAudio()) {
			byte[] audioData = forumManager
					.getPostAudioData(txn, header.getId());
			String audioContentType = forumManager
					.getPostAudioContentType(txn, header.getId());
			return new ForumPostItem(header, text, audioData,
					audioContentType);
		}
		return new ForumPostItem(header, text);
	}

	@Override
	public void createAndStoreMessage(String text,
			@Nullable MessageId parentId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				GroupCount count = forumManager.getGroupCount(groupId);
				long timestamp = max(count.getLatestMsgTime() + 1,
						clock.currentTimeMillis());
				createMessage(text, timestamp, parentId, author);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	private void createMessage(String text, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author) {
		cryptoExecutor.execute(() -> {
			ForumPost msg = forumManager.createLocalPost(groupId, text,
					timestamp, parentId, author);
			storePost(msg, text);
		});
	}

	private void storePost(ForumPost msg, String text) {
		runOnDbThread(false, txn -> {
			long start = now();
			ForumPostHeader header = forumManager.addLocalPost(txn, msg);
			logDuration(LOG, "Storing forum post", start);
			txn.attach(() -> {
				ForumPostItem item = new ForumPostItem(header, text);
				addItem(item, true);
			});
		}, this::handleException);
	}

	void createAndStoreAudioMessage(byte[] audioData, String contentType,
			@Nullable MessageId parentId) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				GroupCount count = forumManager.getGroupCount(groupId);
				long timestamp = max(count.getLatestMsgTime() + 1,
						clock.currentTimeMillis());
				createAudioMessage(audioData, contentType, timestamp,
						parentId, author);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	private void createAudioMessage(byte[] audioData, String contentType,
			long timestamp, @Nullable MessageId parentId,
			LocalAuthor author) {
		cryptoExecutor.execute(() -> {
			LOG.info("Creating forum audio post...");
			ForumPost msg = forumManager.createLocalAudioPost(groupId, "",
					timestamp, parentId, author, audioData, contentType);
			storeAudioPost(msg, audioData, contentType);
		});
	}

	private void storeAudioPost(ForumPost msg, byte[] audioData,
			String contentType) {
		runOnDbThread(false, txn -> {
			long start = now();
			ForumPostHeader header = forumManager.addLocalPost(txn, msg);
			logDuration(LOG, "Storing forum audio post", start);
			txn.attach(() -> {
				ForumPostItem item = new ForumPostItem(header, "",
						audioData, contentType);
				addItem(item, true);
			});
		}, this::handleException);
	}

	void createAndStoreImageMessage(byte[] imageData, String contentType,
			@Nullable MessageId parentId, @Nullable String text) {
		runOnDbThread(() -> {
			try {
				LocalAuthor author = identityManager.getLocalAuthor();
				GroupCount count = forumManager.getGroupCount(groupId);
				long timestamp = max(count.getLatestMsgTime() + 1,
						clock.currentTimeMillis());
				createImageMessage(imageData, contentType, timestamp,
						parentId, author, text);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	private void createImageMessage(byte[] imageData, String contentType,
			long timestamp, @Nullable MessageId parentId,
			LocalAuthor author, @Nullable String text) {
		cryptoExecutor.execute(() -> {
			LOG.info("Creating forum image post...");
			String t = text != null ? text : "";
			ForumPost msg = forumManager.createLocalImagePost(groupId, t,
					timestamp, parentId, author, imageData, contentType);
			storeImagePost(msg, t, imageData, contentType);
		});
	}

	private void storeImagePost(ForumPost msg, String text,
			byte[] imageData, String contentType) {
		runOnDbThread(false, txn -> {
			long start = now();
			ForumPostHeader header = forumManager.addLocalPost(txn, msg);
			logDuration(LOG, "Storing forum image post", start);
			txn.attach(() -> {
				ForumPostItem item = new ForumPostItem(header, text,
						null, null, imageData, contentType);
				addItem(item, true);
			});
		}, this::handleException);
	}

	@Override
	protected void markItemRead(ForumPostItem item) {
		runOnDbThread(() -> {
			try {
				forumManager.setReadFlag(groupId, item.getId(), true);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@Override
	public void loadSharingContacts() {
		runOnDbThread(true, txn -> {
			Collection<Contact> contacts =
					forumSharingManager.getSharedWith(txn, groupId);
			Collection<ContactId> contactIds = new ArrayList<>(contacts.size());
			for (Contact c : contacts) contactIds.add(c.getId());
			txn.attach(() -> sharingController.addAll(contactIds));
		}, this::handleException);
	}

	void deleteForum() {
		runOnDbThread(() -> {
			try {
				Forum f = forumManager.getForum(groupId);
				forumManager.removeForum(f);
				getApplication().getSharedPreferences("forum_prefs",
						Context.MODE_PRIVATE).edit()
						.remove("forum_walkie_talkie_" + groupId.hashCode())
						.remove("forum_voice_distortion_" + groupId.hashCode())
						.apply();
				androidExecutor.runOnUiThread(() -> Toast
						.makeText(getApplication(), R.string.forum_left_toast,
								LENGTH_SHORT).show());
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	@Nullable
	MessageId getCurrentReplyId() {
		return getReplyId();
	}

	void clearReplyId() {
		setReplyId(null);
	}

	void loadRetentionDuration(Consumer<Long> callback) {
		runOnDbThread(() -> {
			try {
				long duration = forumManager.getRetentionDuration(groupId);
				androidExecutor.runOnUiThread(() -> callback.accept(duration));
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	void setRetentionDuration(long duration) {
		runOnDbThread(() -> {
			try {
				forumManager.setRetentionDuration(groupId, duration);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

}
