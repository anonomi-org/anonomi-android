package org.anonomi.android.map;

public class OnlineMapEntry {

	public final String id;
	public final String name;
	public final String description;
	public final String tileUrl;
	public final int zoomMin;
	public final int zoomMax;
	public final double bboxNorth;
	public final double bboxSouth;
	public final double bboxEast;
	public final double bboxWest;
	/** Whether fetched tiles are written to / read from the local disk cache. */
	public final boolean cacheEnabled;
	/** Whether tiles from imported offline ZIPs are used as a first-tier source. */
	public final boolean useOfflineImports;
	/** Whether this map is allowed to fetch tiles over the network. */
	public final boolean fetchOnline;

	public OnlineMapEntry(String id, String name, String description, String tileUrl,
			int zoomMin, int zoomMax,
			double bboxNorth, double bboxSouth, double bboxEast, double bboxWest,
			boolean cacheEnabled, boolean useOfflineImports, boolean fetchOnline) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.tileUrl = tileUrl;
		this.zoomMin = zoomMin;
		this.zoomMax = zoomMax;
		this.bboxNorth = bboxNorth;
		this.bboxSouth = bboxSouth;
		this.bboxEast = bboxEast;
		this.bboxWest = bboxWest;
		this.cacheEnabled = cacheEnabled;
		this.useOfflineImports = useOfflineImports;
		this.fetchOnline = fetchOnline;
	}
}
