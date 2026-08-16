/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.api.messaging;

import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * A place on a map, as shared in a conversation. The label is written by
 * whoever shared it, so it is arbitrary text.
 */
@Immutable
@NotNullByDefault
public class Location {

	private final String label;
	private final double latitude;
	private final double longitude;
	private final double zoom;

	public Location(String label, double latitude, double longitude,
			double zoom) {
		this.label = label;
		this.latitude = latitude;
		this.longitude = longitude;
		this.zoom = zoom;
	}

	public String getLabel() {
		return label;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getZoom() {
		return zoom;
	}
}
