package org.anonomi.android.privategroup.reveal;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.contactselection.ContactSelectorController;
import org.anonomi.android.controller.handler.ExceptionHandler;
import org.anonomi.android.controller.handler.ResultExceptionHandler;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

@NotNullByDefault
public interface RevealContactsController
		extends ContactSelectorController<RevealableContactItem> {

	void isOnboardingNeeded(
			ResultExceptionHandler<Boolean, DbException> handler);

	void onboardingShown(ExceptionHandler<DbException> handler);

	void reveal(GroupId g, Collection<ContactId> contacts,
			ExceptionHandler<DbException> handler);

}
