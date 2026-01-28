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
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.versioning.ClientVersioningManager;
import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.anonchat.api.client.ProtocolStateException;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.api.conversation.ConversationManager;
import org.anonchatsecure.anonchat.api.privategroup.GroupMessageFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupFactory;
import org.anonchatsecure.anonchat.api.privategroup.PrivateGroupManager;
import org.anonchatsecure.anonchat.api.privategroup.event.GroupInvitationResponseReceivedEvent;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationResponse;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.lang.Math.max;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.INVISIBLE;
import static org.anonchatsecure.bramble.api.sync.Group.Visibility.SHARED;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.DISSOLVED;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.ERROR;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.INVITED;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.JOINED;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.LEFT;
import static org.anonchatsecure.anonchat.privategroup.invitation.CreatorState.START;

@Immutable
@NotNullByDefault
class CreatorProtocolEngine extends AbstractProtocolEngine<CreatorSession> {

	CreatorProtocolEngine(
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
	public CreatorSession onInviteAction(Transaction txn, CreatorSession s,
			@Nullable String text, long timestamp, byte[] signature,
			long autoDeleteTimer) throws DbException {
		switch (s.getState()) {
			case START:
				return onLocalInvite(txn, s, text, timestamp, signature,
						autoDeleteTimer);
			case INVITED:
			case JOINED:
			case LEFT:
			case DISSOLVED:
			case ERROR:
				throw new ProtocolStateException(); // Invalid in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public CreatorSession onJoinAction(Transaction txn, CreatorSession s) {
		throw new UnsupportedOperationException(); // Invalid in this role
	}

	@Override
	public CreatorSession onLeaveAction(Transaction txn, CreatorSession s,
			boolean isAutoDecline) throws DbException {
		switch (s.getState()) {
			case START:
			case DISSOLVED:
			case ERROR:
				return s; // Ignored in these states
			case INVITED:
			case JOINED:
			case LEFT:
				return onLocalLeave(txn, s);
			default:
				throw new AssertionError();
		}
	}

	@Override
	public CreatorSession onMemberAddedAction(Transaction txn,
			CreatorSession s) {
		return s; // Ignored in this role
	}

	@Override
	public CreatorSession onInviteMessage(Transaction txn, CreatorSession s,
			InviteMessage m) throws DbException, FormatException {
		return abort(txn, s); // Invalid in this role
	}

	@Override
	public CreatorSession onJoinMessage(Transaction txn, CreatorSession s,
			JoinMessage m) throws DbException, FormatException {
		switch (s.getState()) {
			case START:
			case JOINED:
			case LEFT:
				return abort(txn, s); // Invalid in these states
			case INVITED:
				return onRemoteAccept(txn, s, m);
			case DISSOLVED:
			case ERROR:
				return s; // Ignored in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public CreatorSession onLeaveMessage(Transaction txn, CreatorSession s,
			LeaveMessage m) throws DbException, FormatException {
		switch (s.getState()) {
			case START:
			case LEFT:
				return abort(txn, s); // Invalid in these states
			case INVITED:
				return onRemoteDecline(txn, s, m);
			case JOINED:
				return onRemoteLeave(txn, s, m);
			case DISSOLVED:
			case ERROR:
				return s; // Ignored in these states
			default:
				throw new AssertionError();
		}
	}

	@Override
	public CreatorSession onAbortMessage(Transaction txn, CreatorSession s,
			AbortMessage m) throws DbException, FormatException {
		return abort(txn, s);
	}

	private CreatorSession onLocalInvite(Transaction txn, CreatorSession s,
			@Nullable String text, long timestamp, byte[] signature,
			long autoDeleteTimer) throws DbException {
		// Send an INVITE message
		Message sent = sendInviteMessage(txn, s, text, timestamp, signature,
				autoDeleteTimer);
		// Track the message
		conversationManager.trackOutgoingMessage(txn, sent);
		// Move to the INVITED state
		long localTimestamp =
				max(timestamp, getTimestampForVisibleMessage(txn, s));
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), localTimestamp,
				timestamp, INVITED);
	}

	private CreatorSession onLocalLeave(Transaction txn, CreatorSession s)
			throws DbException {
		try {
			// Make the private group invisible to the contact
			setPrivateGroupVisibility(txn, s, INVISIBLE);
		} catch (FormatException e) {
			throw new DbException(e); // Invalid group metadata
		}
		// Send a LEAVE message
		Message sent = sendLeaveMessage(txn, s);
		// Move to the DISSOLVED state
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), DISSOLVED);
	}

	private CreatorSession onRemoteAccept(Transaction txn, CreatorSession s,
			JoinMessage m) throws DbException, FormatException {
		// The timestamp must be higher than the last invite message
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		// Send a JOIN message
		Message sent = sendJoinMessage(txn, s, false);
		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getId());
		// Track the message
		conversationManager.trackMessage(txn, m.getContactGroupId(),
				m.getTimestamp(), false);
		// Receive the auto-delete timer
		receiveAutoDeleteTimer(txn, m);
		// Share the private group with the contact
		setPrivateGroupVisibility(txn, s, SHARED);
		// Broadcast an event
		ContactId contactId =
				clientHelper.getContactId(txn, m.getContactGroupId());
		txn.attach(new GroupInvitationResponseReceivedEvent(
				createInvitationResponse(m, true), contactId));
		// Move to the JOINED state
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), m.getId(), sent.getTimestamp(),
				s.getInviteTimestamp(), JOINED);
	}

	private CreatorSession onRemoteDecline(Transaction txn, CreatorSession s,
			LeaveMessage m) throws DbException, FormatException {
		// The timestamp must be higher than the last invite message
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		// Mark the response visible in the UI
		markMessageVisibleInUi(txn, m.getId());
		// Track the message
		conversationManager.trackMessage(txn, m.getContactGroupId(),
				m.getTimestamp(), false);
		// Receive the auto-delete timer
		receiveAutoDeleteTimer(txn, m);
		// Broadcast an event
		ContactId contactId =
				clientHelper.getContactId(txn, m.getContactGroupId());
		txn.attach(new GroupInvitationResponseReceivedEvent(
				createInvitationResponse(m, false), contactId));
		// Move to the START state
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				s.getInviteTimestamp(), START);
	}

	private CreatorSession onRemoteLeave(Transaction txn, CreatorSession s,
			LeaveMessage m) throws DbException, FormatException {
		// The timestamp must be higher than the last invite message
		if (m.getTimestamp() <= s.getInviteTimestamp()) return abort(txn, s);
		// The dependency, if any, must be the last remote message
		if (!isValidDependency(s, m.getPreviousMessageId()))
			return abort(txn, s);
		// Make the private group invisible to the contact
		setPrivateGroupVisibility(txn, s, INVISIBLE);
		// Move to the LEFT state
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				s.getLastLocalMessageId(), m.getId(), s.getLocalTimestamp(),
				s.getInviteTimestamp(), LEFT);
	}

	private CreatorSession abort(Transaction txn, CreatorSession s)
			throws DbException, FormatException {
		// If the session has already been aborted, do nothing
		if (s.getState() == ERROR) return s;
		// If we subscribe, make the private group invisible to the contact
		if (isSubscribedPrivateGroup(txn, s.getPrivateGroupId()))
			setPrivateGroupVisibility(txn, s, INVISIBLE);
		// Send an ABORT message
		Message sent = sendAbortMessage(txn, s);
		// Move to the ERROR state
		return new CreatorSession(s.getContactGroupId(), s.getPrivateGroupId(),
				sent.getId(), s.getLastRemoteMessageId(), sent.getTimestamp(),
				s.getInviteTimestamp(), ERROR);
	}

	private GroupInvitationResponse createInvitationResponse(
			DeletableGroupInvitationMessage m, boolean accept) {
		SessionId sessionId = new SessionId(m.getPrivateGroupId().getBytes());
		return new GroupInvitationResponse(m.getId(), m.getContactGroupId(),
				m.getTimestamp(), false, false, false, false, sessionId,
				accept, m.getPrivateGroupId(), m.getAutoDeleteTimer(), false);
	}
}
