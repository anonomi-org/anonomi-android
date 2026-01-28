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

package org.anonchatsecure.anonchat.client;

import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.TestUtils;
import org.anonchatsecure.anonchat.api.client.MessageTree;
import org.briarproject.nullsafety.NotNullByDefault;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

public class MessageTreeImplTest {

	private MessageTree<TestNode> tree;

	@Test
	public void testMessageTree() {
		tree = new MessageTreeImpl<>();
		testSimpleTree();
		tree.clear();
		testSimpleTree();
	}

	private void testSimpleTree() {
		TestNode[] nodes = new TestNode[5];
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new TestNode();
		}
		/*
		Construct the following tree:
		4
		1 ->
		   0  ->
		       2
		   3
		 */
		nodes[0].setParentId(nodes[1].getId());
		nodes[2].setParentId(nodes[0].getId());
		nodes[3].setParentId(nodes[1].getId());
		long timestamp = System.currentTimeMillis();
		nodes[4].setTimestamp(timestamp - 5);
		nodes[1].setTimestamp(timestamp - 4);
		nodes[0].setTimestamp(timestamp - 3);
		nodes[3].setTimestamp(timestamp - 2);
		nodes[2].setTimestamp(timestamp - 1);
		// add all nodes except the last one
		tree.add(Arrays.asList(Arrays.copyOf(nodes, nodes.length - 1)));
		tree.add(Collections.singletonList(nodes[nodes.length - 1]));
		TestNode[] sortedNodes =
				tree.depthFirstOrder().toArray(new TestNode[5]);
		assertEquals(nodes[4], sortedNodes[0]);
		assertEquals(nodes[1], sortedNodes[1]);
		assertEquals(nodes[0], sortedNodes[2]);
		assertEquals(nodes[2], sortedNodes[3]);
		assertEquals(nodes[3], sortedNodes[4]);
	}

	@NotNullByDefault
	private class TestNode implements MessageTree.MessageNode {

		private final MessageId id = new MessageId(TestUtils.getRandomId());
		@Nullable
		private MessageId parentId;
		private long timestamp;

		@Override
		public MessageId getId() {
			return id;
		}

		@Override
		@Nullable
		public MessageId getParentId() {
			return parentId;
		}

		@Override
		public void setLevel(int level) {

		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}

		private void setParentId(MessageId parentId) {
			this.parentId = parentId;
		}

		private void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
