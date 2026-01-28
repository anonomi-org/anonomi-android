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

package org.anonchatsecure.anonchat.privategroup.invitation;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.client.ProtocolStateException;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroup;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.event.GroupInvitationRequestReceivedEvent;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationRequest;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.anonchatsecure.bramble.api.sync.Group.Visibility.INVISIBLE;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.SHARED;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.VISIBLE;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.ACCEPTED;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.DISSOLVED;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.ERROR;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.INVITED;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.JOINED;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.LEFT;
import static org.anonchatsecure.anonchat.privategroup.invitation.InviteeState.START;

@Immutable
@NotNullByDefault
class InviteeProtocolEngine extends AbstractProtocolEngine<InviteeSession> {

	InviteeProtocolEngine(
			DatabaseComponent db,
			ClientHelper clientHelper,
			ClientVersioningManager clientVersioningManager,
			PrivateGroupManager privateGroupManager,
			PrivateGroupFactory privateGroupFactory,
			GroupMessageFactory groupMessageFactory,
			IdentityManager identityManager,
			MessageParser messageParser,
			MessageEncoder messageEncoder,
			AutoDeleteManager autoDeleteManager,
			ConversationManager conversationManager,
			Clock clock) {
		super(db, clientHelper, clientVersioningManager, privateGroupManager,
				privateGroupFactory, groupMessageFactory, identityManager,
				messageParser, messageEncoder,
				autoDeleteManager, conversationManager, clock);
	}

	@Override
	public InviteeSession onInviteAction(Transaction txn, InviteeSession s,
			@Nullable String text, long timestamp, byte[] signature,
			long autoDeleteTimer) {
		throw new UnsupportedOperationException(); // Invalid in this role
	}

	@Override
	public InviteeSession onJoinAction(Transaction txn, InviteeSession s)
			throws DbException {
		switch (s.getState()) {
			case START:
			case ACCEPTED:
			case JOINED:
			case LEFT:
			case DISSOLVED:
			case ERROR:
				throw new ProtocolStateException(); // Invalid in these states
			case INVITED:
				return onLocalAccept(txn, s);
			default:
				throw new AssertionError();
		}
	}

	@Override
	public InviteeSession onLeaveAction(Transaction txn, InviteeSession s,
			boolean isAutoDecline) throws DbException {
		switch (s.getState()) {
			case START:
			case LEFT:
			case DISSOLVED:
			case ERROR:
				return s; // Ignored in these states
			case INVITED:
				return onLocalDecline(txn, s, isAutoDecline);
			case ACCEPTED:
			case JOINED:
				return onLocalLeave(txn, s);
			default:
				throw new AssertionError();
		}
	}

	@Override
	public InviteeSession onMemberAddedAction(Transaction txn,
			InviteeSession s) {
		return s; // Ignored in this role
	}

	@Override
	public InviteeSession onInviteMessage(Transaction txn, InviteeSession s,
			InviteMessage m) throws DbException, FormatException {
		switch (s.getState()) {
			case START:
				return onRemoteInvite(txn, s, m);
			case INVITED:
			case ACCEPTED:
			case JOINED:
			case LEFT:
			case DISSOLVED:
				return abort(txn, s); // Invalid in these states
			case ERROR:
				return s; // Ignored in this state
			default:
				throw new AssertionError();
		}
	}

	@Override
	public InviteeSession onJoinMessage(Transaction txn, InviteeSession s,
			JoinMessage m) throws DbException, FormatException {
		switch (s.getState()) {
			case START:
			case INVITED:
			case JOINED:
			case LEFT:
			case DISSOLVED:
				return abort(txn, s); // Invalid in these states
			case ACCEPTED:
				return onRemoteJoin(txn, s, m);
			case ERROR:
				return s; // Ignored in this state
			default:
				throw new AssertionError();
		}
	}

	@Override
	public InviteeSession onLeaveMessage(Transaction txn, InviteeSession s,
			LeaveMessage m) throws DbException, FormatException {
		switch (s.getState()) {
			case START:
			case DISSOLVED:
				return abort(txn, s); // Invalid in these states
			case INVITED:
			case LEFT:
				return onRemoteLeaveWhenNotSubscribed(txn, s, m);
			case ACCEPTED:
			case JOINED:
				return onRemoteLeaveWhenSubscribed(txn, s, m);
			case ERROR:
				return s; // Ignored in this state
			default:
				throw new AssertionError();
		}
	}

	@Override
	public InviteeSession onAbortMessage(Transaction txn, InviteeSession s,
			AbortMessage m) throws DbException, FormatException {
		return abort(txn, s);
	}

	private InviteeSession onLocalAccept(Transaction txn, InviteeSession s)
			throws DbException {
		// Mark the invite message unavailable to answer
		MessageId inviteId = s.getLastRemoteMessageId();
		if (inviteId == null) throw new IllegalStateException();
		markMessageAvailableToAnswer(txn, inviteId, false);
		// Record the response
		markInviteAccepted(txn, inviteId);
		// Send a JOIN message
		Message sent = sendJoinMessage(txn, s, true);
		// Track the message
		conversationManager.trackOutgoingMessage(txn, sent);
		try {
			// Subscribe to the private group
			subscribeToPrivateGroup(txn, inviteId);
			// Make the private group visible to the contact
			setPrivateGroupVisibility(txn, s, VISIBLE);
		} catch (FormatException e) {
			throw new DbException(e); // Invalid group metadata
		}
		// Move to the ACCEPTED state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), ACCEPTED);
	}

