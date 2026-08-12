package org.anonomi.android.hotspot;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonomi.android.hotspot.HotspotState.WebsiteConfig;
import org.anonomi.android.qrcode.QrCodeUtils;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.NetworkUtils.getNetworkInterfaces;
import static org.anonomi.android.hotspot.WebServer.PORT;
import static org.anonomi.android.qrcode.QrCodeUtils.HOTSPOT_QRCODE_FACTOR;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
class WebServerManager {

	interface WebServerListener {
		@IoExecutor
		void onWebServerStarted(WebsiteConfig websiteConfig);

		@IoExecutor
		void onWebServerError();
	}

	private static final Logger LOG =
			getLogger(WebServerManager.class.getName());

	/**
	 * The address a Wi-Fi Direct group owner takes on Android. Used only when
	 * the hotspot interface cannot be identified, so that the server still
	 * binds to a single address rather than to every interface the device is
	 * attached to. If the hotspot is not actually reachable there, binding
	 * fails and the user is told, which beats quietly serving the whole LAN.
	 */
	private static final String DEFAULT_AP_ADDRESS = "192.168.49.1";

	private final Application ctx;
	private final DisplayMetrics dm;

	private volatile WebServerListener listener;
	@Nullable
	private volatile WebServer webServer;

	@Inject
	WebServerManager(Application ctx) {
		this.ctx = ctx;
		dm = ctx.getResources().getDisplayMetrics();
	}

	@UiThread
	void setListener(WebServerListener listener) {
		this.listener = listener;
	}

	@IoExecutor
	void startWebServer() {
		// The hotspot interface only exists once the hotspot is up, and
		// NanoHTTPD takes its bind address in the constructor, so the server
		// is built here rather than at injection time.
		stopWebServer();
		String host = getBindAddress();
		WebServer server = new WebServer(ctx, host);
		webServer = server;
		try {
			server.start();
			onWebServerStarted(host);
		} catch (IOException e) {
			logException(LOG, WARNING, e);
			listener.onWebServerError();
		}
	}

	/**
	 * The address of the hotspot interface, which the server both binds to and
	 * advertises. Deriving both from one lookup keeps the address we listen on
	 * and the address in the QR code from drifting apart.
	 */
	@IoExecutor
	private String getBindAddress() {
		InetAddress address = getAccessPointAddress();
		if (address == null) {
			//LOG.info(
			//		"Could not find access point address, assuming 192.168.49.1");
			return DEFAULT_AP_ADDRESS;
		}
		//if (LOG.isLoggable(INFO)) {
		//	LOG.info("Access point address " + address.getHostAddress());
		//}
		String host = address.getHostAddress();
		return host == null ? DEFAULT_AP_ADDRESS : host;
	}

	@IoExecutor
	private void onWebServerStarted(String host) {
		String url = "http://" + host + ":" + PORT;
		Bitmap qrCode = QrCodeUtils.createQrCode(
				(int) (dm.heightPixels * HOTSPOT_QRCODE_FACTOR), url);
		listener.onWebServerStarted(new WebsiteConfig(url, qrCode));
	}

	/**
	 * It is safe to call this more than once and it won't throw.
	 */
	@IoExecutor
	void stopWebServer() {
		WebServer server = webServer;
		if (server != null) server.stop();
		webServer = null;
	}

	@Nullable
	private static InetAddress getAccessPointAddress() {
		for (NetworkInterface i : getNetworkInterfaces()) {
			if (i.getName().startsWith("p2p")) {
				for (InterfaceAddress a : i.getInterfaceAddresses()) {
					// we consider only IPv4 addresses
					if (a.getAddress().getAddress().length == 4)
						return a.getAddress();
				}
			}
		}
		return null;
	}
}
