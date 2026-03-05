package org.anonomi.android.map;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapServerClient {

	/**
	 * Matches a valid v3 onion URL: http(s)://[56 base32 chars].onion[/optional path]
	 * The 56-char pattern is the same used in TorPlugin for v3 onion address validation.
	 */
	private static final Pattern ONION_URL =
			Pattern.compile("https?://[a-z2-7]{56}\\.onion(/.*)?",
					Pattern.CASE_INSENSITIVE);

	/**
	 * Returns true if the given string is a valid v3 .onion URL.
	 */
	public static boolean isValidOnionUrl(String url) {
		if (url == null || url.isEmpty()) return false;
		return ONION_URL.matcher(url.trim()).matches();
	}


	/**
	 * Fetches {serverUrl}/disco.json and returns the list of available maps.
	 * Runs on caller's thread — call from an IO executor.
	 */
	public static List<OnlineMapEntry> fetchDisco(OkHttpClient client, String serverUrl)
			throws IOException {
		String base = serverUrl.replaceAll("/+$", "");
		String url = base + "/disco.json";
		String body = fetchString(client, url);
		List<OnlineMapEntry> result = new ArrayList<>();
		try {
			JSONArray arr = new JSONObject(body).getJSONArray("maps");
			for (int i = 0; i < arr.length(); i++) {
				OnlineMapEntry entry = parseMapJson(arr.getJSONObject(i), base);
				if (entry != null) result.add(entry);
			}
		} catch (Exception e) {
			throw new IOException("Failed to parse disco.json: " + e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Fetches {mapUrl}/map.json (or {mapUrl} if it already ends with map.json) and
	 * returns the single map entry.
	 * Runs on caller's thread — call from an IO executor.
	 */
	public static OnlineMapEntry fetchMap(OkHttpClient client, String mapUrl) throws IOException {
		String base = mapUrl.replaceAll("/+$", "");
		String url = base.endsWith("map.json") ? base : base + "/map.json";
		String body = fetchString(client, url);
		try {
			// For a direct map URL the base (without /map.json) is the tile base URL
			String tileBase = base.endsWith("map.json")
					? base.substring(0, base.length() - "map.json".length()).replaceAll("/+$", "")
					: base;
			OnlineMapEntry entry = parseMapJson(new JSONObject(body), tileBase);
			if (entry == null) throw new IOException("map.json missing required fields");
			return entry;
		} catch (Exception e) {
			throw new IOException("Failed to parse map.json: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns true if the URL is a server URL (no UUID path segment),
	 * false if it points directly to a specific map.
	 */
	public static boolean isServerUrl(String url) {
		// A UUID v4 path segment matches 8-4-4-4-12 hex chars
		return !url.matches(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*");
	}

	private static String fetchString(OkHttpClient client, String url) throws IOException {
		Request request = new Request.Builder().url(url).build();
		try (Response response = client.newCall(request).execute()) {
			ResponseBody body = response.body();
			if (!response.isSuccessful() || body == null) {
				throw new IOException("HTTP " + response.code() + " for " + url);
			}
			return body.string();
		}
	}

	/**
	 * @param serverBase  base URL of the server (no trailing slash), used to construct
	 *                    tileUrl when the JSON entry doesn't include one explicitly.
	 */
	private static OnlineMapEntry parseMapJson(JSONObject json, String serverBase) {
		try {
			String id = json.getString("id");
			String name = json.optString("name", id);
			String description = json.optString("description", "");
			// tileUrl may be explicit in the JSON, or constructed from serverBase + id
			String tileUrl = json.optString("tileUrl", null);
			if (tileUrl == null || tileUrl.isEmpty()) {
				tileUrl = serverBase + "/" + id + "/{z}/{x}/{y}.png";
			}
			int zoomMin = json.optInt("zoomMin", 0);
			int zoomMax = json.optInt("zoomMax", 18);
			double bboxNorth = 0, bboxSouth = 0, bboxEast = 0, bboxWest = 0;
			if (json.has("bbox")) {
				JSONObject bbox = json.getJSONObject("bbox");
				bboxNorth = bbox.optDouble("north", 0);
				bboxSouth = bbox.optDouble("south", 0);
				bboxEast = bbox.optDouble("east", 0);
				bboxWest = bbox.optDouble("west", 0);
			}
			return new OnlineMapEntry(id, name, description, tileUrl,
					zoomMin, zoomMax, bboxNorth, bboxSouth, bboxEast, bboxWest,
					true, true, true);
		} catch (Exception e) {
			return null;
		}
	}
}
