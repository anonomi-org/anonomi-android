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

	/**
	 * The reason is for diagnosis, so it has to reach the exception a caller
	 * would actually see.
	 */
	@Test
	public void refusingToReadAnUnreadableValueExplainsWhy() {
		try {
			SecureValue.unreadable("record has no separator").get();
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("record has no separator"));
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
