package org.anonomi.android.panic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonomi.android.util.SecurePrefsManager;
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

		SecurePrefsManager securePrefs = new SecurePrefsManager(this);
		String action = securePrefs.getDecrypted(PREF_KEY_PANIC_ACTION);
		if (action == null) action = ACTION_SIGN_OUT;

		sendPanicMessages();

		long delayMillis = panicMessagesSent ? 5000 : 0;
		final String panicAction = action;

		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			executePanicAction(panicAction);
		}, delayMillis);
	}

	private void executePanicAction(String action) {
		switch (action) {
			case ACTION_DELETE_ACCOUNT:
				signOut(true, true);
				finishAndRemoveTask();
				break;
			case ACTION_SHOW_DIALOG:
				showPanicDialog();
				break;
			case ACTION_SIGN_OUT:
			default:
				signOut(true, false);
				finishAndRemoveTask();
				break;
		}
	}

	private void showPanicDialog() {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
				this, R.style.AnonDialogTheme);
		builder.setTitle(R.string.panic_dialog_title);
		builder.setItems(new CharSequence[]{
				getString(R.string.panic_option_sign_out),
				getString(R.string.panic_option_delete_account),
				getString(R.string.cancel)
		}, (dialog, which) -> {
			switch (which) {
				case 0:
					signOut(true, false);
					finishAndRemoveTask();
					break;
				case 1:
					signOut(true, true);
					finishAndRemoveTask();
					break;
				default:
					finish();
					break;
			}
		});
		builder.setOnCancelListener(dialog -> finish());
		builder.show();
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
