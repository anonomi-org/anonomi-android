package org.anonchatsecure.bramble.mailbox;

import org.anonchatsecure.bramble.api.plugin.Plugin;

interface MailboxConfig {

	/**
	 * The minimum interval between API call retries in milliseconds.
	 */
	long getApiCallerMinRetryInterval();

	/**
	 * The maximum interval between API call retries in milliseconds.
	 */
	long getApiCallerMaxRetryInterval();

	/**
	 * How long (in milliseconds) the Tor plugin needs to be continuously
	 * {@link Plugin.State#ACTIVE active} before we assume our contacts can
	 * reach our hidden service.
	 */
	long getTorReachabilityPeriod();

}
