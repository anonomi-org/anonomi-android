package org.anonomi.android.conversation;

public class MapMessageData {
	public final String label;
	public final double latitude;
	public final double longitude;
	public final String zoom;

	public MapMessageData(String label, double latitude, double longitude, String zoom) {
		this.label = label;
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoom = zoom;
	}
}