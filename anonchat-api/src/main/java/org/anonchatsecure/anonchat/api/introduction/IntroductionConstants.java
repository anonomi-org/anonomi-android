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

package org.anonchatsecure.anonchat.api.introduction;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;

public interface IntroductionConstants {

	/**
	 * The maximum length of the introducer's optional message to the
	 * introducees in UTF-8 bytes.
	 */
	int MAX_INTRODUCTION_TEXT_LENGTH = MAX_MESSAGE_BODY_LENGTH - 1024;

	String LABEL_SESSION_ID = "org.anonchatsecure.anonchat.introduction/SESSION_ID";

	String LABEL_MASTER_KEY = "org.anonchatsecure.anonchat.introduction/MASTER_KEY";

	String LABEL_ALICE_MAC_KEY =
			"org.anonchatsecure.anonchat.introduction/ALICE_MAC_KEY";

	String LABEL_BOB_MAC_KEY =
			"org.anonchatsecure.anonchat.introduction/BOB_MAC_KEY";

	String LABEL_AUTH_MAC = "org.anonchatsecure.anonchat.introduction/AUTH_MAC";

	String LABEL_AUTH_SIGN = "org.anonchatsecure.anonchat.introduction/AUTH_SIGN";

	String LABEL_AUTH_NONCE = "org.anonchatsecure.anonchat.introduction/AUTH_NONCE";

	String LABEL_ACTIVATE_MAC =
			"org.anonchatsecure.anonchat.introduction/ACTIVATE_MAC";

}
