package org.anonomi.android.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.content.Context.MODE_PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Moving a stored value from one preference file to another, which is how the
 * disguise leaves the file an account deletion clears.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 24) // Must be >= minSdkVersion or the manifest fails to parse
public class SecurePrefsMoveEntryTest {

	private static final String KEY = "key";

	private SharedPreferences from, to;

	@Before
	public void setUp() {
		Context ctx = RuntimeEnvironment.getApplication();
		from = ctx.getSharedPreferences("from", MODE_PRIVATE);
		to = ctx.getSharedPreferences("to", MODE_PRIVATE);
		from.edit().clear().commit();
		to.edit().clear().commit();
	}

	@Test
	public void testMovesTheValueAndLeavesNoCopyBehind() {
		from.edit().putString(KEY, "ciphertext").commit();
		SecurePrefsManager.moveEntry(from, to, KEY);
		assertEquals("ciphertext", to.getString(KEY, null));
		assertFalse(from.contains(KEY));
	}

	/**
	 * Moved as it is stored rather than decrypted and written again, so a value
	 * that cannot be read arrives still unreadable. Arriving as one that was
	 * never set would say the disguise had not been configured.
	 */
	@Test
	public void testMovesAValueThatWillNotDecryptUnchanged() {
		from.edit().putString(KEY, "not a record").commit();
		SecurePrefsManager.moveEntry(from, to, KEY);
		assertEquals("not a record", to.getString(KEY, null));
		assertFalse(from.contains(KEY));
	}

	@Test
	public void testWritesNothingWhenThereIsNothingToMove() {
		SecurePrefsManager.moveEntry(from, to, KEY);
		assertFalse(to.contains(KEY));
	}

	/**
	 * The move has already happened, and what is in the destination is what the
	 * user last set. A stale copy must not replace it.
	 */
	@Test
	public void testDoesNotOverwriteAValueAlreadyThere() {
		from.edit().putString(KEY, "stale").commit();
		to.edit().putString(KEY, "current").commit();
		SecurePrefsManager.moveEntry(from, to, KEY);
		assertEquals("current", to.getString(KEY, null));
		assertFalse(from.contains(KEY));
	}

	/**
	 * Only putEncrypted writes this key and it only writes strings, so anything
	 * else was planted. It is not carried over.
	 */
	@Test
	public void testLeavesAValueOfAnotherTypeWhereItIs() {
		from.edit().putBoolean(KEY, true).commit();
		SecurePrefsManager.moveEntry(from, to, KEY);
		assertFalse(to.contains(KEY));
		assertTrue(from.contains(KEY));
	}
}
