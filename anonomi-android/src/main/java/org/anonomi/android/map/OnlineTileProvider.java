package org.anonomi.android.map;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.MapTileIndex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OnlineTileProvider extends MapTileModuleProviderBase {

	private static final String TAG = "OnlineTileProvider";

	private final OkHttpClient httpClient;
	private final String tileUrlTemplate;
	private final File cacheDir;
	private final boolean cacheEnabled;
	private ITileSource tileSource;

	public OnlineTileProvider(OkHttpClient httpClient, String tileUrlTemplate,
			File cacheDir, boolean cacheEnabled) {
		super(2, 18);
		this.httpClient = httpClient;
		this.tileUrlTemplate = tileUrlTemplate;
		this.cacheDir = cacheDir;
		this.cacheEnabled = cacheEnabled;
	}

	@Override
	public void setTileSource(ITileSource tileSource) {
		this.tileSource = tileSource;
	}

	@Override
	public int getMinimumZoomLevel() {
		return 2;
	}

	@Override
	public int getMaximumZoomLevel() {
		return 18;
	}

	@Override
	public boolean getUsesDataConnection() {
		return true;
	}

	@Override
	public String getName() {
		return "OnlineTileProvider";
	}

	@Override
	public String getThreadGroupName() {
		return "OnlineTileProviderThreadGroup";
	}

	@Override
	public TileLoader getTileLoader() {
		return new OnlineTileLoader();
	}

	private class OnlineTileLoader extends TileLoader {

		@Override
		public Drawable loadTile(long pMapTileIndex) {
			if (tileSource == null) return null;

			int z = MapTileIndex.getZoom(pMapTileIndex);
			int x = MapTileIndex.getX(pMapTileIndex);
			int y = MapTileIndex.getY(pMapTileIndex);

			// Serve from disk cache if available and caching is enabled.
			if (cacheEnabled) {
				File cacheFile = new File(cacheDir, z + "/" + x + "/" + y + ".png");
				if (cacheFile.exists() && cacheFile.length() > 0) {
					try (FileInputStream fis = new FileInputStream(cacheFile)) {
						return tileSource.getDrawable(fis);
					} catch (Exception e) {
						// Corrupted file — delete it and fall through to re-download
						cacheFile.delete();
					}
				}
			}

			String url = tileUrlTemplate
					.replace("{z}", String.valueOf(z))
					.replace("{x}", String.valueOf(x))
					.replace("{y}", String.valueOf(y));

			try {
				Request request = new Request.Builder().url(url).build();
				Response response = httpClient.newCall(request).execute();
				ResponseBody body = response.body();
				if (!response.isSuccessful() || body == null) return null;

				byte[] bytes = body.bytes();

				// Persist to disk only when caching is enabled.
				if (cacheEnabled) {
					File cacheFile = new File(cacheDir, z + "/" + x + "/" + y + ".png");
					cacheFile.getParentFile().mkdirs();
					try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
						fos.write(bytes);
					}
				}

				return tileSource.getDrawable(new ByteArrayInputStream(bytes));
			} catch (Exception e) {
				Log.d(TAG, "Could not fetch tile " + z + "/" + x + "/" + y);
				return null;
			}
		}
	}
}