	private InviteeSession onLocalDecline(Transaction txn, InviteeSession s,
			boolean isAutoDecline) throws DbException {
		// Mark the invite message unavailable to answer
		MessageId inviteId = s.getLastRemoteMessageId();
		if (inviteId == null) throw new IllegalStateException();
		markMessageAvailableToAnswer(txn, inviteId, false);
		// Send a LEAVE message
		Message sent = sendLeaveMessage(txn, s, true, isAutoDecline);
		// Track the message
		conversationManager.trackOutgoingMessage(txn, sent);
		// Move to the START state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), START);
	}

	private InviteeSession onLocalLeave(Transaction txn, InviteeSession s)
			throws DbException {
		// Send a LEAVE message
		Message sent = sendLeaveMessage(txn, s);
		try {
			// Make the private group invisible to the contact
			setPrivateGroupVisibility(txn, s, INVISIBLE);
		} catch (FormatException e) {
			throw new DbException(e); // Invalid group metadata
		}
		// Move to the LEFT state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), LEFT);
	}

	private InviteeSession onRemoteInvite(Transaction txn, InviteeSession s,
			InviteMessage m) throws DbException, FormatException {
		// The timestamp must be higher than the last invite message, if any
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// Check that the contact is the creator
		ContactId contactId =
				clientHelper.getContactId(txn, s.getContactGroupId());
		Author contact = db.getContact(txn, contactId).getAuthor();
		if (!contact.getId().equals(m.getCreator().getId()))
			return abort(txn, s);
		// Mark the invite message visible in the UI and available to answer
		markMessageVisibleInUi(txn, m.getId());
		markMessageAvailableToAnswer(txn, m.getId(), true);
		// Track the message
		conversationManager.trackMessage(txn, m.getContactGroupId(),
				m.getTimestamp(), false);
		// Receive the auto-delete timer
		receiveAutoDeleteTimer(txn, m);
		// Broadcast an event
		PrivateGroup privateGroup = privateGroupFactory.createPrivateGroup(
				m.getGroupName(), m.getCreator(), m.getSalt());
		txn.attach(new GroupInvitationRequestReceivedEvent(
				createInvitationRequest(m, privateGroup), contactId));
		// Move to the INVITED state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				m.getTimestamp(), INVITED);
	}

	private InviteeSession onRemoteJoin(Transaction txn, InviteeSession s,
			JoinMessage m) throws DbException, FormatException {
		// The timestamp must be higher than the last invite message, if any
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		try {
			// Share the private group with the contact
			setPrivateGroupVisibility(txn, s, SHARED);
		} catch (FormatException e) {
			throw new DbException(e); // Invalid group metadata
		}
		// Move to the JOINED state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				s.getInviteTimestamp(), JOINED);
	}

	private InviteeSession onRemoteLeaveWhenNotSubscribed(Transaction txn,
			InviteeSession s, LeaveMessage m)
			throws DbException, FormatException {
		// The timestamp must be higher than the last invite message, if any
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		// Mark any invite messages in the session unavailable to answer
		markInvitesUnavailableToAnswer(txn, s);
		// Move to the DISSOLVED state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				s.getInviteTimestamp(), DISSOLVED);
	}

	private InviteeSession onRemoteLeaveWhenSubscribed(Transaction txn,
			InviteeSession s, LeaveMessage m)
			throws DbException, FormatException {
		// The timestamp must be higher than the last invite message, if any
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		try {
			// Make the private group invisible to the contact
			setPrivateGroupVisibility(txn, s, INVISIBLE);
		} catch (FormatException e) {
			throw new DbException(e); // Invalid group metadata
		}
		// Mark the group dissolved
		privateGroupManager.markGroupDissolved(txn, s.getPrivateGroupId());
		// Move to the DISSOLVED state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				s.getInviteTimestamp(), DISSOLVED);
	}

	private InviteeSession abort(Transaction txn, InviteeSession s)
			throws DbException, FormatException {
		// If the session has already been aborted, do nothing
		if (s.getState() == ERROR) return s;
		// Mark any invite messages in the session unavailable to answer
		markInvitesUnavailableToAnswer(txn, s);
		// If we subscribe, make the private group invisible to the contact
		if (isSubscribedPrivateGroup(txn, s.getPrivateGroupId()))
			setPrivateGroupVisibility(txn, s, INVISIBLE);
		// Send an ABORT message
		Message sent = sendAbortMessage(txn, s);
		// Move to the ERROR state
		return new InviteeSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), ERROR);
	}

	private GroupInvitationRequest createInvitationRequest(InviteMessage m,
			PrivateGroup pg) {
		SessionId sessionId = new SessionId(m.getPrivateGroupId().getBytes());
		return new GroupInvitationRequest(m.getId(), m.getContactGroupId(),
				m.getTimestamp(), false, false, false, false, sessionId, pg,
				m.getText(), true, false, m.getAutoDeleteTimer());
	}

}
