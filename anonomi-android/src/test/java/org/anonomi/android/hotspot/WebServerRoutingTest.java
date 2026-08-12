package org.anonomi.android.hotspot;

import org.anonomi.android.hotspot.WebServer.ApkRoute;
import org.anonomi.android.hotspot.WebServer.AssetCheck;
import org.junit.Test;

import static org.anonomi.android.hotspot.WebServer.resolveApk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the path to asset routing decision. Recipients install whatever
 * this server hands them, so serving the wrong file is the worst thing it can
 * do.
 * <p>
 * The URIs here are what {@code IHTTPSession#getUri()} yields, which NanoHTTPD
 * has already stripped of its query string and percent-decoded (verified in
 * {@code NanoHTTPD.decodeHeader}). Escaped traversal therefore arrives at the
 * resolver already decoded, which is why the decoded forms are what is tested.
 */
public class WebServerRoutingTest {

	/**
	 * The name the app offers its own APK under, from
	 * {@link HotspotViewModel#getApkFileName()}.
	 */
	private static final String INSTALLED_APK = "anonomi-1.4.0.apk";

	/** Every companion APK bundled and valid, as on the official flavour. */
	private static final AssetCheck ALL_PRESENT = assetName -> true;

	/** No companion APK bundled at all, as on the fdroid flavour. */
	private static final AssetCheck NONE_PRESENT = assetName -> false;

	/**
	 * Everything bundled except tor-browser.apk, which is the Git LFS pointer
	 * rather than an APK because the object was never pulled.
	 */
	private static final AssetCheck TOR_BROWSER_IS_LFS_POINTER =
			assetName -> !assetName.equals("tor-browser.apk");

	// --- exact known paths resolve to the right asset ------------------------

	@Test
	public void eachCompanionApkResolvesToItsOwnAsset() {
		assertAsset("anonomi-postbox.apk", "/anonomi-postbox.apk");
		assertAsset("monerujo.apk", "/monerujo.apk");
		assertAsset("orbot.apk", "/orbot.apk");
		assertAsset("tor-browser.apk", "/tor-browser.apk");
	}

	@Test
	public void versionedAppPathResolvesToInstalledApk() {
		ApkRoute route = resolveApk("/" + INSTALLED_APK, INSTALLED_APK,
				ALL_PRESENT);
		assertTrue("the app's own download link must serve the installed APK",
				route != null && route.isInstalledApk());
	}

	@Test
	public void unrewrittenTemplateAppPathIsNotServed() {
		// hotspot.html ships href="/app.apk", but getHtml always rewrites it to
		// the versioned name, so no served page ever links here and the server
		// should not answer a path it never advertises
		assertNull(resolveApk("/app.apk", INSTALLED_APK, ALL_PRESENT));
	}

	// --- no fall-through to the installed APK --------------------------------

	@Test
	public void unknownApkPathDoesNotFallThroughToInstalledApk() {
		assertNull("GET /evil.apk must 404, not serve the installed app APK",
				resolveApk("/evil.apk", INSTALLED_APK, ALL_PRESENT));
	}

	@Test
	public void ambiguousCompoundNamesResolveDeterministically() {
		// Under substring matching these depended on map order. Neither names
		// a bundled asset, so both are now refused rather than guessed at.
		assertNull(resolveApk("/monerujo-orbot.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/orbot-monerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
	}

	@Test
	public void namesMerelyContainingAnAllowedNameAreRefused() {
		assertNull(resolveApk("/evil-monerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/monerujo.apk.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/postbox.apk", INSTALLED_APK, ALL_PRESENT));
	}

	@Test
	public void matchingIsCaseSensitive() {
		assertNull(resolveApk("/MONERUJO.APK", INSTALLED_APK, ALL_PRESENT));
	}

	@Test
	public void nonApkAndEmptyPathsResolveToNothing() {
		assertNull(resolveApk("/", INSTALLED_APK, ALL_PRESENT));
		assertNull(resolveApk("", INSTALLED_APK, ALL_PRESENT));
		assertNull(resolveApk("/index.html", INSTALLED_APK, ALL_PRESENT));
		assertNull("a relative path is not a request we answer",
				resolveApk("monerujo.apk", INSTALLED_APK, ALL_PRESENT));
	}

	// --- an asset that is not really an APK is not served --------------------

	@Test
	public void assetFailingTheApkCheckIsNotServed() {
		assertNull("a Git LFS pointer must not be served as an APK",
				resolveApk("/tor-browser.apk", INSTALLED_APK,
						TOR_BROWSER_IS_LFS_POINTER));
		// the other assets are unaffected
		assertAsset("orbot.apk", "/orbot.apk", TOR_BROWSER_IS_LFS_POINTER);
	}

	@Test
	public void onFdroidCompanionApksAreAbsentAndSimply404() {
		assertNull(resolveApk("/anonomi-postbox.apk", INSTALLED_APK,
				NONE_PRESENT));
		assertNull(resolveApk("/monerujo.apk", INSTALLED_APK, NONE_PRESENT));
		assertNull(resolveApk("/orbot.apk", INSTALLED_APK, NONE_PRESENT));
		assertNull(resolveApk("/tor-browser.apk", INSTALLED_APK,
				NONE_PRESENT));
	}

	@Test
	public void onFdroidTheAppItselfIsStillServed() {
		ApkRoute route = resolveApk("/" + INSTALLED_APK, INSTALLED_APK,
				NONE_PRESENT);
		assertTrue("the app APK is not an asset and must still be served",
				route != null && route.isInstalledApk());
	}

	// --- path traversal ------------------------------------------------------

	@Test
	public void traversalAttemptsDoNotEscapeTheAllowlist() {
		assertNull(resolveApk("/../../foo.apk", INSTALLED_APK, ALL_PRESENT));
		assertNull(resolveApk("/../monerujo.apk", INSTALLED_APK, ALL_PRESENT));
		assertNull(resolveApk("/assets/../monerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/subdir/monerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("//monerujo.apk", INSTALLED_APK, ALL_PRESENT));
		assertNull(resolveApk("/monerujo.apk/", INSTALLED_APK, ALL_PRESENT));
		assertNull("a backslash must not be read as a separator either",
				resolveApk("/..\\monerujo.apk", INSTALLED_APK, ALL_PRESENT));
	}

	@Test
	public void encodedTraversalDoesNotEscapeTheAllowlist() {
		// NanoHTTPD decodes before serve() runs, so this is what the resolver
		// actually receives for a %2e%2e%2f request
		assertNull(resolveApk("/../monerujo.apk", INSTALLED_APK, ALL_PRESENT));
		// and an undecoded sequence is no better
		assertNull(resolveApk("/%2e%2e%2fmonerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/%2F..%2Fmonerujo.apk", INSTALLED_APK,
				ALL_PRESENT));
	}

	@Test
	public void embeddedNulAndQueryCharactersAreRefused() {
		assertNull(resolveApk("/monerujo.apk\0.txt", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/monerujo.apk?x=1", INSTALLED_APK,
				ALL_PRESENT));
		assertNull(resolveApk("/monerujo.apk#frag", INSTALLED_APK,
				ALL_PRESENT));
	}

	// --- helpers -------------------------------------------------------------

	private void assertAsset(String expectedAsset, String uri) {
		assertAsset(expectedAsset, uri, ALL_PRESENT);
	}

	private void assertAsset(String expectedAsset, String uri,
			AssetCheck assetCheck) {
		ApkRoute route = resolveApk(uri, INSTALLED_APK, assetCheck);
		assertEquals(uri + " must resolve to " + expectedAsset,
				ApkRoute.asset(expectedAsset), route);
	}
}
