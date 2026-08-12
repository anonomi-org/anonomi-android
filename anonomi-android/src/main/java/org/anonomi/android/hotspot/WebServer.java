package org.anonomi.android.hotspot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.anonomi.R;
import org.briarproject.nullsafety.NotNullByDefault;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import fi.iki.elonen.NanoHTTPD;

import static android.util.Xml.Encoding.UTF_8;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.BuildConfig.VERSION_NAME;
import static org.anonomi.android.hotspot.HotspotViewModel.getApkFileName;

@NotNullByDefault
class WebServer extends NanoHTTPD {

	final static int PORT = 9999;

	private static final Logger LOG = getLogger(WebServer.class.getName());
	private static final String FILE_HTML = "hotspot.html";

	private static final String MIME_APK = "application/vnd.android.package-archive";
	private static final Pattern REGEX_AGENT = Pattern.compile("Android ([0-9]+)");

	/**
	 * The bundled companion APKs, named by the exact request path each one is
	 * offered under. The same names are passed to {@link #setButton} to build
	 * the page, so the two lists have to be kept in step by hand: adding an
	 * asset here but not there widens what the server will hand out beyond
	 * what it offers.
	 */
	private static final Set<String> APK_ASSETS =
			unmodifiableSet(new HashSet<>(asList(
					"anonomi-postbox.apk",
					"monerujo.apk",
					"orbot.apk",
					"tor-browser.apk")));

	/**
	 * Tests whether a bundled asset is present in this build and really is an
	 * APK. Only the official flavour bundles the companion APKs, so on fdroid
	 * this returns false for all of them, which is the normal case and not an
	 * error.
	 */
	interface AssetCheck {
		boolean isApk(String assetName);
	}

	/**
	 * The file an APK request resolved to. Either the APK of the app itself,
	 * read back from the installed package, or one named bundled asset.
	 */
	static final class ApkRoute {

		static final ApkRoute INSTALLED_APK = new ApkRoute(null);

		@Nullable
		private final String assetName;

		private ApkRoute(@Nullable String assetName) {
			this.assetName = assetName;
		}

		static ApkRoute asset(String assetName) {
			return new ApkRoute(assetName);
		}

		boolean isInstalledApk() {
			return assetName == null;
		}

		@Nullable
		String getAssetName() {
			return assetName;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (!(o instanceof ApkRoute)) return false;
			String other = ((ApkRoute) o).assetName;
			return assetName == null ? other == null : assetName.equals(other);
		}

		@Override
		public int hashCode() {
			return assetName == null ? 0 : assetName.hashCode();
		}

		@Override
		public String toString() {
			return assetName == null ? "ApkRoute[installed APK]"
					: "ApkRoute[asset " + assetName + "]";
		}
	}

	/**
	 * Decides which file, if any, an APK request is allowed to fetch. Pure: it
	 * touches neither Android nor the network, so the routing decision can be
	 * unit tested without standing up a server.
	 *
	 * @param uri the request path, as returned by
	 * 		{@link IHTTPSession#getUri()}. NanoHTTPD has already stripped the
	 * 		query string and percent-decoded it, so escaped traversal sequences
	 * 		arrive here already decoded.
	 * @param installedApkName the file name under which the app offers its own
	 * 		APK, from {@link HotspotViewModel#getApkFileName()}
	 * @param assetCheck tests whether a bundled asset can actually be served
	 * @return the file to serve, or null if the request must be answered with
	 * 		404
	 */
	@Nullable
	static ApkRoute resolveApk(String uri, String installedApkName,
			AssetCheck assetCheck) {
		String name = fileName(uri);
		if (name == null) return null;
		if (name.equals(installedApkName)) return ApkRoute.INSTALLED_APK;
		if (!APK_ASSETS.contains(name)) return null;
		// Never hand over an asset that is absent or is not really an APK.
		// tor-browser.apk is stored in Git LFS and builds without the object
		// pulled package the ~130 byte text pointer under the same name. On
		// fdroid none of the companion APKs are bundled at all, so a negative
		// answer here is the normal case rather than an error.
		if (!assetCheck.isApk(name)) return null;
		return ApkRoute.asset(name);
	}

