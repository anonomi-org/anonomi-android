package org.anonomi.android.panic;

import org.anonomi.android.util.SecureValue;
import org.junit.Test;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Someone configures "delete account", their stored setting later fails to
 * decrypt, and they trigger panic under duress. They must not get the
 * weakest action because we could not read the strongest one.
 */
public class PanicActionPolicyTest {

	@Test
	public void unreadableActionDoesNotResolveToSignOut() {
		String action = PanicActionPolicy.resolve(null,
				SecureValue.unreadable("decryption failed"));
		assertNotEquals("An unreadable panic action resolved to the weakest " +
				"action, so a configured account deletion silently became a " +
				"sign-out", ACTION_SIGN_OUT, action);
	}

	@Test
	public void unreadableActionResolvesToDeleteAccount() {
		assertEquals(ACTION_DELETE_ACCOUNT, PanicActionPolicy.resolve(null,
				SecureValue.unreadable("decryption failed")));
	}

	/**
	 * A value that decrypts but is not an action we recognise is no more
	 * readable than one that does not decrypt at all.
	 */
	@Test
	public void unrecognisedActionDoesNotResolveToSignOut() {
		assertEquals(ACTION_DELETE_ACCOUNT, PanicActionPolicy.resolve(null,
				SecureValue.present("not_an_action")));
	}

	@Test
	public void emptyStoredActionDoesNotResolveToSignOut() {
		assertEquals(ACTION_DELETE_ACCOUNT,
				PanicActionPolicy.resolve(null, SecureValue.present("")));
	}

	/**
	 * Absent is not unreadable: someone who never configured an action has not
	 * asked for their account to be deleted.
	 */
	@Test
	public void absentActionResolvesToSignOut() {
		assertEquals(ACTION_SIGN_OUT,
				PanicActionPolicy.resolve(null, SecureValue.absent()));
	}

	@Test
	public void storedActionIsHonoured() {
		assertEquals(ACTION_DELETE_ACCOUNT, PanicActionPolicy.resolve(null,
				SecureValue.present(ACTION_DELETE_ACCOUNT)));
		assertEquals(ACTION_SIGN_OUT, PanicActionPolicy.resolve(null,
				SecureValue.present(ACTION_SIGN_OUT)));
		assertEquals(ACTION_SHOW_DIALOG, PanicActionPolicy.resolve(null,
				SecureValue.present(ACTION_SHOW_DIALOG)));
	}

	/**
	 * The dialog passes the choice the user just made; it must win over
	 * whatever is stored, including over an unreadable stored value.
	 */
	@Test
	public void intentOverrideWins() {
		assertEquals(ACTION_SIGN_OUT, PanicActionPolicy.resolve(ACTION_SIGN_OUT,
				SecureValue.present(ACTION_DELETE_ACCOUNT)));
		assertEquals(ACTION_SIGN_OUT, PanicActionPolicy.resolve(ACTION_SIGN_OUT,
				SecureValue.unreadable("decryption failed")));
	}
}
