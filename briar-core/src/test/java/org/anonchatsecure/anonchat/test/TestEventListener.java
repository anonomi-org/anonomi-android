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

package org.anonchatsecure.anonchat.test;

import org.anonchatsecure.bramble.api.event.Event;
import org.anonchatsecure.bramble.api.event.EventListener;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.atomic.AtomicReference;

import static org.anonchatsecure.anonchat.test.BriarIntegrationTest.waitForEvents;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@NotNullByDefault
public class TestEventListener<T extends Event> implements EventListener {

	@FunctionalInterface
	public interface EventRunnable {
		void run() throws Exception;
	}

	public static <T extends Event> T assertEvent(
			BriarIntegrationTestComponent c, Class<T> clazz, EventRunnable r)
			throws Exception {
		TestEventListener<T> listener = new TestEventListener<>(clazz);
		c.getEventBus().addListener(listener);
		try {
			r.run();
			waitForEvents(c);
			return listener.assertAndGetEvent();
		} finally {
			c.getEventBus().removeListener(listener);
		}
	}

	private TestEventListener(Class<T> clazz) {
		this.clazz = clazz;
	}

	private final Class<T> clazz;

	private final AtomicReference<T> event = new AtomicReference<>();

	@Override
	public void eventOccurred(Event e) {
		if (e.getClass().equals(clazz)) {
			//noinspection unchecked
			assertNull("Event already received", event.getAndSet((T) e));
		}
	}

	private T assertAndGetEvent() {
		T t = event.get();
		assertNotNull("No event received", t);
		return t;
	}

}
