package org.anonchatsecure.bramble.api.db;

import org.anonchatsecure.bramble.api.event.Event;

/**
 * A {@link CommitAction} that broadcasts an event.
 */
public class EventAction implements CommitAction {

	private final Event event;

	EventAction(Event event) {
		this.event = event;
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}
}
