package org.anonomi.android.hotspot;

import android.content.Context;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import fi.iki.elonen.NanoHTTPD;

import static android.util.Xml.Encoding.UTF_8;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
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
	 * Order matters: first match wins.
	 * Key: substring in URI, Value: asset filename
	 */
	private static final Map<String, String> APK_ASSETS = new LinkedHashMap<>();
	static {
		APK_ASSETS.put("postbox", "anonomi-postbox.apk");
		APK_ASSETS.put("monerujo", "monerujo.apk");
		APK_ASSETS.put("orbot", "orbot.apk");
		APK_ASSETS.put("tor-browser", "tor-browser.apk");
	}

	private final Context ctx;

	WebServer(Context ctx) {
		super(PORT);
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

		if (uri.endsWith(".apk")) {
			return serveApkForUri(uri);
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

	private Response serveApkForUri(String uri) {
		// Serve known asset apks first
		for (Map.Entry<String, String> e : APK_ASSETS.entrySet()) {
			if (uri.contains(e.getKey())) {
				return serveAssetFile(e.getValue(), MIME_APK);
			}
		}
		// Fallback: serve installed app APK
		return serveInstalledApk();
	}

	private Response serveInstalledApk() {
		File file = new File(ctx.getPackageCodePath());
		long fileLen = file.length();

		try {
			FileInputStream fis = new FileInputStream(file);
			Response res = newFixedLengthResponse(OK, MIME_APK, fis, fileLen);
			res.addHeader("Content-Length", String.valueOf(fileLen));
			return res;
		} catch (FileNotFoundException e) {
			logException(LOG, WARNING, e);
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT,
					ctx.getString(R.string.hotspot_error_web_server_serve));
		}
	}

	private Response serveAssetFile(String assetName, String mime) {
		try {
			InputStream is = ctx.getAssets().open(assetName);

			// NOTE: available() is not a guaranteed "total length" for all streams,
			// but for AssetInputStream it typically corresponds to remaining bytes.
			int size = is.available();

			Response res = newFixedLengthResponse(OK, mime, is, size);
			res.addHeader("Content-Length", String.valueOf(size));
			return res;
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			return newFixedLengthResponse(NOT_FOUND, MIME_PLAINTEXT,
					ctx.getString(R.string.hotspot_error_web_server_serve));
		}
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
		requireNonNull(doc.selectFirst(".button")).attr("href", filename);
		requireNonNull(doc.selectFirst("#download_button"))
				.text(ctx.getString(R.string.website_download_button));

		// Optional buttons (only if present in HTML)
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

	private void setButton(Document doc, String selector, String href, int textRes) {
		Element btn = doc.selectFirst(selector);
		if (btn == null) return;
		btn.attr("href", href);
		btn.text(ctx.getString(textRes));
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