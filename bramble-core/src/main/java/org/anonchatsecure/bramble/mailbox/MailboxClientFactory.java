package org.anonchatsecure.bramble.mailbox;

import org.anonchatsecure.bramble.api.mailbox.MailboxProperties;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface MailboxClientFactory {

	/**
	 * Creates a client for communicating with a contact's mailbox.
	 */
	MailboxClient createContactMailboxClient(
			TorReachabilityMonitor reachabilityMonitor);

	/**
	 * Creates a client for communicating with our own mailbox.
	 */
	MailboxClient createOwnMailboxClient(
			TorReachabilityMonitor reachabilityMonitor,
			MailboxProperties properties);
}
