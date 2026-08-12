package org.anonomi.android.panic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.contact.ContactManager;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;

public class PanicResponderActivity extends BriarActivity {

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
	ConversationManager conversationManager;

	private String panicMessage;
	private boolean panicMessagesSent = false;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		skipSignInCheck = true;

		panicMessage = getString(R.string.panic_message);

		Intent intent = getIntent();
		if (intent == null || !ACTION_INTERNAL_PANIC.equals(intent.getAction())) {
			Log.d("PanicResponder", "Not an internal panic intent!");
			finish();
			return;
		}

		Log.d("PanicResponder", "Panic trigger accepted!");

		// Check for action override from PanicDialogHelper (dialog choice)
		String override = intent.getStringExtra(EXTRA_PANIC_ACTION);
		String action;
		if (PanicActionPolicy.isUsableOverride(override)) {
			action = override;
		} else {
			SecureValue stored = new SecurePrefsManager(this)
					.read(PREF_KEY_PANIC_ACTION);
			action = PanicActionPolicy.resolve(stored);
		}

		sendPanicMessages();
		long delayMillis = panicMessagesSent ? 5000 : 0;
		final String panicAction = action;
		new Handler(Looper.getMainLooper()).postDelayed(
				() -> executePanicAction(panicAction), delayMillis);
	}

	private void executePanicAction(String action) {
		switch (action) {
			case ACTION_DELETE_ACCOUNT:
				signOut(true, true);
				finishAndRemoveTask();
				break;
			case ACTION_SIGN_OUT:
			case ACTION_SHOW_DIALOG:
				// Reaching here with "show dialog" means we were started
				// without a choice and cannot ask for one - normally
				// PanicDialogHelper asks first and passes the answer. The
				// setting was read successfully and it says "ask me", not
				// "delete", so take the reversible action.
				signOut(true, false);
				finishAndRemoveTask();
				break;
			default:
				// Unreachable: the action is either a validated override or
				// comes from PanicActionPolicy, which only returns known
				// actions. Reaching it would be a bug in this app, not a
				// tampered setting, so take the reversible branch - a defect
				// should not cost someone their account.
				signOut(true, false);
				finishAndRemoveTask();
				break;
		}
	}

	private void sendPanicMessages() {
		try {
			Collection<Contact> contacts = contactManager.getContacts();
			for (Contact c : contacts) {
				if (c.isPanicContact()) {
					try {
						ContactId contactId = c.getId();
						GroupId groupId = messagingManager.getConversationId(contactId);
						long timestamp = System.currentTimeMillis();

						PrivateMessage panicMsg = privateMessageFactory.createPrivateMessage(
								groupId, timestamp, panicMessage, Collections.emptyList()
						);
						messagingManager.addLocalMessage(panicMsg);
						panicMessagesSent = true;
					} catch (Exception ignored) {
					}
				}
			}
		} catch (DbException ignored) {
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}
}