	/**
	 * The single file name a request path names, or null if it is anything
	 * else. Traversal segments are rejected outright rather than normalised
	 * away, so no input can resolve to a name outside the allowlist.
	 * <p>
	 * NanoHTTPD has already removed the query string, so a '?' or '#' reaching
	 * this method came from percent-encoding and is treated as a bad request.
	 */
	@Nullable
	private static String fileName(String uri) {
		if (!uri.startsWith("/")) return null;
		String name = uri.substring(1);
		if (name.isEmpty()) return null;
		for (int i = 0; i < name.length(); i++) {
			switch (name.charAt(i)) {
				case '/':
				case '\\':
				case '?':
				case '#':
				case '\0':
					return null;
			}
		}
		return name;
	}

	private final Context ctx;

	/**
	 * @param bindAddress the address of the hotspot interface to listen on.
	 * 		This must not be null: NanoHTTPD reads a null host as the wildcard
	 * 		address, which would also expose the server to any other network the
	 * 		device is attached to.
	 */
	WebServer(Context ctx, String bindAddress) {
		super(bindAddress, PORT);
		this.ctx = ctx;
	}

	@Override
	public void start() throws IOException {
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	@Override
	public Response serve(IHTTPSession session) {
		String uri = session.getUri();

		if (uri.endsWith("favicon.ico")) {
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT, NOT_FOUND.getDescription());
		}

		// The resolver alone decides what may be served, so that a request
		// which looks like an APK can never fall through to the page instead
		// of being refused.
		ApkRoute route = resolveApk(uri, getApkFileName(), this::assetExists);
		if (route != null) {
			if (route.isInstalledApk()) return serveInstalledApk();
			return serveAssetFile(requireNonNull(route.getAssetName()),
					MIME_APK);
		}
		if (isApkRequest(uri)) {
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT,
					NOT_FOUND.getDescription());
		}

