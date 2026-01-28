package org.anonomi.android.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import org.anonomi.R;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import java.io.File;

public class MapLocationPickerActivity extends BriarActivity {

	public static final String RESULT_MAP_MESSAGE = "map_message";

	private MapView map;
	private Button sendButton;
	private EditText labelInput;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(getString(R.string.send_offline_location_title));
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		File basePath = new File(getExternalFilesDir(null), "tiles");
		File tileCache = new File(basePath, "AnonMapsCache");

		Configuration.getInstance().setOsmdroidBasePath(basePath);
		Configuration.getInstance().setOsmdroidTileCache(tileCache);
		Configuration.getInstance().setUserAgentValue(getPackageName());

		setContentView(R.layout.activity_map_picker);

		ConstraintLayout rootLayout = findViewById(R.id.root_layout);

		TextView mapInfo = findViewById(R.id.map_info);

		map = new MapView(this);
		map.setId(View.generateViewId());
		map.setLayoutParams(new ConstraintLayout.LayoutParams(
				ConstraintLayout.LayoutParams.MATCH_PARENT,
				0
		));
		rootLayout.addView(map, 0);

		map.setOnTouchListener((v, event) -> {
			hideKeyboard();
			return false;
		});

		labelInput = findViewById(R.id.label_input);
		labelInput.setHint(getString(R.string.label_input_hint));

		ConstraintSet set = new ConstraintSet();
		set.clone(rootLayout);
		set.connect(map.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
		set.connect(map.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
		set.connect(map.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
		set.connect(map.getId(), ConstraintSet.BOTTOM, R.id.input_row, ConstraintSet.TOP);
		set.constrainHeight(labelInput.getId(), ConstraintLayout.LayoutParams.WRAP_CONTENT);
		set.connect(labelInput.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16);
		set.connect(labelInput.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16);
		set.connect(labelInput.getId(), ConstraintSet.BOTTOM, R.id.send_button, ConstraintSet.TOP, 16);
		set.applyTo(rootLayout);

		XYTileSource tileSource = new XYTileSource(
				"LooseFiles", 0, 18, 256, ".png", new String[]{}
		);

		LooseFilesTileProvider looseFilesModule = new LooseFilesTileProvider(tileCache);
		looseFilesModule.setTileSource(tileSource);

		MapTileProviderArray provider = new MapTileProviderArray(
				tileSource,
				null,
				new MapTileModuleProviderBase[]{looseFilesModule}
		);

		map.setTileProvider(provider);
		map.setTileSource(tileSource);
		map.setMultiTouchControls(true);
		map.setUseDataConnection(false);

		GeoPoint defaultPoint = new GeoPoint(0, 0);
		map.getController().setZoom(3);
		map.getController().setCenter(defaultPoint);

		// ✅ Add marker overlay
		org.osmdroid.views.overlay.Marker centerMarker = new org.osmdroid.views.overlay.Marker(map);
		centerMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
		Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.map_marker);
		if (vectorDrawable != null) {
			int sizeInPx = (int) getResources().getDimension(R.dimen.marker_size);  // Define this in dimens.xml, e.g., 36dp
			Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			vectorDrawable.draw(canvas);
			centerMarker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), bitmap));
		}
		centerMarker.setPosition(defaultPoint);
		map.getOverlays().add(centerMarker);

		// ✅ Proper MapListener without lambda
		map.addMapListener(new org.osmdroid.events.MapListener() {
			@Override
			public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
				GeoPoint newCenter = (GeoPoint) map.getMapCenter();
				centerMarker.setPosition(newCenter);

				double lat = newCenter.getLatitude();
				double lon = newCenter.getLongitude();
				double zoom = map.getZoomLevelDouble();
				String infoText = String.format(java.util.Locale.US, "Lat: %.4f, Lon: %.4f, Zoom: %.2f", lat, lon, zoom);
				mapInfo.setText(infoText);

				map.invalidate();
				return true;
			}

			@Override
			public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
				GeoPoint newCenter = (GeoPoint) map.getMapCenter();
				centerMarker.setPosition(newCenter);

				double lat = newCenter.getLatitude();
				double lon = newCenter.getLongitude();
				double zoom = map.getZoomLevelDouble();
				String infoText = String.format(java.util.Locale.US, "Lat: %.4f, Lon: %.4f, Zoom: %.2f", lat, lon, zoom);
				mapInfo.setText(infoText);

				map.invalidate();
				return true;
			}
		});

		sendButton = findViewById(R.id.send_button);
		sendButton.setOnClickListener(v -> {
			GeoPoint point = (GeoPoint) map.getMapCenter();
			double lat = point.getLatitude();
			double lon = point.getLongitude();
			double zoom = map.getZoomLevelDouble();

			String formattedZoom = String.format(java.util.Locale.US, "%.2f", zoom);

			String label = labelInput.getText().toString().trim();
			if (label.isEmpty()) label = getString(R.string.dropped_pin);

			String message = "::map:" + label + ";:" + lat + "," + lon + ";:" + formattedZoom;

			Intent result = new Intent();
			result.putExtra(RESULT_MAP_MESSAGE, message);
			setResult(RESULT_OK, result);
			finish();
		});
	}

	private void hideKeyboard() {
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			view.clearFocus();  // optional: remove focus too
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		map.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		map.onPause();
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();  // Simply close this Activity
		return true;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if (v instanceof EditText) {
				int[] outLocation = new int[2];
				v.getLocationOnScreen(outLocation);
				float x = event.getRawX() + v.getLeft() - outLocation[0];
				float y = event.getRawY() + v.getTop() - outLocation[1];

				if (x < v.getLeft() || x > v.getRight() ||
						y < v.getTop() || y > v.getBottom()) {
					hideKeyboard();
				}
			}
		}
		return super.dispatchTouchEvent(event);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}
}