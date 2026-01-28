package org.anonomi.android.sharing;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.contactselection.ContactSelectorController;
import org.anonomi.android.contactselection.SelectableContactItem;
import org.anonomi.android.controller.handler.ExceptionHandler;

import java.util.Collection;

import javax.annotation.Nullable;

public interface ShareForumController
		extends ContactSelectorController<SelectableContactItem> {

	void share(GroupId g, Collection<ContactId> contacts, @Nullable String text,
			ExceptionHandler<DbException> handler);

}
