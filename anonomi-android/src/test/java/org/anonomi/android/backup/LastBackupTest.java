package org.anonomi.android.backup;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 24) // Must be >= minSdkVersion or the manifest fails to parse
public class LastBackupTest {

	private static final Uri URI = Uri.parse(
			"content://com.android.externalstorage.documents/document/"
					+ "primary%3ADownload%2Fbackup-20260816.bin");

	@Test
	public void testRoundTrip() {
		String stored = LastBackup.encode(URI, 1786968000000L, "backup.bin");

		LastBackup read = LastBackup.decode(stored);

		assertNotNull(read);
		assertEquals(URI, read.uri);
		assertEquals(1786968000000L, read.created);
		assertEquals("backup.bin", read.displayName);
	}

	@Test
	public void testNameKeepsItsSeparators() {
		// The name is last in the record precisely so that it can hold one
		String name = "odd\nname";
		String stored = LastBackup.encode(URI, 1L, name);

		LastBackup read = LastBackup.decode(stored);

		assertNotNull(read);
		assertEquals(name, read.displayName);
	}

	@Test
	public void testMalformedRecordsAreNotABackup() {
		assertNull(LastBackup.decode(""));
		assertNull(LastBackup.decode("not a record"));
		assertNull(LastBackup.decode("1\ncontent://x"));
		// Time that is not a number
		assertNull(LastBackup.decode("soon\ncontent://x\nbackup.bin"));
		// No scheme, so nothing that could be opened
		assertNull(LastBackup.decode("1\nbackup.bin\nbackup.bin"));
	}
}
