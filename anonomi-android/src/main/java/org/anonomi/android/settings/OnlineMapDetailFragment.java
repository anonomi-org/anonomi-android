package org.anonomi.android.settings;

import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.anonomi.R;
import org.anonomi.android.map.OnlineMapEntry;
import org.anonomi.android.map.OnlineMapStore;

import java.io.File;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class OnlineMapDetailFragment extends PreferenceFragmentCompat {

	public static final String ARG_MAP_ID = "map_id";

	/** Mutable snapshot of the entry; updated when toggles are changed. */
	private OnlineMapEntry currentEntry;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.online_map_detail_preferences, rootKey);

		String mapId = requireNonNull(requireArguments().getString(ARG_MAP_ID));
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

		currentEntry = findEntry(prefs, mapId);
		if (currentEntry == null) {
			getParentFragmentManager().popBackStack();
			return;
		}

		String defaultId = OnlineMapStore.getDefaultId(prefs);

		// ── Fetch online toggle ───────────────────────────────────────────
		SwitchPreferenceCompat fetchOnlineSwitch =
				requireNonNull(findPreference("pref_map_detail_fetch_online"));
		fetchOnlineSwitch.setChecked(currentEntry.fetchOnline);
		fetchOnlineSwitch.setOnPreferenceChangeListener((pref, val) -> {
			currentEntry = withFetchOnline(currentEntry, (boolean) val);
			OnlineMapStore.save(prefs, currentEntry);
			return true;
		});

		// ── Cache toggle ──────────────────────────────────────────────────
		SwitchPreferenceCompat cacheSwitch =
				requireNonNull(findPreference("pref_map_detail_cache"));
		cacheSwitch.setChecked(currentEntry.cacheEnabled);
		cacheSwitch.setOnPreferenceChangeListener((pref, val) -> {
			currentEntry = withCache(currentEntry, (boolean) val);
			OnlineMapStore.save(prefs, currentEntry);
			return true;
		});

		// ── Offline imports toggle ────────────────────────────────────────
		SwitchPreferenceCompat importsSwitch =
				requireNonNull(findPreference("pref_map_detail_offline_imports"));
		importsSwitch.setChecked(currentEntry.useOfflineImports);
		importsSwitch.setOnPreferenceChangeListener((pref, val) -> {
			currentEntry = withOfflineImports(currentEntry, (boolean) val);
			OnlineMapStore.save(prefs, currentEntry);
			return true;
		});

		// ── Cache size (populated asynchronously in onStart) ──────────────
		Preference cacheSizePref =
				requireNonNull(findPreference("pref_map_detail_cache_size"));
		cacheSizePref.setSummary(getString(R.string.pref_summary_map_no_cache));

		// ── Clear cache ───────────────────────────────────────────────────
		Preference clearCachePref =
				requireNonNull(findPreference("pref_map_detail_clear_cache"));
		clearCachePref.setOnPreferenceClickListener(pref -> {
			new AlertDialog.Builder(requireContext(), R.style.AnonDialogTheme)
					.setTitle(getString(R.string.pref_title_map_clear_cache))
					.setMessage(getString(R.string.tile_cache_clear_confirm))
					.setPositiveButton(R.string.button_delete, (d, which) -> {
						File mapCacheDir = mapCacheDir(currentEntry.id);
						if (mapCacheDir.exists()) deleteRecursive(mapCacheDir);
						cacheSizePref.setSummary(getString(R.string.pref_summary_map_no_cache));
						Toast.makeText(requireContext(),
								getString(R.string.map_cache_cleared), Toast.LENGTH_SHORT).show();
					})
					.setNegativeButton(R.string.button_cancel, null)
					.show();
			return true;
		});

		// ── Set / Remove as default ───────────────────────────────────────
		Preference setDefaultPref =
				requireNonNull(findPreference("pref_map_detail_set_default"));
		updateDefaultPref(setDefaultPref, currentEntry.id.equals(defaultId), prefs);

		// ── Remove map ────────────────────────────────────────────────────
		Preference removePref =
				requireNonNull(findPreference("pref_map_detail_remove"));
		removePref.setOnPreferenceClickListener(pref -> {
			showRemoveDialog(prefs);
			return true;
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		if (currentEntry != null) {
			requireActivity().setTitle(currentEntry.name);
			refreshCacheSize();
		}
	}

	// ── Private helpers ───────────────────────────────────────────────────

	private void refreshCacheSize() {
		Preference cacheSizePref = findPreference("pref_map_detail_cache_size");
		if (cacheSizePref == null || currentEntry == null) return;

		File mapCacheDir = mapCacheDir(currentEntry.id);
		new Thread(() -> {
			long bytes = folderSize(mapCacheDir);
			String label = bytes > 0
					? formatBytes(bytes)
					: getString(R.string.pref_summary_map_no_cache);
			if (isAdded()) {
				requireActivity().runOnUiThread(() -> cacheSizePref.setSummary(label));
			}
		}).start();
	}

	private File mapCacheDir(String mapId) {
		File fetchedDir = new File(
				requireContext().getExternalFilesDir(null), "tiles/fetched");
		return new File(fetchedDir, mapId);
	}

	private long folderSize(File dir) {
		if (dir == null || !dir.exists()) return 0;
		long size = 0;
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				size += f.isDirectory() ? folderSize(f) : f.length();
			}
		}
		return size;
	}

	private String formatBytes(long bytes) {
		if (bytes < 1024) return bytes + " B";
		if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
		if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
		return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
	}

	private void updateDefaultPref(Preference pref, boolean isDefault,
			SharedPreferences prefs) {
		if (isDefault) {
			pref.setTitle(getString(R.string.unset_default_map));
			pref.setOnPreferenceClickListener(p -> {
				OnlineMapStore.setDefaultId(prefs, null);
				updateDefaultPref(pref, false, prefs);
				Toast.makeText(requireContext(),
						getString(R.string.map_unset_as_default_done),
						Toast.LENGTH_LONG).show();
				return true;
			});
		} else {
			pref.setTitle(getString(R.string.set_as_default));
			pref.setOnPreferenceClickListener(p -> {
				OnlineMapStore.setDefaultId(prefs, currentEntry.id);
				updateDefaultPref(pref, true, prefs);
				Toast.makeText(requireContext(),
						getString(R.string.map_set_as_default_done),
						Toast.LENGTH_SHORT).show();
				return true;
			});
		}
	}

	private void showRemoveDialog(SharedPreferences prefs) {
		if (currentEntry == null) return;
		new AlertDialog.Builder(requireContext(), R.style.AnonDialogTheme)
				.setTitle(getString(R.string.map_remove_confirm_title))
				.setMessage(getString(R.string.map_remove_confirm_message, currentEntry.name))
				.setPositiveButton(R.string.button_delete, (d, which) -> {
					if (currentEntry.id.equals(OnlineMapStore.getDefaultId(prefs))) {
						OnlineMapStore.setDefaultId(prefs, null);
					}
					OnlineMapStore.delete(prefs, currentEntry.id);
					Toast.makeText(requireContext(),
							getString(R.string.map_removed), Toast.LENGTH_SHORT).show();
					getParentFragmentManager().popBackStack();
				})
				.setNegativeButton(R.string.button_cancel, null)
				.show();
	}

	private void deleteRecursive(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			File[] children = fileOrDir.listFiles();
			if (children != null) {
				for (File child : children) deleteRecursive(child);
			}
		}
		fileOrDir.delete();
	}

	private static OnlineMapEntry findEntry(SharedPreferences prefs, String mapId) {
		List<OnlineMapEntry> maps = OnlineMapStore.loadAll(prefs);
		for (OnlineMapEntry e : maps) {
			if (e.id.equals(mapId)) return e;
		}
		return null;
	}

	private static OnlineMapEntry withFetchOnline(OnlineMapEntry e, boolean fetchOnline) {
		return new OnlineMapEntry(e.id, e.name, e.description, e.tileUrl,
				e.zoomMin, e.zoomMax, e.bboxNorth, e.bboxSouth, e.bboxEast, e.bboxWest,
				e.cacheEnabled, e.useOfflineImports, fetchOnline);
	}

	private static OnlineMapEntry withCache(OnlineMapEntry e, boolean cacheEnabled) {
		return new OnlineMapEntry(e.id, e.name, e.description, e.tileUrl,
				e.zoomMin, e.zoomMax, e.bboxNorth, e.bboxSouth, e.bboxEast, e.bboxWest,
				cacheEnabled, e.useOfflineImports, e.fetchOnline);
	}

	private static OnlineMapEntry withOfflineImports(OnlineMapEntry e, boolean useOfflineImports) {
		return new OnlineMapEntry(e.id, e.name, e.description, e.tileUrl,
				e.zoomMin, e.zoomMax, e.bboxNorth, e.bboxSouth, e.bboxEast, e.bboxWest,
				e.cacheEnabled, useOfflineImports, e.fetchOnline);
	}
}
