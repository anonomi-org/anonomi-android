package org.anonomi.android.sharing;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.NoSuchContactException;
import org.anonchatsecure.bramble.api.db.NoSuchGroupException;
import org.anonchatsecure.bramble.api.lifecycle.LifecycleManager;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.contactselection.ContactSelectorControllerImpl;
import org.anonomi.android.controller.handler.ExceptionHandler;
import org.anonchatsecure.anonchat.api.blog.BlogSharingManager;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.anonchatsecure.anonchat.api.sharing.SharingManager.SharingStatus;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

@Immutable
@NotNullByDefault
class ShareBlogControllerImpl extends ContactSelectorControllerImpl
		implements ShareBlogController {

	private final static Logger LOG =
			getLogger(ShareBlogControllerImpl.class.getName());

	private final BlogSharingManager blogSharingManager;

	@Inject
	ShareBlogControllerImpl(@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, ContactManager contactManager,
			AuthorManager authorManager,
			BlogSharingManager blogSharingManager) {
		super(dbExecutor, lifecycleManager, contactManager, authorManager);
		this.blogSharingManager = blogSharingManager;
	}

	@Override
	protected SharingStatus getSharingStatus(GroupId g, Contact c)
			throws DbException {
		return blogSharingManager.getSharingStatus(g, c);
	}

	@Override
	public void share(GroupId g, Collection<ContactId> contacts, @Nullable
			String text, ExceptionHandler<DbException> handler) {
		runOnDbThread(() -> {
			try {
				for (ContactId c : contacts) {
					try {
						blogSharingManager.sendInvitation(g, c, text);
					} catch (NoSuchContactException | NoSuchGroupException e) {
						logException(LOG, WARNING, e);
					}
				}
			} catch (DbException e) {
				logException(LOG, WARNING, e);
				handler.onException(e);
			}
		});
	}

}
