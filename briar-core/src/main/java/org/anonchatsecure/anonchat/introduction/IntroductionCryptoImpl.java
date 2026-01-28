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

package org.anonchatsecure.anonchat.introduction;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.KeyPair;
import org.anonchatsecure.bramble.api.crypto.PrivateKey;
import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.anonchatsecure.anonchat.introduction.IntroduceeSession.Common;
import org.anonchatsecure.anonchat.introduction.IntroduceeSession.Remote;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_ACTIVATE_MAC;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_ALICE_MAC_KEY;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_AUTH_MAC;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_AUTH_NONCE;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_AUTH_SIGN;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_BOB_MAC_KEY;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_MASTER_KEY;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionConstants.LABEL_SESSION_ID;
import static org.anonchatsecure.anonchat.api.introduction.IntroductionManager.MAJOR_VERSION;
import static org.anonchatsecure.anonchat.introduction.IntroduceeSession.Local;

@Immutable
@NotNullByDefault
class IntroductionCryptoImpl implements IntroductionCrypto {

	private final CryptoComponent crypto;
	private final ClientHelper clientHelper;

	@Inject
	IntroductionCryptoImpl(
			CryptoComponent crypto,
			ClientHelper clientHelper) {
		this.crypto = crypto;
		this.clientHelper = clientHelper;
	}

	@Override
	public SessionId getSessionId(Author introducer, Author local,
			Author remote) {
		boolean isAlice = isAlice(local.getId(), remote.getId());
		byte[] hash = crypto.hash(
				LABEL_SESSION_ID,
				introducer.getId().getBytes(),
				isAlice ? local.getId().getBytes() : remote.getId().getBytes(),
				isAlice ? remote.getId().getBytes() : local.getId().getBytes()
		);
		return new SessionId(hash);
	}

	@Override
	public KeyPair generateAgreementKeyPair() {
		return crypto.generateAgreementKeyPair();
	}

	@Override
	public boolean isAlice(AuthorId local, AuthorId remote) {
		return local.compareTo(remote) < 0;
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public SecretKey deriveMasterKey(IntroduceeSession s)
			throws GeneralSecurityException {
		return deriveMasterKey(
				s.getLocal().ephemeralPublicKey,
				s.getLocal().ephemeralPrivateKey,
				s.getRemote().ephemeralPublicKey,
				s.getLocal().alice
		);
	}

	SecretKey deriveMasterKey(PublicKey publicKey, PrivateKey privateKey,
			PublicKey remotePublicKey, boolean alice)
			throws GeneralSecurityException {
		KeyPair keyPair = new KeyPair(publicKey, privateKey);
		return crypto.deriveSharedSecret(
				LABEL_MASTER_KEY,
				remotePublicKey,
				keyPair,
				new byte[] {MAJOR_VERSION},
				alice ? publicKey.getEncoded() : remotePublicKey.getEncoded(),
				alice ? remotePublicKey.getEncoded() : publicKey.getEncoded()
		);
	}

	@Override
	public SecretKey deriveMacKey(SecretKey masterKey, boolean alice) {
		return crypto.deriveKey(
				alice ? LABEL_ALICE_MAC_KEY : LABEL_BOB_MAC_KEY,
				masterKey
		);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public byte[] authMac(SecretKey macKey, IntroduceeSession s,
			AuthorId localAuthorId) {
		// the macKey is not yet available in the session at this point
		return authMac(macKey, s.getIntroducer().getId(), localAuthorId,
				s.getLocal(), s.getRemote());
	}

	byte[] authMac(SecretKey macKey, AuthorId introducerId,
			AuthorId localAuthorId, Local local, Remote remote) {
		byte[] inputs = getAuthMacInputs(introducerId, localAuthorId, local,
				remote.author.getId(), remote);
		return crypto.mac(
				LABEL_AUTH_MAC,
				macKey,
				inputs
		);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void verifyAuthMac(byte[] mac, IntroduceeSession s,
			AuthorId localAuthorId) throws GeneralSecurityException {
		verifyAuthMac(mac, new SecretKey(s.getRemote().macKey),
				s.getIntroducer().getId(), localAuthorId, s.getLocal(),
				s.getRemote().author.getId(), s.getRemote());
	}

	void verifyAuthMac(byte[] mac, SecretKey macKey, AuthorId introducerId,
			AuthorId localAuthorId, Common local, AuthorId remoteAuthorId,
			Common remote) throws GeneralSecurityException {
		// switch input for verification
		byte[] inputs = getAuthMacInputs(introducerId, remoteAuthorId, remote,
				localAuthorId, local);
		if (!crypto.verifyMac(mac, LABEL_AUTH_MAC, macKey, inputs)) {
			throw new GeneralSecurityException();
		}
	}

	@SuppressWarnings("ConstantConditions")
	private byte[] getAuthMacInputs(AuthorId introducerId,
			AuthorId localAuthorId, Common local, AuthorId remoteAuthorId,
			Common remote) {
		BdfList localInfo = BdfList.of(
				localAuthorId,
				local.acceptTimestamp,
				local.ephemeralPublicKey,
				clientHelper.toDictionary(local.transportProperties)
		);
		BdfList remoteInfo = BdfList.of(
				remoteAuthorId,
				remote.acceptTimestamp,
				remote.ephemeralPublicKey,
				clientHelper.toDictionary(remote.transportProperties)
		);
		BdfList macList = BdfList.of(
				introducerId,
				localInfo,
				remoteInfo
		);
		try {
			return clientHelper.toByteArray(macList);
		} catch (FormatException e) {
			throw new AssertionError();
		}
	}

	@Override
	public byte[] sign(SecretKey macKey, PrivateKey privateKey)
			throws GeneralSecurityException {
		return crypto.sign(LABEL_AUTH_SIGN, getNonce(macKey), privateKey);
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void verifySignature(byte[] signature, IntroduceeSession s)
			throws GeneralSecurityException {
		SecretKey macKey = new SecretKey(s.getRemote().macKey);
		verifySignature(macKey, s.getRemote().author.getPublicKey(), signature);
	}

	void verifySignature(SecretKey macKey, PublicKey publicKey,
			byte[] signature) throws GeneralSecurityException {
		byte[] nonce = getNonce(macKey);
		if (!crypto.verifySignature(signature, LABEL_AUTH_SIGN, nonce,
				publicKey)) {
			throw new GeneralSecurityException();
		}
	}

	private byte[] getNonce(SecretKey macKey) {
		return crypto.mac(LABEL_AUTH_NONCE, macKey);
	}

	@Override
	public byte[] activateMac(IntroduceeSession s) {
		if (s.getLocal().macKey == null)
			throw new AssertionError("Local MAC key is null");
		return activateMac(new SecretKey(s.getLocal().macKey));
	}

	byte[] activateMac(SecretKey macKey) {
		return crypto.mac(
				LABEL_ACTIVATE_MAC,
				macKey
		);
	}

	@Override
	public void verifyActivateMac(byte[] mac, IntroduceeSession s)
			throws GeneralSecurityException {
		if (s.getRemote().macKey == null)
			throw new AssertionError("Remote MAC key is null");
		verifyActivateMac(mac, new SecretKey(s.getRemote().macKey));
	}

	void verifyActivateMac(byte[] mac, SecretKey macKey)
			throws GeneralSecurityException {
		if (!crypto.verifyMac(mac, LABEL_ACTIVATE_MAC, macKey)) {
			throw new GeneralSecurityException();
		}
	}

}
