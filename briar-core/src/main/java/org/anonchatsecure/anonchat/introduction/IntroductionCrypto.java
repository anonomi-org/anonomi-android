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

import org.anonchatsecure.bramble.api.crypto.KeyPair;
import org.anonchatsecure.bramble.api.crypto.PrivateKey;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.anonchat.api.client.SessionId;

import java.security.GeneralSecurityException;

interface IntroductionCrypto {

	/**
	 * Returns the {@link SessionId} based on the introducer
	 * and the two introducees.
	 */
	SessionId getSessionId(Author introducer, Author local, Author remote);

	/**
	 * Returns true if the local author is alice
	 * <p>
	 * Alice is the Author whose unique ID has the lower ID,
	 * comparing the IDs as byte strings.
	 */
	boolean isAlice(AuthorId local, AuthorId remote);

	/**
	 * Generates an agreement key pair.
	 */
	KeyPair generateAgreementKeyPair();

	/**
	 * Derives a session master key for Alice or Bob.
	 *
	 * @return The secret master key
	 */
	SecretKey deriveMasterKey(IntroduceeSession s)
			throws GeneralSecurityException;

	/**
	 * Derives a MAC key from the session's master key for Alice or Bob.
	 *
	 * @param masterKey The key returned by
	 * {@link #deriveMasterKey(IntroduceeSession)}
	 * @param alice true for Alice's MAC key, false for Bob's
	 * @return The MAC key
	 */
	SecretKey deriveMacKey(SecretKey masterKey, boolean alice);

	/**
	 * Generates a MAC that covers both introducee's ephemeral public keys,
	 * transport properties, Author IDs and timestamps of the accept message.
	 */
	byte[] authMac(SecretKey macKey, IntroduceeSession s,
			AuthorId localAuthorId);

	/**
	 * Verifies a received MAC
	 *
	 * @param mac The MAC to verify
	 * as returned by {@link #deriveMasterKey(IntroduceeSession)}
	 * @throws GeneralSecurityException if the verification fails
	 */
	void verifyAuthMac(byte[] mac, IntroduceeSession s, AuthorId localAuthorId)
			throws GeneralSecurityException;

	/**
	 * Signs a nonce derived from the macKey
	 * with the local introducee's identity private key.
	 *
	 * @param macKey The corresponding MAC key for the signer's role
	 * @param privateKey The identity private key
	 * (from {@link LocalAuthor#getPrivateKey()})
	 * @return The signature as a byte array
	 */
	byte[] sign(SecretKey macKey, PrivateKey privateKey)
			throws GeneralSecurityException;

	/**
	 * Verifies the signature on a nonce derived from the MAC key.
	 *
	 * @throws GeneralSecurityException if the signature is invalid
	 */
	void verifySignature(byte[] signature, IntroduceeSession s)
			throws GeneralSecurityException;

	/**
	 * Generates a MAC using the local MAC key.
	 */
	byte[] activateMac(IntroduceeSession s);

	/**
	 * Verifies a MAC from an ACTIVATE message.
	 *
	 * @throws GeneralSecurityException if the verification fails
	 */
	void verifyActivateMac(byte[] mac, IntroduceeSession s)
			throws GeneralSecurityException;

}
