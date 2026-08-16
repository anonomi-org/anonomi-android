package org.anonchatsecure.bramble.crypto;

import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.system.SystemClock;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.bramble.test.TestSecureRandomProvider;
import org.bouncycastle.crypto.generators.SCrypt;
import org.junit.Test;

import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.util.StringUtils.toUtf8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * A backup has to be readable on a device other than the one that wrote it,
 * so the cost is taken from the file rather than calibrated locally.
 */
public class FixedCostKeyDerivationTest extends BrambleTestCase {

	private static final int COST = 1024;
	private static final String PASSWORD = "123456789012345678901234567890";

	private final CryptoComponentImpl crypto =
			new CryptoComponentImpl(new TestSecureRandomProvider(),
					new ScryptKdf(new SystemClock()));

	@Test
	public void testDerivesScryptWithTheGivenSaltAndCost() {
		byte[] salt = getRandomBytes(32);
		SecretKey k = crypto.deriveKeyFromPassword(PASSWORD, salt, COST);
		assertArrayEquals(SCrypt.generate(toUtf8(PASSWORD), salt, COST, 8, 1,
				SecretKey.LENGTH), k.getBytes());
	}

	@Test
	public void testSameInputsGiveTheSameKey() {
		byte[] salt = getRandomBytes(32);
		assertArrayEquals(
				crypto.deriveKeyFromPassword(PASSWORD, salt, COST).getBytes(),
				crypto.deriveKeyFromPassword(PASSWORD, salt, COST).getBytes());
	}

	@Test
	public void testDifferentSaltOrCostGivesADifferentKey() {
		byte[] salt = getRandomBytes(32), otherSalt = getRandomBytes(32);
		SecretKey k = crypto.deriveKeyFromPassword(PASSWORD, salt, COST);
		assertNotEquals(k, crypto.deriveKeyFromPassword(PASSWORD, otherSalt,
				COST));
		assertNotEquals(k, crypto.deriveKeyFromPassword(PASSWORD, salt,
				COST * 2));
		assertNotEquals(k, crypto.deriveKeyFromPassword("other", salt, COST));
	}

	@Test
	public void testKeyIsTheRightLength() {
		SecretKey k = crypto.deriveKeyFromPassword(PASSWORD,
				getRandomBytes(32), COST);
		assertEquals(SecretKey.LENGTH, k.getBytes().length);
	}
}
