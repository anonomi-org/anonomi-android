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

package org.anonchatsecure.anonchat.messaging;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.BdfReader;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.db.Metadata;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.anonchat.api.messaging.Location;
import org.jmock.Expectations;
import org.junit.Test;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.anonchatsecure.bramble.test.TestUtils.getClientId;
import static org.anonchatsecure.bramble.test.TestUtils.getGroup;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;
import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The factory and the validator are otherwise only tested against bodies
 * written by hand, so a disagreement between them about the shape of a
 * location would leave both sets of tests passing and the wire broken.
 */
public class LocationRoundTripTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final BdfReaderFactory bdfReaderFactory =
			context.mock(BdfReaderFactory.class);
	private final MetadataEncoder metadataEncoder =
			context.mock(MetadataEncoder.class);
	private final Clock clock = context.mock(Clock.class);
	private final BdfReader reader = context.mock(BdfReader.class);

	private final Group group = getGroup(getClientId(), 123);
	private final Message message = getMessage(group.getId());

	@Test
	public void testFactoryOutputPassesTheValidator() throws Exception {
		Location location = new Location("Café; on the corner", 38.7223,
				-9.1393, 15.0);
		AtomicReference<BdfList> written = new AtomicReference<>();

		context.checking(new Expectations() {{
			oneOf(clientHelper).createMessage(with(group.getId()),
					with(message.getTimestamp()), with(any(BdfList.class)));
			will(new org.jmock.api.Action() {
				@Override
				public Object invoke(org.jmock.api.Invocation i) {
					written.set((BdfList) i.getParameter(2));
					return message;
				}

				@Override
				public void describeTo(org.hamcrest.Description d) {
					d.appendText("captures the serialised body");
				}
			});
		}});

		new PrivateMessageFactoryImpl(clientHelper).createLocationMessage(
				group.getId(), message.getTimestamp(), location,
				NO_AUTO_DELETE_TIMER);

		BdfList body = written.get();
		assertNotNull("the factory serialised nothing", body);

		// Now hand exactly those bytes to the validator
		AtomicReference<BdfDictionary> meta = new AtomicReference<>();
		context.checking(new Expectations() {{
			oneOf(clock).currentTimeMillis();
			will(returnValue(message.getTimestamp() + 1000));
			oneOf(bdfReaderFactory).createReader(with(any(InputStream.class)));
			will(returnValue(reader));
			oneOf(reader).readList();
			will(returnValue(body));
			oneOf(reader).eof();
			will(returnValue(true));
			oneOf(metadataEncoder).encode(with(any(BdfDictionary.class)));
			will(new org.jmock.api.Action() {
				@Override
				public Object invoke(org.jmock.api.Invocation i) {
					meta.set((BdfDictionary) i.getParameter(0));
					return new Metadata();
				}

				@Override
				public void describeTo(org.hamcrest.Description d) {
					d.appendText("captures the metadata");
				}
			});
		}});

		new PrivateMessageValidator(bdfReaderFactory, metadataEncoder, clock)
				.validateMessage(message, group);

		// The label survives the separator that broke the text format
		BdfDictionary m = meta.get();
		assertNotNull(m);
		assertEquals(location.getLabel(), m.getString("locationLabel"));
		assertEquals(location.getLatitude(),
				m.getDouble("locationLatitude"), 0.0);
		assertEquals(location.getLongitude(),
				m.getDouble("locationLongitude"), 0.0);
		assertEquals(location.getZoom(), m.getDouble("locationZoom"), 0.0);
	}
}
