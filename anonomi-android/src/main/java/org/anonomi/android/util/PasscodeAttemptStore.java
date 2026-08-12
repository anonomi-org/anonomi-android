package org.anonomi.android.util;

/**
 * Keeps the run of wrong passcodes in encrypted preferences, where killing the
 * calculator does not reach it.
 */
public class PasscodeAttemptStore implements PasscodeThrottle.Store {

	private final SecurePrefsManager securePrefs;
	private final String key;

	public PasscodeAttemptStore(SecurePrefsManager securePrefs, String key) {
		this.securePrefs = securePrefs;
		this.key = key;
	}

	@Override
	public SecureValue read() {
		return securePrefs.read(key);
	}

	@Override
	public void write(String state) {
		securePrefs.putEncrypted(key, state);
	}
}
