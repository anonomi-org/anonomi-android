package org.anonomi.android.map;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.anonchatsecure.bramble.api.WeakSingletonProvider;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

public class MapTileConfig {

	public static final String PREF_ONLINE_ENABLED = "pref_map_online_enabled";

	public static MapTileProviderArray buildProvider(
			XYTileSource tileSource,
			File importsDir,
			File fetchedDir,
			SharedPreferences prefs,
			WeakSingletonProvider<OkHttpClient> httpClientProvider,
			Context context) {

		List<MapTileModuleProviderBase> providers = new ArrayList<>();

		// Resolve online maps up front — needed to read per-map settings.
		OnlineMapEntry defaultMap = null;
		List<OnlineMapEntry> otherMaps = new ArrayList<>();
		boolean onlineEnabled = prefs.getBoolean(PREF_ONLINE_ENABLED, true);

		if (onlineEnabled) {
			List<OnlineMapEntry> maps = OnlineMapStore.loadAll(prefs);
			String defaultId = OnlineMapStore.getDefaultId(prefs);
			for (OnlineMapEntry entry : maps) {
				if (entry.id.equals(defaultId)) {
					defaultMap = entry;
				} else {
					otherMaps.add(entry);
				}
			}
		}

		// 1. Imported offline tiles — controlled by the default map's useOfflineImports
		//    flag. Falls back to true when no online map is configured yet.
		boolean useOfflineImports = defaultMap == null || defaultMap.useOfflineImports;
		if (useOfflineImports) {
			LooseFilesTileProvider importsProvider = new LooseFilesTileProvider(importsDir);
			importsProvider.setTileSource(tileSource);
			providers.add(importsProvider);
		}

		// 2. Online tile providers — only when the global online switch is on AND a default
		//    map is explicitly chosen. Per-map fetchOnline flag gates individual providers;
		//    the global switch acts as a hard master override (off = no online fetching).
		if (onlineEnabled && defaultMap != null) {
			OkHttpClient httpClient = null;
			if (defaultMap.fetchOnline) {
				httpClient = httpClientProvider.get();
				addOnlineProvider(providers, tileSource, httpClient, fetchedDir, defaultMap);
			}
			for (OnlineMapEntry entry : otherMaps) {
				if (entry.fetchOnline) {
					if (httpClient == null) httpClient = httpClientProvider.get();
					addOnlineProvider(providers, tileSource, httpClient, fetchedDir, entry);
				}
			}
		}

		return new MapTileProviderArray(
				tileSource,
				null,
				providers.toArray(new MapTileModuleProviderBase[0])
		);
	}

	private static void addOnlineProvider(List<MapTileModuleProviderBase> providers,
			XYTileSource tileSource, OkHttpClient httpClient,
			File fetchedDir, OnlineMapEntry entry) {
		File mapCacheDir = new File(fetchedDir, entry.id);
		OnlineTileProvider provider = new OnlineTileProvider(
				httpClient, entry.tileUrl, mapCacheDir, entry.cacheEnabled);
		provider.setTileSource(tileSource);
		providers.add(provider);
	}
}
