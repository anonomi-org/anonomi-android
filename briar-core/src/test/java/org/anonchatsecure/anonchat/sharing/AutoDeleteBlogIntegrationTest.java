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

package org.anonchatsecure.anonchat.sharing;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.anonchat.api.blog.Blog;
import org.anonchatsecure.anonchat.api.blog.BlogManager;
import org.anonchatsecure.anonchat.api.blog.event.BlogInvitationResponseReceivedEvent;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager.ConversationClient;
import org.anonchatsecure.anonchat.api.conversation.event.ConversationMessageReceivedEvent;
import org.anonchatsecure.anonchat.api.sharing.InvitationResponse;
import org.anonchatsecure.anonchat.api.sharing.Shareable;
import org.anonchatsecure.anonchat.api.sharing.SharingManager;
import org.anonchatsecure.anonchat.test.BriarIntegrationTestComponent;
import org.junit.Before;

import java.util.Collection;

public class AutoDeleteBlogIntegrationTest
		extends AbstractAutoDeleteIntegrationTest {

	private SharingManager<Blog> sharingManager0;
	private SharingManager<Blog> sharingManager1;
	private Blog shareable;
	private BlogManager manager0;
	private BlogManager manager1;
	private Class<BlogInvitationResponseReceivedEvent>
			responseReceivedEventClass;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		manager0 = c0.getBlogManager();
		manager1 = c1.getBlogManager();
		// personalBlog(author0) is already shared with c1
		shareable = manager0.getPersonalBlog(author2);
		sharingManager0 = c0.getBlogSharingManager();
		sharingManager1 = c1.getBlogSharingManager();
		responseReceivedEventClass = BlogInvitationResponseReceivedEvent.class;
	}

	@Override
	protected ConversationClient getConversationClient(
			BriarIntegrationTestComponent component) {
		return component.getBlogSharingManager();
	}

	@Override
	protected SharingManager<? extends Shareable> getSharingManager0() {
		return sharingManager0;
	}

	@Override
	protected SharingManager<? extends Shareable> getSharingManager1() {
		return sharingManager1;
	}

	@Override
	protected Shareable getShareable() {
		return shareable;
	}

	@Override
	protected Collection<Blog> subscriptions0() throws DbException {
		return manager0.getBlogs();
	}

	@Override
	protected Collection<Blog> subscriptions1() throws DbException {
		return manager1.getBlogs();
	}

	@Override
	protected Class<? extends ConversationMessageReceivedEvent<? extends InvitationResponse>> getResponseReceivedEventClass() {
		return responseReceivedEventClass;
	}
}
