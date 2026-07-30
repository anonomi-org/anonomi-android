package org.anonomi.android.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.function.BiConsumer;


public class MapImporter {

	@FunctionalInterface
	public interface TriConsumer<A, B, C> {
		void accept(A a, B b, C c);
	}

	/**
	 * Reads the current zip entry fully. InputStream#readAllBytes needs API 33,
	 * well above our minSdkVersion, so read in chunks instead.
	 */
	private static byte[] readEntry(ZipInputStream zis) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = zis.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	public static void importOfflineMaps(Context context, Uri zipUri, TriConsumer<String, String, String> callback) {
		new Thread(() -> {
			String regionName = "unknown";
			String statusText = "Incomplete";
			String metadataJsonText = null;

			try (InputStream inputStream = context.getContentResolver().openInputStream(zipUri);
					ZipInputStream zis = new ZipInputStream(inputStream)) {

				ZipEntry entry;
				File destDir = new File(context.getExternalFilesDir(null), "tiles/");
				if (!destDir.exists()) destDir.mkdirs();

				Log.d("MapImporter", "Started extracting to: " + destDir.getAbsolutePath());

				// Track extracted size
				long totalBytes = 0;

				while ((entry = zis.getNextEntry()) != null) {
					if (entry.isDirectory()) {
						zis.closeEntry();
						continue;
					}

					// Check if it's the .amd file
					if (entry.getName().endsWith(".amd")) {
						Log.d("MapImporter", "Found metadata: " + entry.getName());
						// Read metadata
						byte[] metaBytes = readEntry(zis);
						String jsonText = new String(metaBytes, StandardCharsets.UTF_8);

						metadataJsonText = jsonText;

						JSONObject meta = new JSONObject(jsonText);
						regionName = meta.optString("region", "");
						if (regionName.isEmpty()) {
							// Derive a name from the .amd filename
							String amdName = entry.getName();
							int slash = amdName.lastIndexOf('/');
							if (slash >= 0) amdName = amdName.substring(slash + 1);
							amdName = amdName.replace(".amd", "");
							regionName = amdName.equals("export") ? "Imported Map" : amdName;
						}

						Log.d("MapImporter", "Parsed region: " + regionName);
						zis.closeEntry();
						continue;
					}

					// Save tile file
					File newFile = new File(destDir, entry.getName());
					File parent = newFile.getParentFile();
					if (!parent.exists()) parent.mkdirs();

					try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile))) {
						byte[] buffer = new byte[4096];
						int len;
						while ((len = zis.read(buffer)) > 0) {
							bos.write(buffer, 0, len);
							totalBytes += len;
						}
					}

					zis.closeEntry();
				}

				// Done
				String sizeStr = humanReadableByteCountBin(totalBytes);
				statusText = "Size: " + sizeStr + ", Status: Imported";

				Log.d("MapImporter", "Import completed. Region: " + regionName + ", Size: " + sizeStr);

			} catch (Exception e) {
				Log.e("MapImporter", "Failed to import offline maps", e);
				statusText = "Failed to import";
			}

			// Notify the UI via callback
		if (callback != null) {
			String finalRegionName = regionName;
			String finalStatusText = statusText;
			String finalMetadata = metadataJsonText != null ? metadataJsonText : "{}";
			new android.os.Handler(context.getMainLooper()).post(() ->
					callback.accept(finalRegionName, finalStatusText, finalMetadata)
			);
		}
		}).start();
	}

	// Helper to make size readable
	private static String humanReadableByteCountBin(long bytes) {
		String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
		int unitIndex = 0;
		double size = bytes;
		while (size >= 1024 && unitIndex < units.length - 1) {
			size /= 1024;
			unitIndex++;
		}
		return String.format("%.1f %s", size, units[unitIndex]);
	}
}