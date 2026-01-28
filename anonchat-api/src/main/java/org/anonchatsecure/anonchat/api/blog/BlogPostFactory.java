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

package org.anonchatsecure.anonchat.api.blog;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.Nullable;

import static org.anonchatsecure.anonchat.api.blog.BlogManager.CLIENT_ID;

@NotNullByDefault
public interface BlogPostFactory {

	String SIGNING_LABEL_POST = CLIENT_ID.getString() + "/POST";
	String SIGNING_LABEL_COMMENT = CLIENT_ID.getString() + "/COMMENT";

	BlogPost createBlogPost(GroupId groupId, long timestamp,
			@Nullable MessageId parent, LocalAuthor author, String text)
			throws FormatException, GeneralSecurityException;

	Message createBlogComment(GroupId groupId, LocalAuthor author,
			@Nullable String comment, MessageId parentOriginalId,
			MessageId parentCurrentId)
			throws FormatException, GeneralSecurityException;

	/**
	 * Wraps a blog post
	 */
	Message wrapPost(GroupId groupId, byte[] descriptor, long timestamp,
			BdfList body) throws FormatException;

	/**
	 * Re-wraps a previously wrapped post
	 */
	Message rewrapWrappedPost(GroupId groupId, BdfList body)
			throws FormatException;

	/**
	 * Wraps a blog comment
	 */
	Message wrapComment(GroupId groupId, byte[] descriptor, long timestamp,
			BdfList body, MessageId parentCurrentId) throws FormatException;

	/**
	 * Re-wraps a previously wrapped comment
	 */
	Message rewrapWrappedComment(GroupId groupId, BdfList body,
			MessageId parentCurrentId) throws FormatException;
}