		try {
			String html = getHtml(session.getHeaders().get("user-agent"));
			return newFixedLengthResponse(OK, MIME_HTML, html);
		} catch (Exception e) {
			logException(LOG, WARNING, e);
			return newFixedLengthResponse(INTERNAL_ERROR, MIME_PLAINTEXT,
					ctx.getString(R.string.hotspot_error_web_server_serve));
		}
	}

	/**
	 * True if the request asks for an APK by name, whatever it resolved to.
	 * Matched case-insensitively so that this cannot disagree with
	 * {@link #resolveApk} about whether a refusal or the page is the right
	 * answer.
	 */
	private static boolean isApkRequest(String uri) {
		return uri.toLowerCase(Locale.US).endsWith(".apk");
	}

	private Response serveInstalledApk() {
		File file = new File(ctx.getPackageCodePath());
		long fileLen = file.length();

		try {
			FileInputStream fis = new FileInputStream(file);
			// No explicit Content-Length header: NanoHTTPD emits one from the
			// length passed here, and adding it by hand emits it twice.
			return newFixedLengthResponse(OK, MIME_APK, fis, fileLen);
		} catch (FileNotFoundException e) {
			logException(LOG, WARNING, e);
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT,
					ctx.getString(R.string.hotspot_error_web_server_serve));
		}
	}

	private Response serveAssetFile(String assetName, String mime) {
		try {
			InputStream is = ctx.getAssets().open(assetName);
			long length = assetLength(assetName, is);
			if (length < 0) {
				// No exact length to be had. Chunked transfer is correct
				// without a Content-Length, whereas announcing a length we
				// cannot stand behind risks handing over a truncated APK.
				return newChunkedResponse(OK, mime, is);
			}
			return newFixedLengthResponse(OK, mime, is, length);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT,
					ctx.getString(R.string.hotspot_error_web_server_serve));
		}
	}

	/**
	 * The exact number of bytes {@code is} will yield, or -1 if that cannot be
	 * established.
	 * <p>
	 * {@link android.content.res.AssetManager#openFd(String)} is the documented
	 * way to ask, but it only succeeds for assets the build stored
	 * uncompressed, and the companion APKs are currently deflated. For those,
	 * AssetInputStream#available() reports the whole uncompressed length of a
	 * freshly opened stream, so it is exact here even though InputStream in
	 * general only promises an estimate. A saturated int is treated as unknown
	 * rather than trusted, since that is the one case where it would be short.
	 */
	private long assetLength(String assetName, InputStream is) {
		try (AssetFileDescriptor afd = ctx.getAssets().openFd(assetName)) {
			long length = afd.getLength();
			if (length >= 0) return length;
		} catch (IOException e) {
			// stored compressed, so fall back to the stream
		}
		try {
			int available = is.available();
			if (available > 0 && available < Integer.MAX_VALUE) return available;
		} catch (IOException e) {
			// fall through to the chunked path
		}
		return -1;
	}

	private String getHtml(@Nullable String userAgent) throws Exception {
		Document doc;
		try (InputStream is = ctx.getAssets().open(FILE_HTML)) {
			doc = Jsoup.parse(is, UTF_8.name(), "");
		}
		String filename = getApkFileName();

		// AnonChat
		requireNonNull(doc.selectFirst("#download_title"))
				.text(ctx.getString(R.string.website_download_title_1, VERSION_NAME));
		requireNonNull(doc.selectFirst("#download_intro"))
				.text(ctx.getString(R.string.website_download_intro_1));
		Element httpNotice = doc.selectFirst("#http_notice_text");
		if (httpNotice != null) {
			httpNotice.text(ctx.getString(R.string.website_http_notice));
		}
		// Root-absolute, like the hrefs the template ships: the resolver only
		// accepts top-level names, so a relative href would break the download
		// for anyone who loaded the page at anything but "/".
		requireNonNull(doc.selectFirst(".button")).attr("href", "/" + filename);
		requireNonNull(doc.selectFirst("#download_button"))
				.text(ctx.getString(R.string.website_download_button));

		// Companion app buttons. Only the official flavour bundles these, so
		// each one is removed unless its asset is actually present.
		setButton(doc, "#mailbox_button", "anonomi-postbox.apk",
				R.string.website_download_mailbox_button);
		setButton(doc, "#monerujo_button", "monerujo.apk",
				R.string.website_download_monerujo_button);
		setButton(doc, "#orbot_button", "orbot.apk",
				R.string.website_download_orbot_button);
		setButton(doc, "#torbrowser_button", "tor-browser.apk",
				R.string.website_download_torbrowser_button);

		// Footer
		requireNonNull(doc.selectFirst("#download_outro"))
				.text(ctx.getString(R.string.website_download_outro));
		requireNonNull(doc.selectFirst("#troubleshooting_title"))
				.text(ctx.getString(R.string.website_troubleshooting_title));
		requireNonNull(doc.selectFirst("#troubleshooting_1"))
				.text(ctx.getString(R.string.website_troubleshooting_1));
		requireNonNull(doc.selectFirst("#troubleshooting_2"))
				.text(getUnknownSourcesString(userAgent));

		return doc.outerHtml();
	}

	private void setButton(Document doc, String selector, String assetName,
			int textRes) {
		Element btn = doc.selectFirst(selector);
		if (btn == null) return;
		if (!assetExists(assetName)) {
			// Offering a download we cannot serve would only produce a 404
			btn.remove();
			return;
		}
		btn.attr("href", "/" + assetName);
		Element span = btn.selectFirst("span");
		if (span != null) {
			span.text(ctx.getString(textRes));
		} else {
			btn.text(ctx.getString(textRes));
		}
	}

	/**
	 * True if the asset is present and really is an APK. The check matters
	 * because tor-browser.apk is stored in Git LFS: without the object pulled,
	 * the build packages the ~130 byte text pointer under the same name, which
	 * would otherwise be offered as a download.
	 * <p>
	 * This guards both the button in the page and {@link #resolveApk}, so a
	 * direct request cannot reach a file the page declined to link to.
	 */
	private boolean assetExists(String assetName) {
		try (InputStream is = ctx.getAssets().open(assetName)) {
			// APKs are zip archives, so they start with the local file header
			return is.read() == 'P' && is.read() == 'K';
		} catch (IOException e) {
			return false;
		}
	}

	private String getUnknownSourcesString(@Nullable String userAgent) {
		boolean is8OrHigher = false;
		if (userAgent != null) {
			Matcher matcher = REGEX_AGENT.matcher(userAgent);
			if (matcher.find()) {
				int androidMajorVersion = Integer.parseInt(requireNonNull(matcher.group(1)));
				is8OrHigher = androidMajorVersion >= 8;
			}
		}
		return is8OrHigher ?
				ctx.getString(R.string.website_troubleshooting_2_new) :
				ctx.getString(R.string.website_troubleshooting_2_old);
	}
}