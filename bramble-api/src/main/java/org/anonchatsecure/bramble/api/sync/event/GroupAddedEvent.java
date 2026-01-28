package org.anonchatsecure.bramble.api.sync.event;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.sync.Group;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

/**
 * An event that is broadcast when a group is added to the database.
 */
@Immutable
@NotNullByDefault
public class GroupAddedEvent extends Event {

	private final Group group;

	public GroupAddedEvent(Group group) {
		this.group = group;
	}

	public Group getGroup() {
		return group;
	}
}
