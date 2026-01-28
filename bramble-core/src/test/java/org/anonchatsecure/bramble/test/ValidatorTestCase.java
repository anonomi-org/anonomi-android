package org.anonchatsecure.bramble.test;

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.MetadataEncoder;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.jmock.Expectations;

import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getClientId;
import static org.anonchatsecure.bramble.test.TestUtils.getGroup;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;

public abstract class ValidatorTestCase extends BrambleMockTestCase {

	protected final ClientHelper clientHelper =
			context.mock(ClientHelper.class);
	protected final MetadataEncoder metadataEncoder =
			context.mock(MetadataEncoder.class);
	protected final Clock clock = context.mock(Clock.class);

	protected final Group group = getGroup(getClientId(), 123);
	protected final GroupId groupId = group.getId();
	protected final byte[] descriptor = group.getDescriptor();
	protected final Message message = getMessage(groupId);
	protected final MessageId messageId = message.getId();
	protected final long timestamp = message.getTimestamp();
	protected final Author author = getAuthor();
	protected final BdfList authorList = BdfList.of(
			author.getFormatVersion(),
			author.getName(),
			author.getPublicKey()
	);

	protected void expectParseAuthor(BdfList authorList, Author author)
			throws Exception {
		context.checking(new Expectations() {{
			oneOf(clientHelper).parseAndValidateAuthor(authorList);
			will(returnValue(author));
		}});
	}

}