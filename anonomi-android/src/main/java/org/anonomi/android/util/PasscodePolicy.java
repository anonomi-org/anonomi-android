package org.anonomi.android.util;

import androidx.annotation.Nullable;

/**
 * What counts as a usable calculator passcode. The floor is low on purpose -
 * hashing and backoff do most of the work - and only rules out what those two
 * cannot help with, a passcode someone idly pressing keys would find.
 */
public final class PasscodePolicy {

	/**
	 * Every character the keypad can produce, and so the only characters a
	 * passcode may contain: accepting one it cannot produce guarantees a
	 * lockout. The two keys drawn as parentheses send {@code %} and {@code @}.
	 */
	private static final String KEYPAD = "0123456789.+-*/%@";

	private static final String OPERATORS = "+-*/%@";

	public static final int MIN_LENGTH = 4;
	public static final int MIN_OPERATORS = 1;
	public static final int MIN_OPERANDS = 2;

	/**
	 * Counting operators and operands measures shape, not variety, and the two
	 * come apart: {@code 1+1+1} satisfies every other rule here on one
	 * repeated key.
	 */
	public static final int MIN_DISTINCT_DIGITS = 2;

	private PasscodePolicy() {
	}

	/**
	 * The form a passcode is compared and stored in. Whitespace is dropped
	 * because it is chosen on a keyboard that can produce it and entered on a
	 * keypad that cannot.
	 */
	public static String normalise(@Nullable String expression) {
		if (expression == null) return "";
		StringBuilder cleaned = new StringBuilder(expression.length());
		for (int i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			if (!Character.isWhitespace(c)) cleaned.append(c);
		}
		return cleaned.toString();
	}

	/**
	 * Applied where a passcode is set, never where one is checked: an existing
	 * passcode that would fail this is still its owner's way into their app.
	 */
	public static boolean isAcceptable(@Nullable String expression) {
		String cleaned = normalise(expression);
		if (cleaned.length() < MIN_LENGTH) return false;

		int operators = 0, operands = 0;
		boolean withinOperand = false;
		boolean[] digitSeen = new boolean[10];
		for (int i = 0; i < cleaned.length(); i++) {
			char c = cleaned.charAt(i);
			if (KEYPAD.indexOf(c) < 0) return false;
			if (OPERATORS.indexOf(c) >= 0) {
				operators++;
				withinOperand = false;
			} else {
				if (c != '.') digitSeen[c - '0'] = true;
				if (!withinOperand) {
					operands++;
					withinOperand = true;
				}
			}
		}
		int distinctDigits = 0;
		for (boolean seen : digitSeen) if (seen) distinctDigits++;
		return operators >= MIN_OPERATORS && operands >= MIN_OPERANDS
				&& distinctDigits >= MIN_DISTINCT_DIGITS;
	}
}
