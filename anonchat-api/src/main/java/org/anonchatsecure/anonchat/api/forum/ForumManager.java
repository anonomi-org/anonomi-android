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

package org.anonchatsecure.anonchat.api.forum;

import org.anonchatsecure.bramble.api.crypto.CryptoExecutor;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

@NotNullByDefault
public interface ForumManager {

	/**
	 * The unique ID of the forum client.
	 */
	ClientId CLIENT_ID = new ClientId("org.anonchatsecure.anonchat.forum");

	/**
	 * The current major version of the forum client.
	 */
	int MAJOR_VERSION = 0;

	/**
	 * The current minor version of the forum client.
	 */
	int MINOR_VERSION = 0;

	/**
	 * Subscribes to a forum.
	 */
	Forum addForum(String name) throws DbException;

	/**
	 * Subscribes to a forum within the given {@link Transaction}.
	 */
	void addForum(Transaction txn, Forum f) throws DbException;

	/**
	 * Unsubscribes from a forum.
	 */
	void removeForum(Forum f) throws DbException;

	/**
	 * Unsubscribes from a forum.
	 */
	void removeForum(Transaction txn, Forum f) throws DbException;

	/**
	 * Creates a local forum post.
	 */
	@CryptoExecutor
	ForumPost createLocalPost(GroupId groupId, String text, long timestamp,
			@Nullable MessageId parentId, LocalAuthor author);

	/**
	 * Creates a local forum audio post.
	 */
	@CryptoExecutor
	ForumPost createLocalAudioPost(GroupId groupId, String text,
			long timestamp, @Nullable MessageId parentId,
			LocalAuthor author, byte[] audioData, String contentType);

	/**
	 * Stores a local forum post.
	 */
	ForumPostHeader addLocalPost(ForumPost p) throws DbException;

	/**
	 * Stores a local forum post.
	 */
	ForumPostHeader addLocalPost(Transaction txn, ForumPost p)
			throws DbException;

	/**
	 * Returns the forum with the given ID.
	 */
	Forum getForum(GroupId g) throws DbException;

	/**
	 * Returns the forum with the given ID.
	 */
	Forum getForum(Transaction txn, GroupId g) throws DbException;

	/**
	 * Returns all forums to which the user subscribes.
	 */
	Collection<Forum> getForums() throws DbException;

	/**
	 * Returns all forums to which the user subscribes.
	 */
	Collection<Forum> getForums(Transaction txn) throws DbException;

	/**
	 * Returns the text of the forum post with the given ID.
	 */
	String getPostText(MessageId m) throws DbException;

	/**
	 * Returns the text of the forum post with the given ID.
	 */
	String getPostText(Transaction txn, MessageId m) throws DbException;

	/**
	 * Returns the audio data of the forum post with the given ID,
	 * or null if the post has no audio.
	 */
	@Nullable
	byte[] getPostAudioData(MessageId m) throws DbException;

	/**
	 * Returns the audio data of the forum post with the given ID,
	 * or null if the post has no audio.
	 */
	@Nullable
	byte[] getPostAudioData(Transaction txn, MessageId m) throws DbException;

	/**
	 * Returns the audio content type of the forum post with the given ID,
	 * or null if the post has no audio.
	 */
	@Nullable
	String getPostAudioContentType(MessageId m) throws DbException;

	/**
	 * Returns the audio content type of the forum post with the given ID,
	 * or null if the post has no audio.
	 */
	@Nullable
	String getPostAudioContentType(Transaction txn, MessageId m)
			throws DbException;

	/**
	 * Returns the headers of all posts in the given forum.
	 */
	Collection<ForumPostHeader> getPostHeaders(GroupId g) throws DbException;

	/**
	 * Returns the headers of all posts in the given forum.
	 */
	List<ForumPostHeader> getPostHeaders(Transaction txn, GroupId g)
			throws DbException;

	/**
	 * Registers a hook to be called whenever a forum is removed.
	 */
	void registerRemoveForumHook(RemoveForumHook hook);

	/**
	 * Returns the group count for the given forum.
	 */
	GroupCount getGroupCount(GroupId g) throws DbException;

	/**
	 * Returns the group count for the given forum.
	 */
	GroupCount getGroupCount(Transaction txn, GroupId g) throws DbException;

	/**
	 * Marks a message as read or unread and updates the group count.
	 */
	void setReadFlag(GroupId g, MessageId m, boolean read) throws DbException;

	/**
	 * Marks a message as read or unread and updates the group count.
	 */
	void setReadFlag(Transaction txn, GroupId g, MessageId m, boolean read) throws DbException;

	interface RemoveForumHook {
		/**
		 * Called when a forum is being removed.
		 *
		 * @param txn A read-write transaction
		 * @param f The forum that is being removed
		 */
		void removingForum(Transaction txn, Forum f) throws DbException;
	}
}
