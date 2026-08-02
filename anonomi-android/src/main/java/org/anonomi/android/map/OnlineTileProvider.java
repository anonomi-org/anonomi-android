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

	/** Thread pool size and pending queue depth for tile downloads. */
	private static final int THREAD_POOL_SIZE = 2;
	private static final int PENDING_QUEUE_SIZE = 18;
	/** Widest range osmdroid will ask for; a server may declare less. */
	private static final int ZOOM_FLOOR = 0;
	private static final int ZOOM_CEILING = 22;

	private final OkHttpClient httpClient;
	private final String tileUrlTemplate;
	private final File cacheDir;
	private final boolean cacheEnabled;
	private final int zoomMin;
	private final int zoomMax;
	private ITileSource tileSource;

	public OnlineTileProvider(OkHttpClient httpClient, String tileUrlTemplate,
			File cacheDir, boolean cacheEnabled, int zoomMin, int zoomMax) {
		super(THREAD_POOL_SIZE, PENDING_QUEUE_SIZE);
		this.httpClient = httpClient;
		this.tileUrlTemplate = tileUrlTemplate;
		this.cacheDir = cacheDir;
		this.cacheEnabled = cacheEnabled;
		// Honour the range the map declares, but keep it inside what osmdroid
		// can address and ordered, since the values come from a remote server.
		int min = Math.max(ZOOM_FLOOR, Math.min(zoomMin, ZOOM_CEILING));
		int max = Math.max(ZOOM_FLOOR, Math.min(zoomMax, ZOOM_CEILING));
		this.zoomMin = Math.min(min, max);
		this.zoomMax = Math.max(min, max);
	}

	@Override
	public void setTileSource(ITileSource tileSource) {
		this.tileSource = tileSource;
	}

	@Override
	public int getMinimumZoomLevel() {
		return zoomMin;
	}

	@Override
	public int getMaximumZoomLevel() {
		return zoomMax;
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
