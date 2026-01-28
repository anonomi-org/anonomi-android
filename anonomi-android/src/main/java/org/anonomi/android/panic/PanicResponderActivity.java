package org.anonomi.android.panic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import androidx.preference.PreferenceManager;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
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

import info.guardianproject.GuardianProjectRSA4096;
import info.guardianproject.panic.Panic;
import info.guardianproject.trustedintents.TrustedIntents;

import static org.anonomi.android.panic.PanicPreferencesFragment.KEY_LOCK;
import static org.anonomi.android.panic.PanicPreferencesFragment.KEY_PURGE;

public class PanicResponderActivity extends BriarActivity {

	@Inject
	ContactManager contactManager;

	@Inject
	MessagingManager messagingManager;

	@Inject
	PrivateMessageFactory privateMessageFactory;

	@Inject
	ConversationManager conversationManager;

	private String panicMessage;
	private boolean panicMessagesSent = false; // 🚨 Track if any panic message was sent

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		panicMessage = getString(R.string.panic_message);

		TrustedIntents trustedIntents = TrustedIntents.get(this);
		trustedIntents.addTrustedSigner(GuardianProjectRSA4096.class);
		trustedIntents.addTrustedSigner(FDroidSignaturePin.class);
		trustedIntents.addTrustedSigner(AnonRippleSignaturePin.class);

		Intent intent = trustedIntents.getIntentFromTrustedSender(this);

		// ✅ Moved the Log statements here, after the intent is declared
		Log.d("PanicResponder", "Intent: " + intent);
		if (intent != null && Panic.isTriggerIntent(intent)) {
			Log.d("PanicResponder", "Panic trigger accepted!");

			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

			sendPanicMessages();

			// Only wait if something was sent
			long delayMillis = panicMessagesSent ? 10000 : 0;

			new Handler(Looper.getMainLooper()).postDelayed(() -> {
				if (sharedPref.getBoolean(KEY_PURGE, false)) {
					signOut(true, true);
				} else if (sharedPref.getBoolean(KEY_LOCK, true)) {
					signOut(true, false);
				}
				finishAndRemoveTask();
			}, delayMillis);

		} else {
			Log.d("PanicResponder", "Not a trusted panic intent!");
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
						panicMessagesSent = true; // ✅ Panic message sent
					} catch (Exception ignored) {
						// Fail silently
					}
				}
			}
		} catch (DbException ignored) {
			// Fail silently
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}
}