package org.anonchatsecure.bramble.plugin.file;

import org.anonchatsecure.bramble.api.plugin.PluginCallback;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPlugin;
import org.anonchatsecure.bramble.api.plugin.simplex.SimplexPluginFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.mailbox.MailboxConstants.ID;
import static org.anonchatsecure.bramble.api.mailbox.MailboxConstants.MAX_LATENCY;

@NotNullByDefault
public class MailboxPluginFactory implements SimplexPluginFactory {

	@Inject
	MailboxPluginFactory() {
	}

	@Override
	public TransportId getId() {
		return ID;
	}

	@Override
	public long getMaxLatency() {
		return MAX_LATENCY;
	}

	@Nullable
	@Override
	public SimplexPlugin createPlugin(PluginCallback callback) {
		return new MailboxPlugin(callback, MAX_LATENCY);
	}
}
