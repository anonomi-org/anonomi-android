package org.anonomi.android.backup;

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

	@Test
	public void testRoundTrip() {
		String stored = LastBackup.encode(1786968000000L);

		LastBackup read = LastBackup.decode(stored);

		assertNotNull(read);
		assertEquals(1786968000000L, read.created);
	}

	@Test
	public void testMalformedRecordsAreNotABackup() {
		assertNull(LastBackup.decode(""));
		assertNull(LastBackup.decode("soon"));
		// The record used to carry the file's whereabouts as well; an old one
		// is not read back rather than being read back wrongly
		assertNull(LastBackup.decode(
				"1786968000000\ncontent://x/1\nbackup.bin"));
	}
}
