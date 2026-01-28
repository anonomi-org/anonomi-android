package org.anonomi.android.map;

import android.os.Bundle;
import android.view.View;
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
import org.osmdroid.views.overlay.Marker;
import androidx.core.content.ContextCompat;

import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;

import android.graphics.drawable.Drawable;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;

import android.graphics.Bitmap;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.WriterException;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;



import java.io.File;

public class MapViewActivity extends BriarActivity {

	public static final String EXTRA_LABEL = "label";
	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";
	public static final String EXTRA_ZOOM = "zoom";

	private MapView map;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set osmdroid config FIRST
		File basePath = new File(getExternalFilesDir(null), "tiles");
		File tileCache = new File(basePath, "AnonMapsCache");

		Configuration.getInstance().setOsmdroidBasePath(basePath);
		Configuration.getInstance().setOsmdroidTileCache(tileCache);
		Configuration.getInstance().setUserAgentValue(getPackageName());

		setContentView(R.layout.activity_map_view);

		android.util.Log.d("MAP_VIEW", "Tiles path: " + tileCache.getAbsolutePath());

		String label = getIntent().getStringExtra(EXTRA_LABEL);
		double latitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
		double longitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
		String zoomStr = getIntent().getStringExtra(EXTRA_ZOOM);
		if (zoomStr != null) {
			zoomStr = zoomStr.trim().replace(":", "");
		}
		android.util.Log.d("MAP_VIEW", "Got zoomStr: " + zoomStr);
		double zoom = 15; // default zoom
		try {
			zoom = Double.parseDouble(zoomStr);
		} catch (Exception ignored) {}


		// Update TextView info
		TextView mapInfo = findViewById(R.id.map_info);
		mapInfo.setText("📍 " + label + "\nLat: " + latitude + "\nLon: " + longitude + "\nZoom: " + zoom);

		// Set up the plain XYTileSource (just meta info)
		XYTileSource tileSource = new XYTileSource(
				"LooseFiles", 0, 18, 256, ".png", new String[]{}
		);

		// Set up the MapView
		ConstraintLayout rootLayout = findViewById(R.id.root_layout);
		map = new MapView(this);

		// Set up the custom LooseFilesTileProvider
		LooseFilesTileProvider looseFilesModule = new LooseFilesTileProvider(tileCache);
		looseFilesModule.setTileSource(tileSource);

		// Wrap the module in a provider array
		MapTileProviderArray provider = new MapTileProviderArray(
				tileSource,
				null,
				new MapTileModuleProviderBase[]{looseFilesModule}
		);

		map.setTileProvider(provider);
		map.setTileSource(tileSource);

		android.util.Log.d("MAP_VIEW", "Tile provider and source set: " + tileSource.name());

		map.setId(View.generateViewId());
		map.setLayoutParams(new ConstraintLayout.LayoutParams(
				ConstraintLayout.LayoutParams.MATCH_PARENT,
				0
		));
		rootLayout.addView(map);

		ConstraintSet set = new ConstraintSet();
		set.clone(rootLayout);
		set.connect(map.getId(), ConstraintSet.TOP, R.id.map_info, ConstraintSet.BOTTOM);
		set.connect(map.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
		set.connect(map.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
		set.connect(map.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
		set.applyTo(rootLayout);

		map.setMultiTouchControls(true);
		map.setUseDataConnection(false);  // offline only

		GeoPoint point = new GeoPoint(latitude, longitude);
		map.getController().setZoom(zoom);
		map.getController().setCenter(point);

		Marker marker = new Marker(map);
		marker.setPosition(point);
		marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		marker.setTitle(label);
		Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.map_marker);
		if (vectorDrawable != null) {
			int sizeInPx = (int) getResources().getDimension(R.dimen.marker_size);  // You can define this in dimens.xml (e.g., 36dp)
			Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			vectorDrawable.draw(canvas);
			marker.setIcon(new BitmapDrawable(getResources(), bitmap));
		}
		map.getOverlays().add(marker);


		ImageView qrSmall = findViewById(R.id.qrCodeSmall);

		String geoContent = "geo:" + latitude + "," + longitude + "?z=" + zoom;

		try {
			QRCodeWriter writer = new QRCodeWriter();
			Bitmap qrBitmap = toBitmap(writer.encode(geoContent, BarcodeFormat.QR_CODE, 400, 400));
			qrSmall.setImageBitmap(qrBitmap);

			qrSmall.setOnClickListener(v -> {
				try {
					// Generate larger QR code when clicked (800x800)
					Bitmap largeQrBitmap = toBitmap(writer.encode(geoContent, BarcodeFormat.QR_CODE, 800, 800));

					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					ImageView qrLarge = new ImageView(this);
					qrLarge.setImageBitmap(largeQrBitmap);
					qrLarge.setAdjustViewBounds(true);
					qrLarge.setScaleType(ImageView.ScaleType.FIT_CENTER);

					int padding = (int) getResources().getDimension(R.dimen.qr_dialog_padding);
					qrLarge.setPadding(padding, padding, padding, padding);

					builder.setView(qrLarge);
					builder.setPositiveButton("Close", null);
					AlertDialog dialog = builder.create();
					dialog.show();

				} catch (WriterException e) {
					Toast.makeText(this, "Error generating large QR code", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			});

		} catch (WriterException e) {
			Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
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

	private Bitmap toBitmap(com.google.zxing.common.BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
			}
		}
		return bmp;
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}
}