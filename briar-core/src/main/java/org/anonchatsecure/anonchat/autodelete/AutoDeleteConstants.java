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

package org.anonchatsecure.anonchat.autodelete;

interface AutoDeleteConstants {

	/**
	 * Group metadata key for storing the auto-delete timer duration.
	 */
	String GROUP_KEY_TIMER = "autoDeleteTimer";

	/**
	 * Group metadata key for storing the timestamp of the latest incoming or
	 * outgoing message carrying an auto-delete timer (including a null timer).
	 */
	String GROUP_KEY_TIMESTAMP = "autoDeleteTimestamp";

	/**
	 * Group metadata key for storing the previous auto-delete timer duration.
	 * This is used to decide whether a local change to the duration should be
	 * overwritten by a duration received from the contact.
	 */
	String GROUP_KEY_PREVIOUS_TIMER = "autoDeletePreviousTimer";

	/**
	 * Special value for {@link #GROUP_KEY_PREVIOUS_TIMER} indicating that
	 * there are no local changes to the auto-delete timer duration that need
	 * to be compared with durations received from the contact.
	 */
	long NO_PREVIOUS_TIMER = 0;
}
