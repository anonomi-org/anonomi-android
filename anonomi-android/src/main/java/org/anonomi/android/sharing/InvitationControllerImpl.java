package org.anonomi.android.sharing;

import android.app.Activity;

import org.anonchatsecure.bramble.api.contact.event.ContactRemovedEvent;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.event.GroupAddedEvent;
import org.anonchatsecure.bramble.api.sync.event.GroupRemovedEvent;
import org.anonomi.android.controller.DbControllerImpl;
import org.anonomi.android.controller.handler.ResultExceptionHandler;
import org.anonchatsecure.anonchat.api.sharing.InvitationItem;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import androidx.annotation.CallSuper;

import static java.util.logging.Level.WARNING;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.LogUtils.now;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public abstract class InvitationControllerImpl<I extends InvitationItem>
		extends DbControllerImpl
		implements InvitationController<I>, EventListener {

	protected static final Logger LOG =
			Logger.getLogger(InvitationControllerImpl.class.getName());

	private final EventBus eventBus;

	// UI thread
	protected InvitationListener listener;

	public InvitationControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, EventBus eventBus) {
		super(dbExecutor, lifecycleManager);
		this.eventBus = eventBus;
	}

	@Override
	public void onActivityCreate(Activity activity) {
		listener = (InvitationListener) activity;
	}

	@Override
	public void onActivityStart() {
		eventBus.addListener(this);
	}

	@Override
	public void onActivityStop() {
		eventBus.removeListener(this);
	}

	@Override
	public void onActivityDestroy() {

	}

	@CallSuper
	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactRemovedEvent) {
			// LOG.info("Contact removed, reloading...");
			listener.loadInvitations(true);
		} else if (e instanceof GroupAddedEvent) {
			GroupAddedEvent g = (GroupAddedEvent) e;
			ClientId cId = g.getGroup().getClientId();
			if (cId.equals(getShareableClientId())) {
				// LOG.info("Group added, reloading");
				listener.loadInvitations(false);
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			ClientId cId = g.getGroup().getClientId();
			if (cId.equals(getShareableClientId())) {
				// LOG.info("Group removed, reloading");
				listener.loadInvitations(false);
			}
		}
	}

	protected abstract ClientId getShareableClientId();

	@Override
	public void loadInvitations(boolean clear,
			ResultExceptionHandler<Collection<I>, DbException> handler) {
		runOnDbThread(() -> {
			try {
				long start = now();
				Collection<I> invitations = new ArrayList<>(getInvitations());
				logDuration(LOG, "Loading invitations", start);
				handler.onResult(invitations);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

	@DatabaseExecutor
	protected abstract Collection<I> getInvitations() throws DbException;

}
