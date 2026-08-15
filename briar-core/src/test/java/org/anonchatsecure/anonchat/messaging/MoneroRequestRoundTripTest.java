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
import org.anonchatsecure.anonchat.api.messaging.MoneroRequest;
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
import static org.junit.Assert.assertNull;

/**
 * The factory and the validator are otherwise only tested against bodies
 * written by hand, so a disagreement between them about the shape of a
 * payment request would leave both sets of tests passing and the wire
 * broken.
 */
public class MoneroRequestRoundTripTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final BdfReaderFactory bdfReaderFactory =
			context.mock(BdfReaderFactory.class);
	private final MetadataEncoder metadataEncoder =
			context.mock(MetadataEncoder.class);
	private final Clock clock = context.mock(Clock.class);
	private final BdfReader reader = context.mock(BdfReader.class);

	private final Group group = getGroup(getClientId(), 123);
	private final Message message = getMessage(group.getId());

	// A real mainnet subaddress, so the length is the one the wire sees
	private final String subaddress =
			"87i7kA61fNvMboXiYWHVygPAggKJPETFqLXXcdH4mQTrECvrTxZMtt6e6owj1k8j" +
					"UVjNR11eBuBMWHFBtxAwEVcm9dcSUxr";

	@Test
	public void testFactoryOutputPassesTheValidator() throws Exception {
		// 0.1 XMR, which is the amount a double cannot hold exactly
		MoneroRequest request = new MoneroRequest(subaddress, 100_000_000_000L,
				"Invoice 42; paid on delivery", "EUR", 155.5);

		BdfDictionary meta = roundTrip(request);
		assertEquals(subaddress, meta.getString("moneroSubaddress"));
		assertEquals(100_000_000_000L,
				meta.getLong("moneroAmount").longValue());
		assertEquals("Invoice 42; paid on delivery",
				meta.getString("moneroDescription"));
		assertEquals("EUR", meta.getString("moneroCurrency"));
		assertEquals(155.5, meta.getDouble("moneroRate"), 0.0);
	}

	@Test
	public void testFactoryOutputPassesTheValidatorWithoutOptionalFields()
			throws Exception {
		MoneroRequest request =
				new MoneroRequest(subaddress, null, null, null, null);

		BdfDictionary meta = roundTrip(request);
		assertEquals(subaddress, meta.getString("moneroSubaddress"));
		assertNull(meta.getOptionalLong("moneroAmount"));
		assertNull(meta.getOptionalString("moneroDescription"));
		assertNull(meta.getOptionalString("moneroCurrency"));
		assertNull(meta.getOptionalDouble("moneroRate"));
	}

	/**
	 * The largest amount that fits in the field, to show the bound is not
	 * one the wire imposes on a plausible request.
	 */
	@Test
	public void testCarriesTheLargestRepresentableAmount() throws Exception {
		MoneroRequest request = new MoneroRequest(subaddress, Long.MAX_VALUE,
				null, null, null);

		BdfDictionary meta = roundTrip(request);
		assertEquals(Long.MAX_VALUE,
				meta.getLong("moneroAmount").longValue());
	}

	private BdfDictionary roundTrip(MoneroRequest request) throws Exception {
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

		new PrivateMessageFactoryImpl(clientHelper)
				.createMoneroRequestMessage(group.getId(),
						message.getTimestamp(), request,
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

		BdfDictionary m = meta.get();
		assertNotNull(m);
		return m;
	}
}
