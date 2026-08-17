package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.briarproject.nullsafety.NotNullByDefault;

/**
 * An event bus for tests that drive the database directly and have no
 * interest in what it broadcasts.
 */
@NotNullByDefault
public class NullEventBus implements EventBus {

	@Override
	public void addListener(EventListener l) {
	}

	@Override
	public void removeListener(EventListener l) {
	}

	@Override
	public void broadcast(Event e) {
	}
}
