package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.system.SecureRandomProvider;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.Provider;

@NotNullByDefault
public class TestSecureRandomProvider implements SecureRandomProvider {

	@Override
	public Provider getProvider() {
		// Use the default provider
		return null;
	}
}
