package org.anonchatsecure.bramble.rendezvous;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.rendezvous.KeyMaterialSource;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface RendezvousCrypto {

	SecretKey deriveRendezvousKey(SecretKey staticMasterKey);

	KeyMaterialSource createKeyMaterialSource(SecretKey rendezvousKey,
			TransportId t);
}
