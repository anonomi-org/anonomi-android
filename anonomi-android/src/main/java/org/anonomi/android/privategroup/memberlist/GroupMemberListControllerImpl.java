package org.anonomi.android.privategroup.memberlist;

import org.anonchatsecure.bramble.api.connection.ConnectionRegistry;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.controller.DbControllerImpl;
import org.anonomi.android.controller.handler.ResultExceptionHandler;
import org.anonchatsecure.anonchat.api.privategroup.GroupMember;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

class GroupMemberListControllerImpl extends DbControllerImpl
		implements GroupMemberListController {

	private static final Logger LOG =
			Logger.getLogger(GroupMemberListControllerImpl.class.getName());

	private final ConnectionRegistry connectionRegistry;
	private final PrivateGroupManager privateGroupManager;

	@Inject
	GroupMemberListControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			ConnectionRegistry connectionRegistry,
			PrivateGroupManager privateGroupManager) {
		super(dbExecutor, lifecycleManager);
		this.connectionRegistry = connectionRegistry;
		this.privateGroupManager = privateGroupManager;
	}

	@Override
	public void loadMembers(GroupId groupId,
			ResultExceptionHandler<Collection<MemberListItem>, DbException> handler) {
		runOnDbThread(() -> {
			try {
				Collection<MemberListItem> items = new ArrayList<>();
				Collection<GroupMember> members =
						privateGroupManager.getMembers(groupId);
				for (GroupMember m : members) {
					ContactId c = m.getContactId();
					boolean online = false;
					if (c != null)
						online = connectionRegistry.isConnected(c);
					items.add(new MemberListItem(m, online));
				}
				handler.onResult(items);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}
