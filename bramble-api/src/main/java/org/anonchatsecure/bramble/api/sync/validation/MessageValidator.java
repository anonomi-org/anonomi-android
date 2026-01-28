package org.anonchatsecure.bramble.api.sync.validation;

import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.InvalidMessageException;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageContext;

public interface MessageValidator {

	/**
	 * Validates the given message and returns its metadata and
	 * dependencies.
	 */
	MessageContext validateMessage(Message m, Group g)
			throws InvalidMessageException;
}
