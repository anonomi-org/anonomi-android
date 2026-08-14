package org.anonchatsecure.bramble.account;

import android.content.Context;
import android.content.SharedPreferences;

import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.jmock.Expectations;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.anonchatsecure.bramble.test.TestUtils.deleteTestDirectory;
import static org.anonchatsecure.bramble.test.TestUtils.getTestDirectory;

public class ExternalStorageCleanupTest extends BrambleMockTestCase {

	private final SharedPreferences prefs =
			context.mock(SharedPreferences.class);
	private final SharedPreferences.Editor editor =
			context.mock(SharedPreferences.Editor.class);
	private final Context ctx;

	private final File testDir = getTestDirectory();
	private final File externalFilesDir = new File(testDir, "external");
	private final File fetchedTile =
			new File(externalFilesDir, "tiles/fetched/map/tile");
	private final File importedTile =
			new File(externalFilesDir, "tiles/AnonMapsCache/tile");

	public ExternalStorageCleanupTest() {
		context.setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
		ctx = context.mock(Context.class);
	}

	@Test
	public void testLeavesExternalStorageAloneWhenNothingIsDue()
			throws Exception {
		context.checking(new Expectations() {{
			allowing(ctx).getApplicationContext();
			will(returnValue(ctx));
			oneOf(ctx).getSharedPreferences("external_cleanup", MODE_PRIVATE);
			will(returnValue(prefs));
			oneOf(prefs).getBoolean("due", false);
			will(returnValue(false));
		}});

		assertTrue(fetchedTile.getParentFile().mkdirs());
		assertTrue(fetchedTile.createNewFile());

		new ExternalStorageCleanup(ctx).runIfDue();

		assertTrue(fetchedTile.exists());
	}

	@Test
	public void testDeletesExternalStorageAndClearsMarkWhenDue()
			throws Exception {
		context.checking(new Expectations() {{
			allowing(ctx).getApplicationContext();
			will(returnValue(ctx));
			oneOf(ctx).getSharedPreferences("external_cleanup", MODE_PRIVATE);
			will(returnValue(prefs));
			oneOf(prefs).getBoolean("due", false);
			will(returnValue(true));
			// A device with no secondary external storage reports it as null
			oneOf(ctx).getExternalFilesDirs(null);
			will(returnValue(new File[] {externalFilesDir, null}));
			oneOf(prefs).edit();
			will(returnValue(editor));
			oneOf(editor).remove("due");
			will(returnValue(editor));
			oneOf(editor).commit();
			will(returnValue(true));
		}});

		assertTrue(fetchedTile.getParentFile().mkdirs());
		assertTrue(fetchedTile.createNewFile());
		assertTrue(importedTile.getParentFile().mkdirs());
		assertTrue(importedTile.createNewFile());

		new ExternalStorageCleanup(ctx).runIfDue();

		assertFalse(fetchedTile.exists());
		assertFalse(importedTile.exists());
		assertFalse(externalFilesDir.exists());
	}

	@After
	public void tearDown() {
		deleteTestDirectory(testDir);
	}
}
