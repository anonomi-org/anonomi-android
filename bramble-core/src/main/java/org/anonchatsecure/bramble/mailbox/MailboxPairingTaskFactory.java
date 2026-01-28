package org.anonchatsecure.bramble.mailbox;


import org.anonchatsecure.bramble.api.mailbox.MailboxPairingTask;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface MailboxPairingTaskFactory {

	MailboxPairingTask createPairingTask(String qrCodePayload);

}
