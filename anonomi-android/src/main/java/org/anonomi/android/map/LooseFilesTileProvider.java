package org.anonomi.android.map;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.MapTileIndex;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LooseFilesTileProvider extends MapTileModuleProviderBase {

	private final File tileCacheDir;
	private ITileSource tileSource;

	public LooseFilesTileProvider(File tileCacheDir) {
		super(2, 18);  // min/max zoom levels
		this.tileCacheDir = tileCacheDir;
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
		return false;  // offline only
	}

	@Override
	public String getName() {
		return "LooseFilesTileProvider";
	}

	@Override
	public String getThreadGroupName() {
		return "LooseFilesTileProviderThreadGroup";
	}

	@Override
	public TileLoader getTileLoader() {
		return new LooseFilesTileLoader();
	}

	private class LooseFilesTileLoader extends TileLoader {
		@Override
		public Drawable loadTile(long pMapTileIndex) {
			if (tileSource == null) {
				Log.e("LooseFilesTileLoader", "No tile source set");
				return null;
			}

			int z = MapTileIndex.getZoom(pMapTileIndex);
			int x = MapTileIndex.getX(pMapTileIndex);
			int y = MapTileIndex.getY(pMapTileIndex);
			File tileFile = new File(tileCacheDir, z + "/" + x + "/" + y + ".png");

			Log.d("LooseFilesTileLoader", "Trying to load tile: " + tileFile.getAbsolutePath());

			if (!tileFile.exists()) {
				Log.w("LooseFilesTileLoader", "Tile not found: " + tileFile.getAbsolutePath());
				return null;
			}

			try (InputStream is = new FileInputStream(tileFile)) {
				return tileSource.getDrawable(is);
			} catch (Exception e) {
				Log.e("LooseFilesTileLoader", "Error loading tile: " + tileFile.getAbsolutePath(), e);
			}
			return null;
		}
	}
}