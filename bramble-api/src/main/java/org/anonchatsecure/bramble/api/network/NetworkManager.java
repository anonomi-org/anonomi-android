package org.anonchatsecure.bramble.api.network;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface NetworkManager {

	NetworkStatus getNetworkStatus();
}
