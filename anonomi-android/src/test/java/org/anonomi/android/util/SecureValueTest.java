package org.anonomi.android.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The contract that replaces the old three-outcomes-one-null read.
 */
public class SecureValueTest {

	@Test
	public void absentIsNotUnreadable() {
		assertTrue(SecureValue.absent().isAbsent());
		assertFalse(SecureValue.absent().isUnreadable());
		assertTrue(SecureValue.unreadable("nope").isUnreadable());
		assertFalse(SecureValue.unreadable("nope").isAbsent());
		assertNotEquals(SecureValue.absent().getState(),
				SecureValue.unreadable("nope").getState());
	}

	/**
	 * A value that decrypted to the empty string is a value, not a missing
	 * one. Clearing the stealth passcode stores exactly this.
	 */
	@Test
	public void emptyIsPresent() {
		SecureValue empty = SecureValue.present("");
		assertTrue(empty.isPresent());
		assertFalse(empty.isAbsent());
		assertEquals("", empty.get());
	}

	@Test
	public void getReturnsTheValueWhenPresent() {
		assertEquals("delete_account",
				SecureValue.present("delete_account").get());
	}

	@Test
	public void getRefusesWhenThereIsNoValue() {
		assertGetThrows(SecureValue.absent());
		assertGetThrows(SecureValue.unreadable("decryption failed"));
	}

	@Test
	public void orIfAbsentSuppliesTheDefaultOnlyForAbsent() {
		assertEquals("stored",
				SecureValue.present("stored").orIfAbsent("fallback"));
		assertEquals("fallback",
				SecureValue.absent().orIfAbsent("fallback"));
	}

	/**
	 * The point of the type. A caller that wants a default for "never set"
	 * must still say what it wants for "could not be read", rather than
	 * getting the same answer for both by accident.
	 */
	@Test
	public void orIfAbsentRefusesToDefaultAnUnreadableValue() {
		try {
			SecureValue.unreadable("decryption failed").orIfAbsent("fallback");
			fail("orIfAbsent silently supplied a default for an unreadable " +
					"value, which is the ambiguity this type removes");
		} catch (IllegalStateException expected) {
			// what we want
		}
	}

	@Test
	public void toStringDoesNotLeakTheValue() {
		assertFalse(SecureValue.present("1+1").toString().contains("1+1"));
	}

	private void assertGetThrows(SecureValue value) {
		try {
			value.get();
			fail("expected IllegalStateException for " + value.getState());
		} catch (IllegalStateException expected) {
			// what we want
		}
	}
}
