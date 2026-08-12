package org.anonomi.android.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * The stealth passcode is checked, never read back. Storing it in a form that
 * can be turned back into the expression hands it to anything able to run as
 * this app, which is exactly the position the threat model assumes an attacker
 * reaches: holding the unlocked device.
 */
public class PasscodeHasherTest {

	private static final String PASSCODE = "123+45*6";

	/**
	 * The whole point. A stored passcode must not contain the expression, in
	 * any encoding, and must not be reversible into it.
	 */
	@Test
	public void aStoredPasscodeDoesNotContainTheExpression() {
		String stored = PasscodeHasher.hash(PASSCODE);
		assertFalse("The stored passcode contains the expression itself, so " +
						"anything able to read it can type it in: " + stored,
				stored.contains(PASSCODE));
	}

	@Test
	public void theCorrectPasscodeVerifies() {
		assertTrue(PasscodeHasher.verify(PASSCODE,
				PasscodeHasher.hash(PASSCODE)));
	}

	@Test
	public void aWrongPasscodeDoesNotVerify() {
		String stored = PasscodeHasher.hash(PASSCODE);
		assertFalse(PasscodeHasher.verify("123+45*7", stored));
		assertFalse(PasscodeHasher.verify("123+45*", stored));
		assertFalse(PasscodeHasher.verify("123+45*66", stored));
		assertFalse(PasscodeHasher.verify("", stored));
	}

	/**
	 * The expression is chosen in a dialog with a keyboard and entered on a
	 * keypad that cannot produce a space, so the two must agree.
	 */
	@Test
	public void whitespaceIsIgnoredOnBothSides() {
		assertTrue(PasscodeHasher.verify(" 123 + 45 * 6 ",
				PasscodeHasher.hash("123+45*6")));
		assertTrue(PasscodeHasher.verify("123+45*6",
				PasscodeHasher.hash(" 123 + 45 * 6 ")));
	}

	/**
	 * Two people who choose the same expression must not end up with the same
	 * stored value, and neither must one person who sets it twice.
	 */
	@Test
	public void everyStoredPasscodeGetsItsOwnSalt() {
		assertNotEquals(PasscodeHasher.hash(PASSCODE),
				PasscodeHasher.hash(PASSCODE));
	}

	/**
	 * Existing installs hold the expression itself. Rejecting those would lock
	 * people out of their own app on upgrade, so they still unlock.
	 */
	@Test
	public void aPasscodeStoredAsPlaintextStillUnlocks() {
		assertTrue("An existing passcode stopped working on upgrade",
				PasscodeHasher.verify("123+45*6", "123+45*6"));
		assertTrue(PasscodeHasher.verify("1+1", "1+1"));
		assertFalse(PasscodeHasher.verify("1+2", "1+1"));
	}

	/**
	 * ...and having unlocked once, it is replaced, so the expression stops
	 * being recoverable from storage without anyone being asked to do
	 * anything.
	 */
	@Test
	public void aPasscodeStoredAsPlaintextIsUpgradedOnceItHasBeenUsed() {
		String legacy = "123+45*6";
		assertTrue("A plaintext passcode was left in place rather than " +
				"upgraded, so it stays recoverable forever",
				PasscodeHasher.needsRehash(legacy));

		String upgraded = PasscodeHasher.hash(legacy);
		assertFalse("The upgraded value still contains the expression",
				upgraded.contains(legacy));
		assertTrue("The passcode stopped working after being upgraded",
				PasscodeHasher.verify(legacy, upgraded));
		assertFalse("The upgraded value wants upgrading again on every unlock",
				PasscodeHasher.needsRehash(upgraded));
	}

	/**
	 * An empty stored value is how stealth mode records that it is off. It must
	 * never match, least of all the empty expression.
	 */
	@Test
	public void anEmptyStoredValueNeverVerifies() {
		assertFalse(PasscodeHasher.verify("", ""));
		assertFalse(PasscodeHasher.verify("1+1", ""));
	}

	/**
	 * A stored value that claims to be a hash but is not one is not a passcode
	 * either. It must fail like a wrong answer rather than throw, because the
	 * caller is a screen that has to keep looking like a calculator.
	 */
	@Test
	public void aMalformedHashFailsRatherThanThrowing() {
		assertFalse(PasscodeHasher.verify(PASSCODE, "argon2id$"));
		assertFalse(PasscodeHasher.verify(PASSCODE, "argon2id$8192$4$1$zz$zz"));
		assertFalse(PasscodeHasher.verify(PASSCODE, "argon2id$8192$4$1$00"));
		assertFalse(PasscodeHasher.verify(PASSCODE,
				"argon2id$8192$4$1$00$00$00"));
		assertFalse(PasscodeHasher.verify(PASSCODE, "argon2id$0$0$0$00$00"));
	}

	/**
	 * Whoever can write this value can also choose its cost. An unbounded
	 * memory parameter turns every unlock attempt into an out-of-memory kill,
	 * which locks the owner out of their own app.
	 */
	@Test
	public void anAbsurdCostIsRejectedRatherThanHonoured() {
		assertFalse(PasscodeHasher.verify(PASSCODE,
				"argon2id$2000000$4$1$00112233445566778899aabbccddeeff$00"));
		assertFalse(PasscodeHasher.verify(PASSCODE,
				"argon2id$8192$100000$1$00112233445566778899aabbccddeeff$00"));
	}

	/**
	 * Verification uses the parameters the value was written with, so the cost
	 * can be raised later without invalidating everyone's passcode.
	 */
	@Test
	public void aHashWrittenWithOlderParametersStillVerifies() {
		byte[] salt = new byte[16];
		for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;
		String weaker = PasscodeHasher.hash(PASSCODE, salt, 512, 1, 1);

		assertTrue("Raising the cost would lock out everyone who set a " +
				"passcode before it was raised",
				PasscodeHasher.verify(PASSCODE, weaker));
		assertFalse(PasscodeHasher.verify("123+45*7", weaker));
		assertTrue("A hash written with weaker parameters is not upgraded",
				PasscodeHasher.needsRehash(weaker));
	}

	/**
	 * Comparison uses {@link java.security.MessageDigest#isEqual}, which stops
	 * at neither the first differing byte nor a length mismatch. Timing is not
	 * assertable here without a flaky measurement, so what this pins down is
	 * what would be lost by going back to {@code equals}: a nearly-right guess
	 * is refused as flatly as one that is nothing like it.
	 */
	@Test
	public void aNearMissIsRefusedNoDifferentlyFromAWildOne() {
		String stored = PasscodeHasher.hash(PASSCODE);
		assertFalse(PasscodeHasher.verify("123+45*5", stored));
		assertFalse(PasscodeHasher.verify("923+45*6", stored));
		assertFalse(PasscodeHasher.verify("9", stored));
		assertFalse(PasscodeHasher.verify(
				"999999999999999999999999999999", stored));

		String legacy = "123+45*6";
		assertFalse(PasscodeHasher.verify("123+45*5", legacy));
		assertFalse(PasscodeHasher.verify("123+45*66", legacy));
		assertFalse(PasscodeHasher.verify("1", legacy));
	}

	@Test
	public void hashingIsDeterministicForAGivenSalt() {
		byte[] salt = new byte[16];
		assertEquals(PasscodeHasher.hash(PASSCODE, salt, 512, 1, 1),
				PasscodeHasher.hash(PASSCODE, salt, 512, 1, 1));
	}
}
