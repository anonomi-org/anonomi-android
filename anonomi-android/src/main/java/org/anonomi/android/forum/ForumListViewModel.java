package org.anonomi.android.forum;

import android.app.Application;
import android.widget.Toast;

import org.anonchatsecure.bramble.api.contact.event.ContactRemovedEvent;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.db.TransactionManager;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.event.GroupAddedEvent;
import org.anonchatsecure.bramble.api.sync.event.GroupRemovedEvent;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.anonomi.R;
import org.anonomi.android.viewmodel.DbViewModel;
import org.anonomi.android.viewmodel.LiveResult;
import org.anonchatsecure.anonchat.api.android.AndroidNotificationManager;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumPostHeader;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.forum.event.ForumInvitationRequestReceivedEvent;
import org.anonchatsecure.anonchat.api.forum.event.ForumPostReceivedEvent;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static android.widget.Toast.LENGTH_SHORT;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.now;
import static org.anonchatsecure.anonchat.api.forum.ForumManager.CLIENT_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class ForumListViewModel extends DbViewModel implements EventListener {

	private static final Logger LOG =
			getLogger(ForumListViewModel.class.getName());

	private final ForumManager forumManager;
	private final ForumSharingManager forumSharingManager;
	private final AndroidNotificationManager notificationManager;
	private final EventBus eventBus;

	private final MutableLiveData<LiveResult<List<ForumListItem>>> forumItems =
			new MutableLiveData<>();
	private final MutableLiveData<Integer> numInvitations =
			new MutableLiveData<>();

	@Inject
	ForumListViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			ForumManager forumManager,
			ForumSharingManager forumSharingManager,
			AndroidNotificationManager notificationManager, EventBus eventBus) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor);
		this.forumManager = forumManager;
		this.forumSharingManager = forumSharingManager;
		this.notificationManager = notificationManager;
		this.eventBus = eventBus;
		this.eventBus.addListener(this);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		eventBus.removeListener(this);
	}

	void clearAllForumPostNotifications() {
		notificationManager.clearAllForumPostNotifications();
	}

	void blockAllForumPostNotifications() {
		notificationManager.blockAllForumPostNotifications();
	}

	void unblockAllForumPostNotifications() {
		notificationManager.unblockAllForumPostNotifications();
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			// LOG.info("Contact removed, reloading available forums");
			loadForumInvitations();
		} else if (e instanceof ForumInvitationRequestReceivedEvent) {
			// LOG.info("Forum invitation received, reloading available forums");
			loadForumInvitations();
		} else if (e instanceof GroupAddedEvent) {
			GroupAddedEvent g = (GroupAddedEvent) e;
			if (g.getGroup().getClientId().equals(CLIENT_ID)) {
				// LOG.info("Forum added, reloading forums");
				loadForums();
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			if (g.getGroup().getClientId().equals(CLIENT_ID)) {
				// LOG.info("Forum removed, removing from list");
				onGroupRemoved(g.getGroup().getId());
			}
		} else if (e instanceof ForumPostReceivedEvent) {
			ForumPostReceivedEvent f = (ForumPostReceivedEvent) e;
			// LOG.info("Forum post added, updating item");
			onForumPostReceived(f.getGroupId(), f.getHeader());
		}
	}

	void loadForums() {
		loadFromDb(this::loadForums, forumItems::setValue);
	}

	@DatabaseExecutor
	private List<ForumListItem> loadForums(Transaction txn) throws DbException {
		long start = now();
		List<ForumListItem> forums = new ArrayList<>();
		for (Forum f : forumManager.getForums(txn)) {
			GroupCount count = forumManager.getGroupCount(txn, f.getId());
			forums.add(new ForumListItem(f, count));
		}
		Collections.sort(forums);
		logDuration(LOG, "Loading forums", start);
		return forums;
	}

	@UiThread
	private void onForumPostReceived(GroupId g, ForumPostHeader header) {
		List<ForumListItem> list = updateListItems(getList(forumItems),
				itemToTest -> itemToTest.getForum().getId().equals(g),
				itemToUpdate -> new ForumListItem(itemToUpdate, header));
		if (list == null) return;
		// re-sort as the order of items may have changed
		Collections.sort(list);
		forumItems.setValue(new LiveResult<>(list));
	}

	@UiThread
	private void onGroupRemoved(GroupId groupId) {
		removeAndUpdateListItems(forumItems, i ->
				i.getForum().getId().equals(groupId)
		);
	}

	void loadForumInvitations() {
		runOnDbThread(() -> {
			try {
				long start = now();
				int available = forumSharingManager.getInvitations().size();
				logDuration(LOG, "Loading available", start);
				numInvitations.postValue(available);
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

	LiveData<LiveResult<List<ForumListItem>>> getForumListItems() {
		return forumItems;
	}

	LiveData<Integer> getNumInvitations() {
		return numInvitations;
	}

	void deleteForum(GroupId groupId) {
		runOnDbThread(() -> {
			try {
				Forum f = forumManager.getForum(groupId);
				forumManager.removeForum(f);
				androidExecutor.runOnUiThread(() -> Toast
						.makeText(getApplication(), R.string.forum_left_toast,
								LENGTH_SHORT).show());
			} catch (DbException e) {
				handleException(e);
			}
		});
	}

}
