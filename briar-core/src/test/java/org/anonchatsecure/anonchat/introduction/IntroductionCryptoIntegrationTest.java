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

import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.KeyPair;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorFactory;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.anonchatsecure.bramble.api.properties.TransportProperties;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.anonchatsecure.anonchat.api.client.SessionId;
import org.junit.Test;

import java.util.Map;

import javax.inject.Inject;

import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.anonchatsecure.bramble.test.TestUtils.getTransportPropertiesMap;
import static org.anonchatsecure.anonchat.introduction.IntroduceeSession.Local;
import static org.anonchatsecure.anonchat.introduction.IntroduceeSession.Remote;
import static org.anonchatsecure.anonchat.test.BriarTestUtils.getRealAuthor;
import static org.anonchatsecure.anonchat.test.BriarTestUtils.getRealLocalAuthor;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class IntroductionCryptoIntegrationTest extends BrambleTestCase {

	@Inject
	ClientHelper clientHelper;
	@Inject
	AuthorFactory authorFactory;
	@Inject
	CryptoComponent cryptoComponent;

	private final IntroductionCryptoImpl crypto;

	private final Author introducer;
	private final LocalAuthor alice, bob;
	private final long aliceAcceptTimestamp = 42L;
	private final long bobAcceptTimestamp = 1337L;
	private final SecretKey masterKey = getSecretKey();
	private final KeyPair aliceEphemeral, bobEphemeral;
	private final Map<TransportId, TransportProperties> aliceTransport =
			getTransportPropertiesMap(3);
	private final Map<TransportId, TransportProperties> bobTransport =
			getTransportPropertiesMap(3);

	public IntroductionCryptoIntegrationTest() {
		IntroductionIntegrationTestComponent component =
				DaggerIntroductionIntegrationTestComponent.builder().build();
		IntroductionIntegrationTestComponent.Helper
				.injectEagerSingletons(component);
		component.inject(this);
		crypto = new IntroductionCryptoImpl(cryptoComponent, clientHelper);

		introducer = getRealAuthor(authorFactory);
		LocalAuthor introducee1 = getRealLocalAuthor(authorFactory);
		LocalAuthor introducee2 = getRealLocalAuthor(authorFactory);
		boolean isAlice =
				crypto.isAlice(introducee1.getId(), introducee2.getId());
		alice = isAlice ? introducee1 : introducee2;
		bob = isAlice ? introducee2 : introducee1;
		aliceEphemeral = crypto.generateAgreementKeyPair();
		bobEphemeral = crypto.generateAgreementKeyPair();
	}

	@Test
	public void testGetSessionId() {
		SessionId s1 = crypto.getSessionId(introducer, alice, bob);
		SessionId s2 = crypto.getSessionId(introducer, bob, alice);
		assertEquals(s1, s2);

		SessionId s3 = crypto.getSessionId(alice, bob, introducer);
		assertNotEquals(s1, s3);
	}

	@Test
	public void testIsAlice() {
		assertTrue(crypto.isAlice(alice.getId(), bob.getId()));
		assertFalse(crypto.isAlice(bob.getId(), alice.getId()));
	}

	@Test
	public void testDeriveMasterKey() throws Exception {
		SecretKey aliceMasterKey = crypto.deriveMasterKey(
				aliceEphemeral.getPublic(), aliceEphemeral.getPrivate(),
				bobEphemeral.getPublic(), true);
		SecretKey bobMasterKey = crypto.deriveMasterKey(
				bobEphemeral.getPublic(), bobEphemeral.getPrivate(),
				aliceEphemeral.getPublic(), false);
		assertArrayEquals(aliceMasterKey.getBytes(), bobMasterKey.getBytes());
	}

	@Test
	public void testAliceAuthMac() throws Exception {
		SecretKey aliceMacKey = crypto.deriveMacKey(masterKey, true);
		Local local = new Local(true, null, -1, aliceEphemeral.getPublic(),
				aliceEphemeral.getPrivate(), aliceTransport,
				aliceAcceptTimestamp, aliceMacKey.getBytes());
		Remote remote = new Remote(false, bob, null, bobEphemeral.getPublic(),
				bobTransport, bobAcceptTimestamp, null);
		byte[] aliceMac = crypto.authMac(aliceMacKey, introducer.getId(),
				alice.getId(), local, remote);

		// verify from Bob's perspective
		crypto.verifyAuthMac(aliceMac, aliceMacKey, introducer.getId(),
				bob.getId(), remote, alice.getId(), local);
	}

	@Test
	public void testBobAuthMac() throws Exception {
		SecretKey bobMacKey = crypto.deriveMacKey(masterKey, false);
		Local local = new Local(false, null, -1, bobEphemeral.getPublic(),
				bobEphemeral.getPrivate(), bobTransport,
				bobAcceptTimestamp, bobMacKey.getBytes());
		Remote remote = new Remote(true, alice, null,
				aliceEphemeral.getPublic(), aliceTransport,
				aliceAcceptTimestamp, null);
		byte[] bobMac = crypto.authMac(bobMacKey, introducer.getId(),
				bob.getId(), local, remote);

		// verify from Alice's perspective
		crypto.verifyAuthMac(bobMac, bobMacKey, introducer.getId(),
				alice.getId(), remote, bob.getId(), local);
	}

	@Test
	public void testSign() throws Exception {
		SecretKey macKey = crypto.deriveMacKey(masterKey, true);
		byte[] signature = crypto.sign(macKey, alice.getPrivateKey());
		crypto.verifySignature(macKey, alice.getPublicKey(), signature);
	}

	@Test
	public void testAliceActivateMac() throws Exception {
		SecretKey aliceMacKey = crypto.deriveMacKey(masterKey, true);
		byte[] aliceMac = crypto.activateMac(aliceMacKey);
		crypto.verifyActivateMac(aliceMac, aliceMacKey);
	}

	@Test
	public void testBobActivateMac() throws Exception {
		SecretKey bobMacKey = crypto.deriveMacKey(masterKey, false);
		byte[] bobMac = crypto.activateMac(bobMacKey);
		crypto.verifyActivateMac(bobMac, bobMacKey);
	}
}
