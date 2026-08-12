package org.anonomi.android.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Two ways to lose: a passcode nobody would have to guess, and a passcode its
 * owner cannot type. The keypad has ten digits, a dot and six operator keys,
 * and that is the whole alphabet.
 */
public class PasscodePolicyTest {

	@Test
	public void aPasscodeAnyoneWouldTryIsRejected() {
		assertFalse("1+1 was accepted as a passcode",
				PasscodePolicy.isAcceptable("1+1"));
		assertFalse(PasscodePolicy.isAcceptable("2+2"));
		assertFalse(PasscodePolicy.isAcceptable("1+2"));
		assertFalse(PasscodePolicy.isAcceptable("12+34"));
	}

	@Test
	public void somethingLongerWithMoreStepsIsAccepted() {
		assertTrue(PasscodePolicy.isAcceptable("123+45*6"));
		assertTrue(PasscodePolicy.isAcceptable("7*8-91/2"));
		assertTrue(PasscodePolicy.isAcceptable("1.5+2.25*3"));
		assertTrue(PasscodePolicy.isAcceptable("100%7+3-1"));
	}

	/**
	 * The other rules measure shape, and shape is cheap to satisfy on one key:
	 * every one of these is long enough, has enough operators and enough
	 * operands, and is worth a handful of guesses.
	 */
	@Test
	public void anExpressionBuiltFromOneRepeatedKeyIsRejected() {
		assertFalse("1+1+1+1+1 was accepted as a passcode",
				PasscodePolicy.isAcceptable("1+1+1+1+1"));
		assertFalse(PasscodePolicy.isAcceptable("2+2+2+2+2"));
		assertFalse(PasscodePolicy.isAcceptable("1*1*1*1*1"));
		assertFalse(PasscodePolicy.isAcceptable("9-9-9-9-9"));
	}

	/**
	 * Two digits is not variety either, however they are arranged.
	 */
	@Test
	public void anExpressionWithTooFewDistinctDigitsIsRejected() {
		assertFalse(PasscodePolicy.isAcceptable("12+12+12"));
		assertFalse(PasscodePolicy.isAcceptable("1+2+1+2+1"));
		assertFalse(PasscodePolicy.isAcceptable("11*22-11"));
	}

	@Test
	public void anExpressionWithoutEnoughStepsIsRejected() {
		// long enough, but only one operator and two operands
		assertFalse(PasscodePolicy.isAcceptable("12345+6789"));
		assertFalse(PasscodePolicy.isAcceptable("1234567+8"));
	}

	@Test
	public void somethingThatIsNotAnExpressionIsRejected() {
		assertFalse(PasscodePolicy.isAcceptable(null));
		assertFalse(PasscodePolicy.isAcceptable(""));
		assertFalse(PasscodePolicy.isAcceptable("   "));
		assertFalse(PasscodePolicy.isAcceptable("123456789"));
		assertFalse(PasscodePolicy.isAcceptable("+++++++++"));
	}

	/**
	 * The two keys drawn as parentheses send modulus and negate, so a passcode
	 * containing a real parenthesis can be chosen and then never entered.
	 */
	@Test
	public void anExpressionTheKeypadCannotProduceIsRejected() {
		assertFalse("A passcode containing a character the keypad cannot " +
						"produce was accepted, so it could never be entered",
				PasscodePolicy.isAcceptable("(12+34)*56"));
		assertFalse(PasscodePolicy.isAcceptable("123+45*6a"));
		assertFalse(PasscodePolicy.isAcceptable("123+45*6="));
	}

	@Test
	public void theKeysThoseTwoActuallySendAreAccepted() {
		assertTrue(PasscodePolicy.isAcceptable("123%45*6"));
		assertTrue(PasscodePolicy.isAcceptable("123@45+6@"));
	}

	@Test
	public void whitespaceIsNotPartOfAPasscode() {
		assertEquals("123+45*6", PasscodePolicy.normalise(" 123 + 45 * 6 "));
		assertEquals("123+45*6", PasscodePolicy.normalise("123+45*6"));
		assertEquals("", PasscodePolicy.normalise(null));
		assertEquals("", PasscodePolicy.normalise("  \t\n "));
		assertTrue(PasscodePolicy.isAcceptable(" 123 + 45 * 6 "));
	}
}
