package org.anonomi.android.panic;

import android.content.Intent;
import android.os.Bundle;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.controller.AnonChatController;
import org.anonomi.android.logout.ExitActivity;
import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;

public class PanicResponderActivity extends BriarActivity {

	private static final Logger LOG =
			getLogger(PanicResponderActivity.class.getName());

	public static final String ACTION_INTERNAL_PANIC =
			"org.anonomi.android.panic.ACTION_INTERNAL_PANIC";
	public static final String EXTRA_PANIC_ACTION =
			"org.anonomi.android.panic.EXTRA_PANIC_ACTION";

	@Inject
	ContactManager contactManager;

	@Inject
	MessagingManager messagingManager;

	@Inject
	PrivateMessageFactory privateMessageFactory;

	@Inject
	AccountManager accountManager;

	@Inject
	AnonChatController controller;

	@Inject
	AndroidWakeLockManager wakeLockManager;

	private String panicMessage;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		skipSignInCheck = true;

		panicMessage = getString(R.string.panic_message);

		Intent intent = getIntent();
		if (intent == null ||
				!ACTION_INTERNAL_PANIC.equals(intent.getAction())) {
			finish();
			return;
		}

		String override = intent.getStringExtra(EXTRA_PANIC_ACTION);
		String action;
		if (PanicActionPolicy.isUsableOverride(override)) {
			action = override;
		} else {
			SecureValue stored = new SecurePrefsManager(this)
					.read(PREF_KEY_PANIC_ACTION);
			action = PanicActionPolicy.resolve(stored);
		}

		executePanicAction(action);
	}

	private void executePanicAction(String action) {
		switch (action) {
			case ACTION_DELETE_ACCOUNT:
				wipeAccount();
				break;
			case ACTION_SIGN_OUT:
			case ACTION_SHOW_DIALOG:
				// Reaching here with "show dialog" means we were started
				// without a choice and cannot ask for one - normally
				// PanicDialogHelper asks first and passes the answer. The
				// setting was read successfully and it says "ask me", not
				// "delete", so take the reversible action.
				signOutAfterNotifying();
				break;
			default:
				// Unreachable: the action is either a validated override or
				// comes from PanicActionPolicy, which only returns known
				// actions. Reaching it would be a bug in this app, not a
				// tampered setting, so take the reversible branch - a defect
				// should not cost someone their account.
				signOutAfterNotifying();
				break;
		}
	}

	/**
	 * Deletes the key before returning, so the account is already beyond
	 * recovery by the time this activity can be torn down, and leaves the
	 * slower work to a thread that holds a wake lock.
	 */
	private void wipeAccount() {
		// Asked now because it is answered from the database key, which is
		// the first thing the wipe destroys.
		boolean wasSignedIn = controller.accountSignedIn();
		new PanicWipe(new WipeSteps(wasSignedIn), new PanicWipeMarker(this),
				wakefulExecutor())
				.begin(() -> runOnUiThread(this::exitApp));
	}

	/**
	 * Signing out keeps the account, so unlike a wipe there is a database to
	 * write to afterwards and the messages are worth waiting for. Neither
	 * step belongs on the UI thread.
	 */
	private void signOutAfterNotifying() {
		wakefulExecutor().execute(() -> {
			sendPanicMessages();
			signOut(true, false);
		});
	}

	private Executor wakefulExecutor() {
		return task -> wakeLockManager.executeWakefully(task, "PanicResponder");
	}

	private void exitApp() {
		Intent i = new Intent(this, ExitActivity.class);
		i.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
				| FLAG_ACTIVITY_NO_ANIMATION | FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

	private void sendPanicMessages() {
		Collection<Contact> contacts;
		try {
			contacts = contactManager.getContacts();
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			return;
		}
		for (Contact c : contacts) {
			if (c.isPanicContact()) sendPanicMessage(c);
		}
	}

	private void sendPanicMessage(Contact c) {
		try {
			ContactId contactId = c.getId();
			GroupId groupId = messagingManager.getConversationId(contactId);
			long timestamp = System.currentTimeMillis();
			PrivateMessage panicMsg =
					privateMessageFactory.createPrivateMessage(groupId,
							timestamp, panicMessage, Collections.emptyList());
			messagingManager.addLocalMessage(panicMsg);
		} catch (Exception e) {
			// One contact that cannot be reached is not a reason to skip the
			// rest, or to stop what the trigger was used for.
			logException(LOG, WARNING, e);
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	private class WipeSteps implements PanicWipe.Steps {

		private final boolean wasSignedIn;

		WipeSteps(boolean wasSignedIn) {
			this.wasSignedIn = wasSignedIn;
		}

		@Override
		public void destroyKey() {
			accountManager.deleteDatabaseKey();
		}

		@Override
		public void notifyPanicContacts() {
			// The database is still open and still holds the key it was
			// unlocked with, so this works after the key file has gone.
			sendPanicMessages();
		}

		@Override
		public void deleteRemainingData() {
			if (!wasSignedIn) {
				// The trigger can be used from the lock screen, where nothing
				// was ever started and there is nothing to wait for.
				controller.deleteAccount();
				return;
			}
			// Shutting the services down first lets the database close
			// instead of having its files deleted underneath it. Waiting is
			// safe here: the key is already gone, and if the shutdown never
			// finishes the marker stays set and the next start clears up.
			CountDownLatch done = new CountDownLatch(1);
			controller.signOut(result -> done.countDown(), true);
			try {
				done.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
