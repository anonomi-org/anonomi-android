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

package org.anonchatsecure.anonchat.api.autodelete;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

public interface AutoDeleteConstants {

	/**
	 * The minimum valid auto-delete timer duration in milliseconds.
	 */
	long MIN_AUTO_DELETE_TIMER_MS = MINUTES.toMillis(1);

	/**
	 * The maximum valid auto-delete timer duration in milliseconds.
	 */
	long MAX_AUTO_DELETE_TIMER_MS = DAYS.toMillis(365);

	/**
	 * Placeholder value indicating that a message has no auto-delete timer.
	 * This value should not be sent over the wire - send null instead.
	 */
	long NO_AUTO_DELETE_TIMER = -1;
}
