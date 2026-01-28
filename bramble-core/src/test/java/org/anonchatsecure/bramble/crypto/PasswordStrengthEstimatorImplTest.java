package org.anonchatsecure.bramble.crypto;

import org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.junit.Test;

import static org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator.NONE;
import static org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator.QUITE_STRONG;
import static org.junit.Assert.assertTrue;

public class PasswordStrengthEstimatorImplTest extends BrambleTestCase {

	@Test
	public void testWeakPasswords() {
		PasswordStrengthEstimator e = new PasswordStrengthEstimatorImpl();
		assertTrue(e.estimateStrength("") == NONE);
		assertTrue(e.estimateStrength("password") < QUITE_STRONG);
		assertTrue(e.estimateStrength("letmein") < QUITE_STRONG);
		assertTrue(e.estimateStrength("123456") < QUITE_STRONG);
	}

	@Test
	public void testStrongPasswords() {
		PasswordStrengthEstimator e = new PasswordStrengthEstimatorImpl();
		// Industry standard
		assertTrue(e.estimateStrength("Tr0ub4dor&3") > QUITE_STRONG);
		assertTrue(e.estimateStrength("correcthorsebatterystaple")
				> QUITE_STRONG);
	}
}
