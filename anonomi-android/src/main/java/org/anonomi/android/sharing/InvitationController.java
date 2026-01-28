package org.anonomi.android.sharing;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonomi.android.controller.ActivityLifecycleController;
import org.anonomi.android.controller.handler.ExceptionHandler;
import org.anonomi.android.controller.handler.ResultExceptionHandler;
import org.anonchatsecure.anonchat.api.sharing.InvitationItem;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

@NotNullByDefault
public interface InvitationController<I extends InvitationItem>
		extends ActivityLifecycleController {

	void loadInvitations(boolean clear,
			ResultExceptionHandler<Collection<I>, DbException> handler);

	void respondToInvitation(I item, boolean accept,
			ExceptionHandler<DbException> handler);

	interface InvitationListener {

		void loadInvitations(boolean clear);

	}

}
