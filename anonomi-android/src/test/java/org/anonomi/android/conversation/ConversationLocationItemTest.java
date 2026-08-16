package org.anonomi.android.conversation;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConversationLocationItemTest {

	/**
	 * The adapter picks a view holder from the item's layout, and the holder
	 * for the message layouts casts what it is given to a message item. A
	 * location is shown in those layouts, so it has to be one, or binding it
	 * throws {@link ClassCastException} the first time one is displayed.
	 */
	@Test
	public void testIsAMessageItem() {
		assertTrue(ConversationMessageItem.class
				.isAssignableFrom(ConversationLocationItem.class));
	}
}
