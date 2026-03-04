package org.anonomi.android.settings;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.anonomi.R;
import org.anonomi.android.util.MapImporter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class OfflineMapDataFragment extends PreferenceFragmentCompat {

	private static final String TAG = "OfflineMapDataFragment";
	private static final int REQUEST_CODE_IMPORT = 1002;

	private PreferenceCategory importedMapsCategory;
	private ProgressDialog progressDialog;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.offline_map_data_preferences, rootKey);

		Preference importMaps = findPreference(getString(R.string.pref_key_import_offline_maps));
		if (importMaps != null) {
			importMaps.setOnPreferenceClickListener(pref -> {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.setType("application/zip");
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				startActivityForResult(intent, REQUEST_CODE_IMPORT);
				return true;
			});
		}

		importedMapsCategory = findPreference("pref_key_offline_maps_list");
		loadImportedMaps();
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.pref_title_offline_map_data);
	}

	@Override
	public void onResume() {
		super.onResume();
		loadImportedMaps();
	}

	private void loadImportedMaps() {
		if (importedMapsCategory == null) return;
		importedMapsCategory.removeAll();

		java.util.Map<String, ?> allPrefs =
				getPreferenceManager().getSharedPreferences().getAll();
		boolean hasMaps = false;

		for (java.util.Map.Entry<String, ?> entry : allPrefs.entrySet()) {
			if (entry.getKey().startsWith("offline_map_")) {
				hasMaps = true;
				String regionName = entry.getKey().replace("offline_map_", "");
				String jsonString = entry.getValue().toString();

				String summaryText = "";
				try {
					JSONObject meta = new JSONObject(jsonString);

					String zoomsText = "";
					if (meta.has("zooms")) {
						Object zoomsObj = meta.get("zooms");
						if (zoomsObj instanceof org.json.JSONArray) {
							org.json.JSONArray zoomsArray = meta.getJSONArray("zooms");
							if (zoomsArray.length() > 0) {
								int first = zoomsArray.getInt(0);
								int last = zoomsArray.getInt(zoomsArray.length() - 1);
								zoomsText = first + "–" + last;
							}
						} else {
							zoomsText = zoomsObj.toString();
						}
					}

					String sizeStatus = meta.optString("status",
							getString(R.string.status_imported));
					summaryText = getString(R.string.offline_map_summary, zoomsText, sizeStatus);

				} catch (Exception e) {
					summaryText = getString(R.string.failed_to_load_map_info);
					Log.e(TAG, "Failed to parse metadata for: " + regionName, e);
				}

				Preference pref = new Preference(requireContext());
				pref.setTitle(regionName);
				pref.setSummary(summaryText);
				pref.setIcon(mapIcon());
				final String finalRegionName = regionName;
				final String finalPrefKey = entry.getKey();
				pref.setOnPreferenceClickListener(p -> {
					showDeleteDialog(finalRegionName, finalPrefKey, pref);
					return true;
				});
				importedMapsCategory.addPreference(pref);
			}
		}

		if (!hasMaps) {
			Preference emptyPref = new Preference(requireContext());
			emptyPref.setTitle(getString(R.string.no_offline_maps));
			emptyPref.setEnabled(false);
			importedMapsCategory.addPreference(emptyPref);
		}
	}

	private void showDeleteDialog(String regionName, String prefKey, Preference pref) {
		new android.app.AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.delete_offline_map_title))
				.setMessage(getString(R.string.delete_offline_map_message, regionName))
				.setPositiveButton(getString(R.string.button_delete),
						(dialog, which) -> deleteOfflineMap(regionName, prefKey, pref))
				.setNegativeButton(getString(R.string.button_cancel), null)
				.show();
	}

	private void deleteOfflineMap(String regionName, String prefKey, Preference pref) {
		File mapBaseDir = new File(
				requireContext().getExternalFilesDir(null), "tiles/AnonMapsCache");

		String meta = getPreferenceManager().getSharedPreferences()
				.getString(prefKey, null);

		if (meta != null) {
			int start = meta.indexOf("[");
			int end = meta.indexOf("]");
			if (start != -1 && end != -1 && end > start) {
				String zoomsStr = meta.substring(start + 1, end);
				for (String zoom : zoomsStr.split(",")) {
					zoom = zoom.trim();
					if (!zoom.isEmpty()) {
						File zoomDir = new File(mapBaseDir, zoom);
						if (zoomDir.exists()) deleteRecursive(zoomDir);
					}
				}
			}
		}

		getPreferenceManager().getSharedPreferences().edit()
				.remove(prefKey).apply();
		importedMapsCategory.removePreference(pref);

		Toast.makeText(requireContext(),
				getString(R.string.offline_map_deleted, regionName),
				Toast.LENGTH_SHORT).show();
	}

	private Drawable mapIcon() {
		Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_offline_maps);
		if (icon != null) {
			icon = icon.mutate();
			icon.setTint(Color.WHITE);
		}
		return icon;
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_IMPORT
				&& resultCode == requireActivity().RESULT_OK
				&& data != null) {
			Uri zipUri = data.getData();
			if (zipUri != null) {
				progressDialog = new ProgressDialog(requireContext());
				progressDialog.setMessage(getString(R.string.importing_map_message));
				progressDialog.setCancelable(false);
				progressDialog.show();

				MapImporter.importOfflineMaps(getContext(), zipUri,
						(regionName, statusText, metadataJson) ->
								onImportComplete(regionName, statusText, metadataJson));
			}
		}
	}

	private void onImportComplete(String regionName, String statusText, String metadataJsonText) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		try {
			JSONObject meta = new JSONObject(metadataJsonText);
			meta.put("status", statusText);

			getPreferenceManager().getSharedPreferences().edit()
					.putString("offline_map_" + regionName, meta.toString())
					.apply();

			loadImportedMaps();
			Toast.makeText(requireContext(),
					getString(R.string.import_completed_for, regionName),
					Toast.LENGTH_SHORT).show();

		} catch (JSONException e) {
			Toast.makeText(requireContext(),
					getString(R.string.failed_to_parse_metadata, regionName),
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "Error parsing metadata JSON", e);
		}
	}
}
