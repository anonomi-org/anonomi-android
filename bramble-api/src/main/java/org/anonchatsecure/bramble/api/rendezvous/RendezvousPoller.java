package org.anonchatsecure.bramble.api.rendezvous;

import org.anonchatsecure.bramble.api.contact.PendingContactId;

/**
 * Interface for the poller that makes rendezvous connections to pending
 * contacts.
 */
public interface RendezvousPoller {

	long getLastPollTime(PendingContactId p);
}
