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

package org.anonchatsecure.anonchat.privategroup;

import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.test.TestDatabaseConfigModule;
import org.anonchatsecure.anonchat.api.privategroup.GroupMember;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessage;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageHeader;
import org.anonchatsecure.anonchat.api.privategroup.JoinMessageHeader;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager;
import org.anonchatsecure.anonchat.test.BriarIntegrationTest;
import org.anonchatsecure.anonchat.test.BriarIntegrationTestComponent;
import org.anonchatsecure.anonchat.test.DaggerBriarIntegrationTestComponent;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import javax.annotation.Nullable;

import static org.anonchatsecure.anonchat.api.autodelete.AutoDeleteConstants.NO_AUTO_DELETE_TIMER;
import static org.anonchatsecure.anonchat.api.identity.AuthorInfo.Status.OURSELVES;
import static org.anonchatsecure.anonchat.api.privategroup.Visibility.INVISIBLE;
import static org.anonchatsecure.anonchat.api.privategroup.Visibility.REVEALED_BY_CONTACT;
import static org.anonchatsecure.anonchat.api.privategroup.Visibility.REVEALED_BY_US;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class tests how PrivateGroupManager and GroupInvitationManager
 * play together.
 */
public class PrivateGroupIntegrationTest
		extends BriarIntegrationTest<BriarIntegrationTestComponent> {

	private GroupId groupId0;
	private PrivateGroup privateGroup0;
	private PrivateGroupManager groupManager0, groupManager1, groupManager2;
	private GroupInvitationManager groupInvitationManager0,
			groupInvitationManager1, groupInvitationManager2;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		groupManager0 = c0.getPrivateGroupManager();
		groupManager1 = c1.getPrivateGroupManager();
		groupManager2 = c2.getPrivateGroupManager();
		groupInvitationManager0 = c0.getGroupInvitationManager();
		groupInvitationManager1 = c1.getGroupInvitationManager();
		groupInvitationManager2 = c2.getGroupInvitationManager();

		privateGroup0 =
				privateGroupFactory.createPrivateGroup("Test Group", author0);
		groupId0 = privateGroup0.getId();
		long joinTime = c0.getClock().currentTimeMillis();
		GroupMessage joinMsg0 = groupMessageFactory
				.createJoinMessage(groupId0, joinTime, author0);
		groupManager0.addPrivateGroup(privateGroup0, joinMsg0, true);
	}

	@Override
	protected void createComponents() {
		BriarIntegrationTestComponent component =
				DaggerBriarIntegrationTestComponent.builder().build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(component);
		component.inject(this);

		c0 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t0Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c0);

		c1 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t1Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c1);

		c2 = DaggerBriarIntegrationTestComponent.builder()
				.testDatabaseConfigModule(new TestDatabaseConfigModule(t2Dir))
				.build();
		BriarIntegrationTestComponent.Helper.injectEagerSingletons(c2);
	}

	@Test
	public void testMembership() throws Exception {
		sendInvitation(contactId1From0, c0.getClock().currentTimeMillis(),
				"Hi!");

		// our group has only one member (ourselves)
		Collection<GroupMember> members = groupManager0.getMembers(groupId0);
		assertEquals(1, members.size());
		assertEquals(author0, members.iterator().next().getAuthor());
		assertEquals(OURSELVES,
				members.iterator().next().getAuthorInfo().getStatus());

		sync0To1(1, true);
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
		sync1To0(1, true);

		// sync group join messages
		sync0To1(2, true); // + one invitation protocol join message
		sync1To0(1, true);

		// now the group has two members
		members = groupManager0.getMembers(groupId0);
		assertEquals(2, members.size());
		for (GroupMember m : members) {
			if (m.getAuthorInfo().getStatus() == OURSELVES) {
				assertEquals(author0.getId(), m.getAuthor().getId());
			} else {
				assertEquals(author1.getId(), m.getAuthor().getId());
			}
		}

		members = groupManager1.getMembers(groupId0);
		assertEquals(2, members.size());
		for (GroupMember m : members) {
			if (m.getAuthorInfo().getStatus() == OURSELVES) {
				assertEquals(author1.getId(), m.getAuthor().getId());
			} else {
				assertEquals(author0.getId(), m.getAuthor().getId());
			}
		}
	}

	@Test
	public void testRevealContacts() throws Exception {
		// invite two contacts
		sendInvitation(contactId1From0, c0.getClock().currentTimeMillis(),
				"Hi 1!");
		sendInvitation(contactId2From0, c0.getClock().currentTimeMillis(),
				"Hi 2!");
		sync0To1(1, true);
		sync0To2(1, true);

		// accept both invitations
		groupInvitationManager1
				.respondToInvitation(contactId0From1, privateGroup0, true);
		groupInvitationManager2
				.respondToInvitation(contactId0From2, privateGroup0, true);
		sync1To0(1, true);
		sync2To0(1, true);

		// sync group join messages
		sync0To1(2, true); // + one invitation protocol join message
		assertEquals(2, groupManager1.getMembers(groupId0).size());
		sync1To0(1, true);
		assertEquals(2, groupManager0.getMembers(groupId0).size());
		sync0To2(3, true); // 2 join messages and 1 invite join message
		assertEquals(3, groupManager2.getMembers(groupId0).size());
		sync2To0(1, true);
		assertEquals(3, groupManager0.getMembers(groupId0).size());
		sync0To1(1, true);
		assertEquals(3, groupManager1.getMembers(groupId0).size());

		// 1 and 2 add each other as contacts
		addContacts1And2();

		// their relationship is still invisible
		assertEquals(INVISIBLE,
				getGroupMember(groupManager1, author2.getId()).getVisibility());
		assertEquals(INVISIBLE,
				getGroupMember(groupManager2, author1.getId()).getVisibility());

		// 1 reveals the contact relationship to 2
		assertNotNull(contactId2From1);
		groupInvitationManager1.revealRelationship(contactId2From1, groupId0);
		sync1To2(1, true); // 1 sends an invitation protocol join message
		// 2 sends an invitation protocol join message and three private group
		// protocol join messages, which 1 has already seen
		syncMessage(c2, c1, contactId1From2, 1, 3, 0, 1);

		// their relationship is now revealed
		assertEquals(REVEALED_BY_US,
				getGroupMember(groupManager1, author2.getId()).getVisibility());
		assertEquals(REVEALED_BY_CONTACT,
				getGroupMember(groupManager2, author1.getId()).getVisibility());

		// 2 sends a message to the group
		long time = c2.getClock().currentTimeMillis();
		String text = "This is a test message!";
		MessageId previousMsgId = groupManager2.getPreviousMsgId(groupId0);
		GroupMessage msg = groupMessageFactory
				.createGroupMessage(groupId0, time, null, author2, text,
						previousMsgId);
		groupManager2.addLocalMessage(msg);

		// 1 has only the three join messages in the group
		Collection<GroupMessageHeader> headers =
				groupManager1.getHeaders(groupId0);
		assertEquals(3, headers.size());

		// message should sync to 1 without creator (0) being involved
		sync2To1(1, true);
		headers = groupManager1.getHeaders(groupId0);
		assertEquals(4, headers.size());
		boolean foundPost = false;
		for (GroupMessageHeader h : headers) {
			if (h instanceof JoinMessageHeader) continue;
			foundPost = true;
			assertEquals(time, h.getTimestamp());
			assertEquals(groupId0, h.getGroupId());
			assertEquals(author2.getId(), h.getAuthor().getId());
		}
		assertTrue(foundPost);

		// message should sync from 1 to 0 without 2 being involved
		sync1To0(1, true);
		headers = groupManager0.getHeaders(groupId0);
		assertEquals(4, headers.size());
	}

	private void sendInvitation(ContactId c, long timestamp,
			@Nullable String text) throws DbException {
		Contact contact = contactManager0.getContact(c);
		byte[] signature = groupInvitationFactory
				.signInvitation(contact, groupId0, timestamp,
						author0.getPrivateKey());
		groupInvitationManager0.sendInvitation(groupId0, c, text, timestamp,
				signature, NO_AUTO_DELETE_TIMER);
	}

	private GroupMember getGroupMember(PrivateGroupManager groupManager,
			AuthorId a) throws DbException {
		Collection<GroupMember> members = groupManager.getMembers(groupId0);
		for (GroupMember m : members) {
			if (m.getAuthor().getId().equals(a)) return m;
		}
		throw new AssertionError();
	}

}
