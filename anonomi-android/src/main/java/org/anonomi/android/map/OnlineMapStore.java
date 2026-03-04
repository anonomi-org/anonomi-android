package org.anonomi.android.map;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnlineMapStore {

	private static final String KEY_PREFIX = "online_map_";
	private static final String PREF_DEFAULT_MAP_ID = "pref_default_online_map_id";

	public static List<OnlineMapEntry> loadAll(SharedPreferences prefs) {
		List<OnlineMapEntry> result = new ArrayList<>();
		Map<String, ?> all = prefs.getAll();
		for (Map.Entry<String, ?> entry : all.entrySet()) {
			if (entry.getKey().startsWith(KEY_PREFIX)) {
				try {
					OnlineMapEntry map = fromJson(entry.getValue().toString());
					if (map != null) result.add(map);
				} catch (Exception ignored) {
				}
			}
		}
		return result;
	}

	public static void save(SharedPreferences prefs, OnlineMapEntry entry) {
		try {
			JSONObject json = new JSONObject();
			json.put("id", entry.id);
			json.put("name", entry.name);
			json.put("description", entry.description);
			json.put("tileUrl", entry.tileUrl);
			json.put("zoomMin", entry.zoomMin);
			json.put("zoomMax", entry.zoomMax);
			json.put("bboxNorth", entry.bboxNorth);
			json.put("bboxSouth", entry.bboxSouth);
			json.put("bboxEast", entry.bboxEast);
			json.put("bboxWest", entry.bboxWest);
			json.put("cacheEnabled", entry.cacheEnabled);
			json.put("useOfflineImports", entry.useOfflineImports);
			json.put("fetchOnline", entry.fetchOnline);
			prefs.edit().putString(KEY_PREFIX + entry.id, json.toString()).apply();
		} catch (JSONException ignored) {
		}
	}

	public static boolean exists(SharedPreferences prefs, String mapId) {
		return prefs.contains(KEY_PREFIX + mapId);
	}

	public static void delete(SharedPreferences prefs, String mapId) {
		prefs.edit().remove(KEY_PREFIX + mapId).apply();
	}

	public static String getDefaultId(SharedPreferences prefs) {
		return prefs.getString(PREF_DEFAULT_MAP_ID, null);
	}

	public static void setDefaultId(SharedPreferences prefs, String mapId) {
		prefs.edit().putString(PREF_DEFAULT_MAP_ID, mapId).apply();
	}

	static OnlineMapEntry fromJson(String jsonString) {
		try {
			JSONObject json = new JSONObject(jsonString);
			return new OnlineMapEntry(
					json.getString("id"),
					json.optString("name", ""),
					json.optString("description", ""),
					json.getString("tileUrl"),
					json.optInt("zoomMin", 0),
					json.optInt("zoomMax", 18),
					json.optDouble("bboxNorth", 0),
					json.optDouble("bboxSouth", 0),
					json.optDouble("bboxEast", 0),
					json.optDouble("bboxWest", 0),
					json.optBoolean("cacheEnabled", true),
					json.optBoolean("useOfflineImports", true),
					json.optBoolean("fetchOnline", true)
			);
		} catch (JSONException e) {
			return null;
		}
	}
}
