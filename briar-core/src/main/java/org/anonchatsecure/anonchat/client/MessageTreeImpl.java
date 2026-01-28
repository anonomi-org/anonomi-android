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
import org.anonchatsecure.anonchat.api.client.MessageTree;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@NotNullByDefault
public class MessageTreeImpl<T extends MessageTree.MessageNode>
		implements MessageTree<T> {

	@GuardedBy("this")
	private final Map<MessageId, List<T>> nodeMap = new HashMap<>();

	@GuardedBy("this")
	private final List<T> roots = new ArrayList<>();

	@GuardedBy("this")
	private final List<List<T>> unsortedLists = new ArrayList<>();

	@SuppressWarnings("UseCompareMethod")
	private final Comparator<T> comparator = (o1, o2) ->
			Long.valueOf(o1.getTimestamp()).compareTo(o2.getTimestamp());

	@Override
	public synchronized void clear() {
		roots.clear();
		nodeMap.clear();
	}

	@Override
	public synchronized void add(Collection<T> nodes) {
		// add all nodes to the node map
		for (T node : nodes) {
			nodeMap.put(node.getId(), new ArrayList<>());
		}
		// parse the nodes for dependencies
		for (T node : nodes) {
			parseNode(node);
		}
		sortUnsorted();
	}

	@Override
	public synchronized void add(T node) {
		add(Collections.singletonList(node));
	}

	@GuardedBy("this")
	private void markAsUnsorted(List<T> list) {
		if (!unsortedLists.contains(list))
			unsortedLists.add(list);
	}

	@GuardedBy("this")
	private void parseNode(T node) {
		if (node.getParentId() == null) {
			roots.add(node);
			markAsUnsorted(roots);
		} else {
			// retrieve the parent's children
			List<T> pChildren = nodeMap.get(node.getParentId());
			pChildren.add(node);
			markAsUnsorted(pChildren);
		}
	}

	@GuardedBy("this")
	private void sortUnsorted() {
		for (List<T> list : unsortedLists) {
			//noinspection Java8ListSort
			Collections.sort(list, comparator);
		}
		unsortedLists.clear();
	}

	@GuardedBy("this")
	private void traverse(List<T> list, T node, int level) {
		list.add(node);
		List<T> children = nodeMap.get(node.getId());
		node.setLevel(level);
		for (T child : children) {
			traverse(list, child, level + 1);
		}
	}

	@Override
	public synchronized List<T> depthFirstOrder() {
		List<T> orderedList = new ArrayList<>();
		for (T root : roots) {
			traverse(orderedList, root, 0);
		}
		return orderedList;
	}

	@Override
	public synchronized boolean contains(MessageId m) {
		return nodeMap.containsKey(m);
	}
}
