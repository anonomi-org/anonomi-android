package org.anonomi.android.panic;

import android.content.Intent;
import android.os.Bundle;

import org.anonomi.R;
import org.anonomi.android.AnonChatApplication;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.controller.AnonChatController;
import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import org.anonchatsecure.bramble.account.ExternalStorageCleanup;
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

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_HOME;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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

	/**
	 * How long a queued message is given to reach the network before the
	 * database holding it is deleted. Spent after the key has gone, so it
	 * costs nothing that an interruption could take back.
	 */
	private static final long DELIVERY_WINDOW_MS = 5000;

	private static final long SHUTDOWN_TIMEOUT_MS = 15_000;

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
		// Whether the account was unlocked, asked now because it is answered
		// from the database key, which is the first thing the wipe destroys.
		// Not accountSignedIn(): that is also false while the database is
		// open and still migrating, which is exactly when deleting its files
		// from underneath it would do the most damage.
		boolean unlocked = accountManager.hasDatabaseKey();
		new PanicWipe(new WipeSteps(unlocked), new PanicWipeMarker(this),
				wakefulExecutor()).begin(this::exitProcess);
		// begin() does not return until the account is beyond recovery, so
		// the app can leave the screen now instead of waiting for the last
		// file to go. What is left of the wipe carries on without a UI.
		leaveScreen();
	}

	/**
	 * Puts the launcher in front before taking the task away.
	 * <p>
	 * Taking the task away on its own lets whatever was underneath return to
	 * the top for a moment, and an activity that resumes without a database
	 * key sends its user to the setup screen - so the app comes back offering
	 * to create an account instead of disappearing. Nothing of ours resumes
	 * while the launcher is on top.
	 */
	private void leaveScreen() {
		Intent home = new Intent(ACTION_MAIN);
		home.addCategory(CATEGORY_HOME);
		home.setFlags(FLAG_ACTIVITY_NEW_TASK);
		startActivity(home);
		finishAndRemoveTask();
	}

	/**
	 * Signing out keeps the account, so unlike a wipe there is a database to
	 * write to afterwards and the messages are worth waiting for. Neither
	 * step belongs on the UI thread.
	 */
	private void signOutAfterNotifying() {
		wakefulExecutor().execute(() -> {
			sendPanicMessages();
			runOnUiThread(() -> signOut(true));
		});
	}

	private Executor wakefulExecutor() {
		return task -> wakeLockManager.executeWakefully(task, "PanicResponder");
	}

	/**
	 * Ends the process once there is nothing left to delete. The task is
	 * already gone by this point, so there is no activity to start and
	 * nothing to bring back to the screen on the way out.
	 */
	private void exitProcess() {
		AnonChatApplication app = (AnonChatApplication) getApplication();
		if (!app.isInstrumentationTest()) System.exit(0);
	}

	private boolean sendPanicMessages() {
		Collection<Contact> contacts;
		try {
			contacts = contactManager.getContacts();
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			return false;
		}
		boolean sent = false;
		for (Contact c : contacts) {
			if (c.isPanicContact()) sent |= sendPanicMessage(c);
		}
		return sent;
	}

	private boolean sendPanicMessage(Contact c) {
		try {
			ContactId contactId = c.getId();
			GroupId groupId = messagingManager.getConversationId(contactId);
			long timestamp = System.currentTimeMillis();
			PrivateMessage panicMsg =
					privateMessageFactory.createPrivateMessage(groupId,
							timestamp, panicMessage, Collections.emptyList());
			messagingManager.addLocalMessage(panicMsg);
			return true;
		} catch (Exception e) {
			// One contact that cannot be reached is not a reason to skip the
			// rest, or to stop what the trigger was used for.
			logException(LOG, WARNING, e);
			return false;
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	private class WipeSteps implements PanicWipe.Steps {

		private final boolean unlocked;

		WipeSteps(boolean unlocked) {
			this.unlocked = unlocked;
		}

		@Override
		public void destroyKey() {
			accountManager.deleteDatabaseKey();
		}

		@Override
		public boolean notifyPanicContacts() {
			// The database is still open and still holds the key it was
			// unlocked with, so this works after the key file has gone.
			return sendPanicMessages();
		}

		@Override
		public void waitForDelivery() {
			try {
				Thread.sleep(DELIVERY_WINDOW_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void deleteRemainingData() {
			if (!unlocked || !shutDownServices()) {
				// Either nothing was running, or it did not stop. Deleting is
				// what the trigger was for, so it does not wait for either.
				controller.deleteAccount();
			}
			// The disguise is not kept with the settings an account owns, so
			// removing it belongs to the wipe rather than to deleting an
			// account. Cheap, and done before the slow step below.
			SecurePrefsManager.deleteDisguise(PanicResponderActivity.this);
			// External storage is deleted here rather than with the rest of
			// the account, so that a large map cache cannot spend the time
			// the shutdown above is allowed. Waited for rather than left to
			// run on: this returning is what lets the marker be cleared and
			// the process end, and being killed in the middle of it is the
			// case the marker is there for.
			new ExternalStorageCleanup(PanicResponderActivity.this).runIfDue();
		}

		/**
		 * @return true if the services stopped and took the account with
		 * them.
		 */
		private boolean shutDownServices() {
			// Stopping first lets the database close instead of having its
			// files deleted underneath it. The wait is bounded because the
			// shutdown can block on a service that will never finish
			// starting now that the key it needs has gone.
			CountDownLatch done = new CountDownLatch(1);
			controller.signOut(result -> done.countDown(), true);
			try {
				if (done.await(SHUTDOWN_TIMEOUT_MS, MILLISECONDS)) return true;
				LOG.warning("Timed out waiting for services to stop");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}
}
