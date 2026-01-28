package org.anonomi.android.map;

import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.MapTileIndex;
import java.io.File;

public class FileBasedTileSource extends XYTileSource {
	private final File tileCacheDir;

	public FileBasedTileSource(File tileCacheDir) {
		super("FileBased", 0, 18, 256, ".png", new String[]{});
		this.tileCacheDir = tileCacheDir;
	}

	@Override
	public String getTileURLString(long mapTileIndex) {
		int z = MapTileIndex.getZoom(mapTileIndex);
		int x = MapTileIndex.getX(mapTileIndex);
		int y = MapTileIndex.getY(mapTileIndex);
		File tileFile = new File(tileCacheDir, z + "/" + x + "/" + y + ".png");
		String tilePath = "file://" + tileFile.getAbsolutePath();
		android.util.Log.d("TileFetch", "Requesting tile z=" + z + " x=" + x + " y=" + y + " at " + tilePath);
		return tilePath;
	}
}