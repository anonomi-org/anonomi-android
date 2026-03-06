package org.anonomi.android.settings;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import org.anonomi.R;
import org.anonomi.android.map.MapServerClient;
import org.anonomi.android.map.OnlineMapEntry;
import org.anonomi.android.map.OnlineMapStore;
import org.anonomi.android.qrcode.ScanMapUrlActivity;
import org.anonchatsecure.bramble.api.WeakSingletonProvider;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import okhttp3.OkHttpClient;

import static org.anonomi.android.AppModule.getAndroidComponent;

public class OfflineMapsFragment extends PreferenceFragmentCompat {

	private static final String TAG = "OfflineMapsFragment";
	private static final int REQUEST_CODE_SCAN_MAP_QR = 1003;

	@Inject
	WeakSingletonProvider<OkHttpClient> httpClientProvider;
	@Inject
	@IoExecutor
	Executor ioExecutor;

	private PreferenceCategory onlineMapsCategory;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		getAndroidComponent(context).inject(this);
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.offline_maps_preferences, rootKey);

		setupClearCachePreference();
		setupAddMapPreference();
		setupServerUrlPreference();

		onlineMapsCategory = findPreference("pref_key_online_maps_list");
		if (onlineMapsCategory == null) {
			onlineMapsCategory = new PreferenceCategory(requireContext());
			onlineMapsCategory.setTitle(getString(R.string.pref_category_online_maps_list));
			getPreferenceScreen().addPreference(onlineMapsCategory);
		}

		loadOnlineMaps();
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.pref_key_offline_maps_title);
	}

	@Override
	public void onResume() {
		super.onResume();
		loadOnlineMaps();
	}

	// ── Preference setup ──────────────────────────────────────────────────

	private void setupClearCachePreference() {
		Preference clearCache = findPreference("pref_clear_tile_cache");
		if (clearCache != null) {
			clearCache.setOnPreferenceClickListener(pref -> {
				new AlertDialog.Builder(requireContext(), R.style.AnonDialogTheme)
						.setTitle(getString(R.string.pref_title_clear_tile_cache))
						.setMessage(getString(R.string.tile_cache_clear_confirm))
						.setPositiveButton(R.string.button_delete, (d, which) -> {
							File fetchedDir = new File(
									requireContext().getExternalFilesDir(null), "tiles/fetched");
							if (fetchedDir.exists()) deleteRecursive(fetchedDir);
							Toast.makeText(requireContext(),
									getString(R.string.tile_cache_cleared), Toast.LENGTH_SHORT).show();
						})
						.setNegativeButton(R.string.button_cancel, null)
						.show();
				return true;
			});
		}
	}

	private void setupAddMapPreference() {
		Preference addMap = findPreference("pref_key_add_online_map");
		if (addMap != null) {
			addMap.setOnPreferenceClickListener(pref -> {
				showAddMapDialog(null);
				return true;
			});
		}
	}

	private void setupServerUrlPreference() {
		Preference serverUrlPref = findPreference("pref_map_default_server_url");
		if (serverUrlPref == null) return;
		String defaultUrl = getString(R.string.default_map_server_url);
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		serverUrlPref.setSummary(prefs.getString("pref_map_default_server_url", defaultUrl));
		serverUrlPref.setOnPreferenceClickListener(pref -> {
			showServerUrlDialog(serverUrlPref);
			return true;
		});
	}

	private void showServerUrlDialog(Preference serverUrlPref) {
		Context context = requireContext();
		int padPx = (int) (16 * getResources().getDisplayMetrics().density);
		String defaultUrl = getString(R.string.default_map_server_url);
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		String currentUrl = prefs.getString("pref_map_default_server_url", defaultUrl);

		EditText urlInput = new EditText(context);
		urlInput.setText(currentUrl);
		urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
				| android.text.InputType.TYPE_TEXT_VARIATION_URI);
		urlInput.setPadding(padPx, padPx / 2, padPx, padPx / 2);

		AlertDialog dialog = new AlertDialog.Builder(context, R.style.AnonDialogTheme)
				.setTitle(getString(R.string.pref_title_default_map_server))
				.setView(urlInput)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, null)
				.setNeutralButton(R.string.reset_to_default, null)
				.create();

		dialog.setOnShowListener(d -> {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
				String newUrl = urlInput.getText().toString().trim();
				if (!MapServerClient.isValidOnionUrl(newUrl)) {
					urlInput.setError(getString(R.string.error_invalid_onion_url));
					return;
				}
				prefs.edit().putString("pref_map_default_server_url", newUrl).apply();
				serverUrlPref.setSummary(newUrl);
				dialog.dismiss();
			});
			dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
				prefs.edit().putString("pref_map_default_server_url", defaultUrl).apply();
				serverUrlPref.setSummary(defaultUrl);
				dialog.dismiss();
			});
		});

		dialog.show();
	}

	// ── Online maps list ──────────────────────────────────────────────────

	private void loadOnlineMaps() {
		if (onlineMapsCategory == null) return;
		onlineMapsCategory.removeAll();

		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		List<OnlineMapEntry> maps = OnlineMapStore.loadAll(prefs);
		String defaultId = OnlineMapStore.getDefaultId(prefs);

		if (maps.isEmpty()) {
			Preference empty = new Preference(requireContext());
			empty.setTitle(getString(R.string.no_online_maps));
			empty.setEnabled(false);
			onlineMapsCategory.addPreference(empty);
			return;
		}

		for (OnlineMapEntry entry : maps) {
			Preference pref = new Preference(requireContext());
			String title = entry.id.equals(defaultId)
					? entry.name + " \u2605"
					: entry.name;
			pref.setTitle(title);
			pref.setSummary(getString(R.string.online_map_summary, entry.zoomMin, entry.zoomMax));
			pref.setIcon(mapIcon());
			pref.setOnPreferenceClickListener(p -> {
				navigateToMapDetail(entry.id);
				return true;
			});
			onlineMapsCategory.addPreference(pref);
		}
	}

	private void navigateToMapDetail(String mapId) {
		OnlineMapDetailFragment detailFragment = new OnlineMapDetailFragment();
		Bundle args = new Bundle();
		args.putString(OnlineMapDetailFragment.ARG_MAP_ID, mapId);
		detailFragment.setArguments(args);
		getParentFragmentManager().beginTransaction()
				.setCustomAnimations(
						R.anim.step_next_in, R.anim.step_previous_out,
						R.anim.step_previous_in, R.anim.step_next_out)
				.replace(R.id.fragmentContainer, detailFragment)
				.addToBackStack(null)
				.commit();
	}

	// ── Add map dialog ────────────────────────────────────────────────────

	private void showAddMapDialog(String prefillUrl) {
		Context context = requireContext();
		int padPx = (int) (16 * getResources().getDisplayMetrics().density);

		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		String defaultServerUrl = prefs.getString("pref_map_default_server_url",
				getString(R.string.default_map_server_url)).trim();
		boolean hasDefaultServer = !defaultServerUrl.isEmpty();

		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(padPx, padPx / 2, padPx, 0);

		// ── Radio buttons ─────────────────────────────────────────────────
		RadioGroup radioGroup = new RadioGroup(context);
		radioGroup.setOrientation(RadioGroup.VERTICAL);

		RadioButton radioDefault = new RadioButton(context);
		radioDefault.setId(View.generateViewId());
		radioDefault.setText(hasDefaultServer
				? getString(R.string.dialog_radio_default_server)
				: getString(R.string.dialog_radio_default_server) + "\n("
						+ getString(R.string.dialog_no_default_server) + ")");
		radioDefault.setEnabled(hasDefaultServer);
		radioGroup.addView(radioDefault);

		RadioButton radioCustom = new RadioButton(context);
		radioCustom.setId(View.generateViewId());
		radioCustom.setText(getString(R.string.dialog_radio_custom_url));
		radioGroup.addView(radioCustom);

		layout.addView(radioGroup);

		// ── URL input ─────────────────────────────────────────────────────
		EditText urlInput = new EditText(context);
		urlInput.setHint(getString(R.string.add_map_url_hint));
		urlInput.setPadding(0, padPx, 0, padPx);
		if (prefillUrl != null) urlInput.setText(prefillUrl);
		layout.addView(urlInput);

		// ── Scan QR button ────────────────────────────────────────────────
		Button scanButton = new Button(context);
		scanButton.setText(getString(R.string.scan_qr_button));
		scanButton.setAllCaps(false);
		scanButton.setBackgroundColor(Color.TRANSPARENT);
		scanButton.setTextColor(ContextCompat.getColor(context, R.color.briar_button_text_neutral));
		layout.addView(scanButton, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		// ── Initial selection ─────────────────────────────────────────────
		// Default server pre-selected when available and no QR/prefill URL incoming
		boolean startOnDefault = hasDefaultServer && prefillUrl == null;
		radioGroup.check(startOnDefault ? radioDefault.getId() : radioCustom.getId());
		urlInput.setVisibility(startOnDefault ? View.GONE : View.VISIBLE);
		scanButton.setVisibility(startOnDefault ? View.GONE : View.VISIBLE);

		radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
			boolean isCustom = checkedId == radioCustom.getId();
			urlInput.setVisibility(isCustom ? View.VISIBLE : View.GONE);
			scanButton.setVisibility(isCustom ? View.VISIBLE : View.GONE);
		});

		// ── Build dialog ──────────────────────────────────────────────────
		AlertDialog dialog = new AlertDialog.Builder(context, R.style.AnonDialogTheme)
				.setTitle(getString(R.string.add_map_dialog_title))
				.setView(layout)
				.setPositiveButton(android.R.string.ok, null)
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		dialog.setOnShowListener(d ->
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
					String url = radioGroup.getCheckedRadioButtonId() == radioDefault.getId()
							? defaultServerUrl
							: urlInput.getText().toString().trim();
					if (!MapServerClient.isValidOnionUrl(url)) {
						urlInput.setError(getString(R.string.error_invalid_onion_url));
						return;
					}
					dialog.dismiss();
					handleMapUrl(url);
				}));

		scanButton.setOnClickListener(v -> {
			dialog.dismiss();
			Intent intent = new Intent(context, ScanMapUrlActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SCAN_MAP_QR);
		});

		dialog.show();
	}

	private AlertDialog buildFetchingDialog(Context context) {
		int padPx = (int) (16 * getResources().getDisplayMetrics().density);
		LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER_VERTICAL);
		layout.setPadding(padPx, padPx, padPx, padPx);

		ProgressBar spinner = new ProgressBar(context);
		layout.addView(spinner, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView message = new TextView(context);
		message.setText(getString(R.string.fetching_map_info));
		message.setPadding(padPx, 0, 0, 0);
		layout.addView(message, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		return new AlertDialog.Builder(context, R.style.AnonDialogTheme)
				.setView(layout)
				.setCancelable(false)
				.create();
	}

	private void handleMapUrl(String url) {
		AlertDialog progress = buildFetchingDialog(requireContext());
		progress.show();

		ioExecutor.execute(() -> {
			try {
				OkHttpClient client = httpClientProvider.get();
				if (MapServerClient.isServerUrl(url)) {
					List<OnlineMapEntry> maps = MapServerClient.fetchDisco(client, url);
					mainHandler.post(() -> {
						dismissSafely(progress);
						if (isAdded()) showSelectMapsDialog(maps);
					});
				} else {
					OnlineMapEntry entry = MapServerClient.fetchMap(client, url);
					mainHandler.post(() -> {
						dismissSafely(progress);
						if (isAdded()) showConfirmAddMapDialog(entry);
					});
				}
			} catch (Exception e) {
				Log.e(TAG, "Error fetching map info", e);
				boolean isServer = MapServerClient.isServerUrl(url);
				mainHandler.post(() -> {
					dismissSafely(progress);
					if (!isAdded()) return;
					String msg = isServer
							? getString(R.string.error_fetching_disco)
							: getString(R.string.error_fetching_map);
					Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
				});
			}
		});
	}

	private static void dismissSafely(AlertDialog dialog) {
		try {
			if (dialog != null && dialog.isShowing()) dialog.dismiss();
		} catch (Exception ignored) {
		}
	}

	private void showSelectMapsDialog(List<OnlineMapEntry> maps) {
		if (maps.isEmpty()) {
			Toast.makeText(requireContext(), getString(R.string.no_online_maps),
					Toast.LENGTH_SHORT).show();
			return;
		}
		String[] names = new String[maps.size()];
		boolean[] checked = new boolean[maps.size()];
		for (int i = 0; i < maps.size(); i++) {
			names[i] = maps.get(i).name;
			checked[i] = true;
		}
		new AlertDialog.Builder(requireContext(), R.style.AnonDialogTheme)
				.setTitle(getString(R.string.select_maps_to_add))
				.setMultiChoiceItems(names, checked,
						(d, which, isChecked) -> checked[which] = isChecked)
				.setPositiveButton(android.R.string.ok, (d, which) -> {
					SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
					int added = 0, skipped = 0;
					for (int i = 0; i < maps.size(); i++) {
						if (checked[i]) {
							if (OnlineMapStore.exists(prefs, maps.get(i).id)) {
								skipped++;
							} else {
								OnlineMapStore.save(prefs, maps.get(i));
								added++;
							}
						}
					}
					if (added > 0) loadOnlineMaps();
					if (added > 0 && skipped == 0) {
						Toast.makeText(requireContext(),
								getString(R.string.online_map_added, added + " map(s)"),
								Toast.LENGTH_SHORT).show();
					} else if (added > 0) {
						Toast.makeText(requireContext(),
								getString(R.string.online_maps_added_some_skipped, added, skipped),
								Toast.LENGTH_LONG).show();
					} else if (skipped > 0) {
						Toast.makeText(requireContext(),
								getString(R.string.online_map_already_added,
										skipped == 1 ? maps.get(0).name : skipped + " maps"),
								Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showConfirmAddMapDialog(OnlineMapEntry entry) {
		SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
		if (OnlineMapStore.exists(prefs, entry.id)) {
			Toast.makeText(requireContext(),
					getString(R.string.online_map_already_added, entry.name),
					Toast.LENGTH_SHORT).show();
			return;
		}
		new AlertDialog.Builder(requireContext(), R.style.AnonDialogTheme)
				.setTitle(getString(R.string.add_map_dialog_title))
				.setMessage(entry.name + "\n" +
						getString(R.string.online_map_summary, entry.zoomMin, entry.zoomMax))
				.setPositiveButton(android.R.string.ok, (d, which) -> {
					OnlineMapStore.save(prefs, entry);
					loadOnlineMaps();
					Toast.makeText(requireContext(),
							getString(R.string.online_map_added, entry.name),
							Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	// ── QR scan result ────────────────────────────────────────────────────

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_SCAN_MAP_QR
				&& resultCode == requireActivity().RESULT_OK
				&& data != null) {
			String url = data.getStringExtra(ScanMapUrlActivity.EXTRA_URL);
			if (url != null && !url.isEmpty()) {
				handleMapUrl(url);
			}
		}
	}

	// ── Utilities ─────────────────────────────────────────────────────────

	private void deleteRecursive(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			File[] children = fileOrDir.listFiles();
			if (children != null) {
				for (File child : children) deleteRecursive(child);
			}
		}
		fileOrDir.delete();
	}

	private Drawable mapIcon() {
		Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_offline_maps);
		if (icon != null) {
			icon = icon.mutate();
			icon.setTint(Color.WHITE);
		}
		return icon;
	}
}
