package org.anonchatsecure.bramble.api.account;

import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import static org.anonchatsecure.bramble.api.account.BackupConstants.RECOVERY_CODE_DIGITS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RecoveryCodeTest extends BrambleTestCase {

	private static final String CODE = "123456789012345678901234567890";

	private final SecureRandom random = new SecureRandom();

	@Test
	public void testGeneratesDistinctCodesOfDigits() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			String code = RecoveryCode.generate(random);
			assertEquals(RECOVERY_CODE_DIGITS, code.length());
			for (int j = 0; j < code.length(); j++) {
				char c = code.charAt(j);
				assertTrue(c >= '0' && c <= '9');
			}
			codes.add(code);
		}
		assertEquals(100, codes.size());
	}

	@Test
	public void testGeneratedCodesNormaliseToThemselves() {
		for (int i = 0; i < 20; i++) {
			String code = RecoveryCode.generate(random);
			assertEquals(code, RecoveryCode.normalise(code));
			assertEquals(code, RecoveryCode.normalise(RecoveryCode.format(code)));
		}
	}

	@Test
	public void testFormatSplitsTheCodeIntoGroups() {
		assertEquals("12345 67890 12345 67890 12345 67890",
				RecoveryCode.format(CODE));
	}

	@Test
	public void testNormaliseIgnoresSpacesAndDashes() {
		assertEquals(CODE, RecoveryCode.normalise(CODE));
		assertEquals(CODE, RecoveryCode.normalise(
				"12345-67890-12345-67890-12345-67890"));
		assertEquals(CODE, RecoveryCode.normalise(
				" 12345 67890\t12345\n67890 12345 67890 "));
	}

	@Test
	public void testNormaliseAcceptsDigitsFromOtherScripts() {
		// Arabic-Indic digits, as a keypad in another locale sends them
		String group = "\u0661\u0662\u0663\u0664\u0665"
				+ "\u0666\u0667\u0668\u0669\u0660";
		assertEquals(CODE, RecoveryCode.normalise(group + group + group));
	}

	@Test
	public void testNormaliseRejectsAnythingElse() {
		assertNull(RecoveryCode.normalise(""));
		assertNull(RecoveryCode.normalise(CODE.substring(1)));
		assertNull(RecoveryCode.normalise(CODE + "1"));
		assertNull(RecoveryCode.normalise(CODE.substring(1) + "a"));
		assertNull(RecoveryCode.normalise("12345678901234567890123456789x"));
		assertNotNull(RecoveryCode.normalise(CODE));
	}
}
