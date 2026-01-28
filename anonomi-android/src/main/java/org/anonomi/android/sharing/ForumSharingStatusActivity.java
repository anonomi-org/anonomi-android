package org.anonomi.android.sharing;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.event.Event;
import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonchatsecure.anonchat.api.forum.ForumInvitationResponse;
import org.anonchatsecure.anonchat.api.forum.ForumSharingManager;
import org.anonchatsecure.anonchat.api.forum.event.ForumInvitationResponseReceivedEvent;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.Collection;

import javax.inject.Inject;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ForumSharingStatusActivity extends SharingStatusActivity {

	// Fields that are accessed from background threads must be volatile
	@Inject
	protected volatile ForumSharingManager forumSharingManager;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void eventOccurred(Event e) {
		super.eventOccurred(e);
		if (e instanceof ForumInvitationResponseReceivedEvent) {
			ForumInvitationResponseReceivedEvent r =
					(ForumInvitationResponseReceivedEvent) e;
			ForumInvitationResponse h = r.getMessageHeader();
			if (h.getShareableId().equals(getGroupId()) && h.wasAccepted()) {
				loadSharedWith();
			}
		}
	}

	@Override
	int getInfoText() {
		return R.string.sharing_status_forum;
	}

	@Override
	@DatabaseExecutor
	protected Collection<Contact> getSharedWith() throws DbException {
		return forumSharingManager.getSharedWith(getGroupId());
	}

}
