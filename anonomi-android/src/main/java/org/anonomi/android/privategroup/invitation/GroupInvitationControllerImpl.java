package org.anonomi.android.privategroup.invitation;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonomi.android.controller.handler.ExceptionHandler;
import org.anonomi.android.sharing.InvitationControllerImpl;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.event.GroupInvitationRequestReceivedEvent;
import org.anonchatsecure.anonchat.api.privategroup.event.GroupInvitationResponseReceivedEvent;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationItem;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager.CLIENT_ID;

@NotNullByDefault
class GroupInvitationControllerImpl
		extends InvitationControllerImpl<GroupInvitationItem>
		implements GroupInvitationController {

	private final GroupInvitationManager groupInvitationManager;

	@Inject
	GroupInvitationControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, EventBus eventBus,
			GroupInvitationManager groupInvitationManager) {
		super(dbExecutor, lifecycleManager, eventBus);
		this.groupInvitationManager = groupInvitationManager;
	}

	@Override
	public void eventOccurred(Event e) {
		super.eventOccurred(e);

		if (e instanceof GroupInvitationRequestReceivedEvent) {
			LOG.info("Group invitation request received, reloading");
			listener.loadInvitations(false);
		} else if (e instanceof GroupInvitationResponseReceivedEvent) {
			LOG.info("Group invitation response received, reloading");
			listener.loadInvitations(false);
		}
	}

	@Override
	protected ClientId getShareableClientId() {
		return CLIENT_ID;
	}

	@Override
	protected Collection<GroupInvitationItem> getInvitations()
			throws DbException {
		return groupInvitationManager.getInvitations();
	}

	@Override
	public void respondToInvitation(GroupInvitationItem item, boolean accept,
			ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				PrivateGroup g = item.getShareable();
				ContactId c = item.getCreator().getId();
				groupInvitationManager.respondToInvitation(c, g, accept);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}
