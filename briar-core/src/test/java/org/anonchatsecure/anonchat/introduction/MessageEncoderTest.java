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

package org.anonchatsecure.anonchat.introduction;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageFactory;
import org.anonchatsecure.bramble.test.BrambleMockTestCase;
import org.jmock.Expectations;
import org.junit.Test;

import static org.anonchatsecure.bramble.api.sync.SyncConstants.MAX_MESSAGE_BODY_LENGTH;
import static org.anonchatsecure.bramble.test.TestUtils.getAuthor;
import static org.anonchatsecure.bramble.test.TestUtils.getMessage;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomId;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.MAX_INTRODUCTION_TEXT_LENGTH;
import static org.anonchatsecure.anonchat.introduction.MessageType.REQUEST;

public class MessageEncoderTest extends BrambleMockTestCase {

	private final ClientHelper clientHelper = context.mock(ClientHelper.class);
	private final MessageFactory messageFactory =
			context.mock(MessageFactory.class);
	private final MessageEncoder messageEncoder =
			new MessageEncoderImpl(clientHelper, messageFactory);

	private final GroupId groupId = new GroupId(getRandomId());
	private final Message message =
			getMessage(groupId, MAX_MESSAGE_BODY_LENGTH);
	private final long timestamp = message.getTimestamp();
	private final byte[] body = message.getBody();
	private final Author author = getAuthor();
	private final BdfList authorList = new BdfList();
	private final String text = getRandomString(MAX_INTRODUCTION_TEXT_LENGTH);

	@Test
	public void testEncodeRequestMessage() throws FormatException {
		context.checking(new Expectations() {{
			oneOf(clientHelper).toList(author);
			will(returnValue(authorList));
		}});
		expectCreateMessage(
				BdfList.of(REQUEST.getValue(), null, authorList, text));

		messageEncoder.encodeRequestMessage(groupId, timestamp, null,
				author, text);
	}

	private void expectCreateMessage(BdfList bodyList) throws FormatException {
		context.checking(new Expectations() {{
			oneOf(clientHelper).toByteArray(bodyList);
			will(returnValue(body));
			oneOf(messageFactory).createMessage(groupId, timestamp, body);
			will(returnValue(message));
		}});
	}

}
