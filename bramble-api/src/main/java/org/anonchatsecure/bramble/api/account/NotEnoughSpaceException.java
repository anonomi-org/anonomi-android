package org.anonchatsecure.bramble.api.account;

import java.io.IOException;

/**
 * Thrown when there is not enough room to take a copy of the database, which
 * is worth telling the user apart from other reasons a backup can fail
 * because it is the one they can do something about.
 */
public class NotEnoughSpaceException extends IOException {
}
