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
import org.anonchatsecure.bramble.api.client.ContactGroupFactory;
import org.anonchatsecure.bramble.api.contact.Contact;
import org.anonchatsecure.bramble.api.crypto.PrivateKey;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationFactory;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager.CLIENT_ID;
import static org.anonchatsecure.anonchat.api.privategroup.invitation.GroupInvitationManager.MAJOR_VERSION;

@Immutable
@NotNullByDefault
class GroupInvitationFactoryImpl implements GroupInvitationFactory {

	private final ContactGroupFactory contactGroupFactory;
	private final ClientHelper clientHelper;

	@Inject
	GroupInvitationFactoryImpl(ContactGroupFactory contactGroupFactory,
			ClientHelper clientHelper) {
		this.contactGroupFactory = contactGroupFactory;
		this.clientHelper = clientHelper;
	}

	@Override
	public byte[] signInvitation(Contact c, GroupId privateGroupId,
			long timestamp, PrivateKey privateKey) {
		AuthorId creatorId = c.getLocalAuthorId();
		AuthorId memberId = c.getAuthor().getId();
		BdfList token = createInviteToken(creatorId, memberId, privateGroupId,
				timestamp);
		try {
			return clientHelper.sign(SIGNING_LABEL_INVITE, token, privateKey);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public BdfList createInviteToken(AuthorId creatorId, AuthorId memberId,
			GroupId privateGroupId, long timestamp) {
		Group contactGroup = contactGroupFactory.createContactGroup(CLIENT_ID,
				MAJOR_VERSION, creatorId, memberId);
		return BdfList.of(
				timestamp,
				contactGroup.getId(),
				privateGroupId
		);
	}

}
