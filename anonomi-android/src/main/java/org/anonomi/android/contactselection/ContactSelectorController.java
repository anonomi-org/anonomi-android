package org.anonomi.android.contactselection;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.controller.DbController;
import org.anonomi.android.controller.handler.ResultExceptionHandler;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

@NotNullByDefault
public interface ContactSelectorController<I extends BaseSelectableContactItem>
		extends DbController {

	void loadContacts(GroupId g, Collection<ContactId> selection,
			ResultExceptionHandler<Collection<I>, DbException> handler);

}
