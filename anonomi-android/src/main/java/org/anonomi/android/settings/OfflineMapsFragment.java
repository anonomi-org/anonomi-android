package org.anonomi.android.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import android.app.ProgressDialog;

import org.anonomi.R;
import org.anonomi.android.util.MapImporter;
import java.io.File;
import android.util.Log;

import android.content.Context;

import org.json.JSONObject;  // ✅ ADD THIS LINE
import org.json.JSONException;  // ✅ ADD THIS to handle exceptions



import java.util.Map;

public class OfflineMapsFragment extends PreferenceFragmentCompat {

	private static final int REQUEST_CODE_IMPORT = 1002;
	private PreferenceCategory importedMapsCategory;
	private ProgressDialog progressDialog;


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.offline_maps_preferences, rootKey);

		// Hook the Import button
		Preference importMaps = findPreference("pref_key_import_offline_maps");
		if (importMaps != null) {
			importMaps.setOnPreferenceClickListener(preference -> {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.setType("application/zip");
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				startActivityForResult(intent, REQUEST_CODE_IMPORT);
				return true;
			});
		}

		// Find the category where we'll add the imported regions
		importedMapsCategory = findPreference("pref_key_offline_maps_list");

		if (importedMapsCategory == null) {
			// If it's not in XML, create it dynamically
			importedMapsCategory = new PreferenceCategory(requireContext());
			importedMapsCategory.setTitle("Offline Maps Data");
			getPreferenceScreen().addPreference(importedMapsCategory);
		}

		// Load existing imported maps (from prefs or dummy for now)
		loadImportedMaps();
	}

	private void deleteRecursive(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			File[] children = fileOrDir.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursive(child);
				}
			}
		}
		fileOrDir.delete();
	}

	private void loadImportedMaps() {
		importedMapsCategory.removeAll();  // Clear existing list

		// Load from SharedPreferences
		Map<String, ?> allPrefs = getPreferenceManager().getSharedPreferences().getAll();
		boolean hasMaps = false;

		for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
			if (entry.getKey().startsWith("offline_map_")) {
				hasMaps = true;
				String regionName = entry.getKey().replace("offline_map_", "");
				String jsonString = entry.getValue().toString();

				String summaryText = "";
				try {
					JSONObject meta = new JSONObject(jsonString);

					// Extract zooms array nicely
					String zoomsText = "";
					if (meta.has("zooms")) {
						// Could be a JSONArray or String, handle both
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

					String sizeStatus = meta.optString("status", getString(R.string.status_imported));

					summaryText = getString(R.string.offline_map_summary, zoomsText, sizeStatus);

				} catch (Exception e) {
					summaryText = getString(R.string.failed_to_load_map_info);
					Log.e("OfflineMapsFragment", "Failed to parse metadata JSON for: " + regionName, e);
				}

				Preference pref = new Preference(requireContext());
				pref.setTitle(regionName);
				pref.setSummary(summaryText);  // ✅ cleaner summary

				pref.setOnPreferenceClickListener(p -> {
					showDeleteDialog(regionName, entry.getKey(), pref);
					return true;
				});

				importedMapsCategory.addPreference(pref);
			}
		}

		if (!hasMaps) {
			// Show a placeholder if no maps
			Preference emptyPref = new Preference(requireContext());
			emptyPref.setTitle(getString(R.string.no_offline_maps));
			emptyPref.setEnabled(false);
			importedMapsCategory.addPreference(emptyPref);
		}
	}
	private void deleteOfflineMap(String regionName, String prefKey, Preference pref) {
		// 1️⃣ Delete tiles (based on metadata in SharedPreferences)
		File mapBaseDir = new File(requireContext().getExternalFilesDir(null), "tiles/AnonMapsCache");

		// Load metadata from SharedPreferences (we assume it contains Zooms: [..])
		String meta = getPreferenceManager().getSharedPreferences()
				.getString(prefKey, null);

		if (meta != null) {
			int start = meta.indexOf("[");
			int end = meta.indexOf("]");
			if (start != -1 && end != -1 && end > start) {
				String zoomsStr = meta.substring(start + 1, end);
				String[] zooms = zoomsStr.split(",");
				for (String zoom : zooms) {
					zoom = zoom.trim();
					if (!zoom.isEmpty()) {
						File zoomDir = new File(mapBaseDir, zoom);
						if (zoomDir.exists() && zoomDir.isDirectory()) {
							deleteRecursive(zoomDir);
							Log.d("OfflineMapsFragment", "Deleted zoom folder: " + zoomDir.getAbsolutePath());
						}
					}
				}
			} else {
				Log.w("OfflineMapsFragment", "No zoom info found in metadata for: " + regionName);
			}
		} else {
			Log.w("OfflineMapsFragment", "No metadata found for: " + regionName);
		}

		// 2️⃣ Remove from SharedPreferences
		getPreferenceManager().getSharedPreferences()
				.edit()
				.remove(prefKey)
				.apply();

		// 3️⃣ Remove from UI
		importedMapsCategory.removePreference(pref);

		// ✅ Optionally show feedback
		android.widget.Toast.makeText(requireContext(),
				getString(R.string.offline_map_deleted, regionName),
				android.widget.Toast.LENGTH_SHORT).show();
	}

	private void showDeleteDialog(String regionName, String prefKey, Preference pref) {
		new androidx.appcompat.app.AlertDialog.Builder(requireContext())
				.setTitle(getString(R.string.delete_offline_map_title))
				.setMessage(getString(R.string.delete_offline_map_message, regionName))
				.setPositiveButton(getString(R.string.button_delete), (dialog, which) -> {
					deleteOfflineMap(regionName, prefKey, pref);
				})
				.setNegativeButton(getString(R.string.button_cancel), null)
				.show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_IMPORT && resultCode == getActivity().RESULT_OK) {
			Uri zipUri = data.getData();
			if (zipUri != null) {
				// Show progress dialog
				progressDialog = new ProgressDialog(requireContext());
				progressDialog.setMessage(getString(R.string.importing_map_message));
				progressDialog.setCancelable(false);
				progressDialog.show();

				// Start the import process
				MapImporter.importOfflineMaps(getContext(), zipUri,
						(regionName, statusText, metadataJson) -> {
							onImportComplete(regionName, statusText, metadataJson);
						});
			}
		}
	}

	// Callback after import is done
	private void onImportComplete(String regionName, String statusText, String metadataJsonText) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		try {
			JSONObject meta = new JSONObject(metadataJsonText);
			meta.put("status", statusText);  // Add status field into the JSON

			getPreferenceManager().getSharedPreferences().edit()
					.putString("offline_map_" + regionName, meta.toString())
					.apply();

			// Refresh the list
			loadImportedMaps();

			Toast.makeText(requireContext(), getString(R.string.import_completed_for, regionName), Toast.LENGTH_SHORT).show();

		} catch (JSONException e) {
			Toast.makeText(requireContext(), getString(R.string.failed_to_parse_metadata, regionName), Toast.LENGTH_LONG).show();
			Log.e("OfflineMapsFragment", "Error parsing metadata JSON", e);
		}
	}

}