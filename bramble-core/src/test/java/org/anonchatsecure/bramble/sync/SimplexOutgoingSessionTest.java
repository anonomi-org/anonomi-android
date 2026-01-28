package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.event.EventBus;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.sync.Ack;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.sync.SyncRecordWriter;
import org.anonchatsecure.bramble.api.sync.Versions;
import org.anonchatsecure.bramble.api.transport.StreamWriter;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.anonchatsecure.bramble.test.DbExpectations;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_IDS;
import static org.anonchatsecure.bramble.sync.SimplexOutgoingSession.BATCH_CAPACITY;
import static org.anonchatsecure.bramble.test.TestUtils.getContactId;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.test.TestUtils.getTransportId;

public class SimplexOutgoingSessionTest extends BrambleMockTestCase {

	private static final int MAX_LATENCY = Integer.MAX_VALUE;

	private final DatabaseComponent db = context.mock(DatabaseComponent.class);
	private final EventBus eventBus = context.mock(EventBus.class);
	private final StreamWriter streamWriter = context.mock(StreamWriter.class);
	private final SyncRecordWriter recordWriter =
			context.mock(SyncRecordWriter.class);

	private final ContactId contactId = getContactId();
	private final TransportId transportId = getTransportId();
	private final Ack ack =
			new Ack(singletonList(new MessageId(getRandomId())));
	private final Message message = getMessage(new GroupId(getRandomId()),
			MAX_MESSAGE_BODY_LENGTH);

	@Test
	public void testNothingToSend() throws Exception {
		SimplexOutgoingSession session = new SimplexOutgoingSession(db,
				eventBus, contactId, transportId, MAX_LATENCY,
				streamWriter, recordWriter);

		Transaction noAckTxn = new Transaction(null, false);
		Transaction noMsgTxn = new Transaction(null, false);

		context.checking(new DbExpectations() {{
			// Add listener
			oneOf(eventBus).addListener(session);
			// Send the protocol versions
			oneOf(recordWriter).writeVersions(with(any(Versions.class)));
			// No acks to send
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(noAckTxn));
			oneOf(db).generateAck(noAckTxn, contactId, MAX_MESSAGE_IDS);
			will(returnValue(null));
			// No messages to send
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(noMsgTxn));
			oneOf(db).generateBatch(noMsgTxn, contactId,
					BATCH_CAPACITY, MAX_LATENCY);
			will(returnValue(null));
			// Send the end of stream marker
			oneOf(streamWriter).sendEndOfStream();
			// Remove listener
			oneOf(eventBus).removeListener(session);
		}});

		session.run();
	}

	@Test
	public void testSomethingToSend() throws Exception {
		SimplexOutgoingSession session = new SimplexOutgoingSession(db,
				eventBus, contactId, transportId, MAX_LATENCY,
				streamWriter, recordWriter);

		Transaction ackTxn = new Transaction(null, false);
		Transaction noAckTxn = new Transaction(null, false);
		Transaction msgTxn = new Transaction(null, false);
		Transaction noMsgTxn = new Transaction(null, false);

		context.checking(new DbExpectations() {{
			// Add listener
			oneOf(eventBus).addListener(session);
			// Send the protocol versions
			oneOf(recordWriter).writeVersions(with(any(Versions.class)));
			// One ack to send
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(ackTxn));
			oneOf(db).generateAck(ackTxn, contactId, MAX_MESSAGE_IDS);
			will(returnValue(ack));
			oneOf(recordWriter).writeAck(ack);
			// No more acks
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(noAckTxn));
			oneOf(db).generateAck(noAckTxn, contactId, MAX_MESSAGE_IDS);
			will(returnValue(null));
			// One message to send
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(msgTxn));
			oneOf(db).generateBatch(msgTxn, contactId,
					BATCH_CAPACITY, MAX_LATENCY);
			will(returnValue(singletonList(message)));
			oneOf(recordWriter).writeMessage(message);
			// No more messages
			oneOf(db).transactionWithNullableResult(with(false),
					withNullableDbCallable(noMsgTxn));
			oneOf(db).generateBatch(noMsgTxn, contactId,
					BATCH_CAPACITY, MAX_LATENCY);
			will(returnValue(null));
			// Send the end of stream marker
			oneOf(streamWriter).sendEndOfStream();
			// Remove listener
			oneOf(eventBus).removeListener(session);
		}});

		session.run();
	}
}
