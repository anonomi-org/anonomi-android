package org.anonchatsecure.bramble.crypto;

import org.anonchatsecure.bramble.api.crypto.SecretKey;

interface PasswordBasedKdf {

	int chooseCostParameter();

	SecretKey deriveKey(String password, byte[] salt, int cost);
}
