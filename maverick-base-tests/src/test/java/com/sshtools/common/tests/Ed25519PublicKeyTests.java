/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.tests;

import java.io.IOException;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshException;

public abstract class Ed25519PublicKeyTests extends AbstractPublicKeyTests {

	@Override
	protected boolean isJCETested() {
		return true;
	}
	
	public void testEd25519PrivateKeyFileNoPassphraseOpenSSH7_9() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ed25519_7.9"), null, "SHA256:c48rEsAUF8dYuQnCFvEQ0qV+syBSOMaCQsvhdxcMW9M");
	}
	
	public void testEd25519PrivateKeyFileWithPassphraseOpenSSH7_9() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ed25519_7.9_encrypted"), "1234567890", "SHA256:c48rEsAUF8dYuQnCFvEQ0qV+syBSOMaCQsvhdxcMW9M");
	}
	
	public void testEd25519KeySaveOpenSSH7_9() throws IOException, InvalidPassphraseException, SshException {
		
		SshPrivateKeyFile unecrypted = SshPrivateKeyFileFactory.parse(getClass().getResourceAsStream("/openssh/ed25519_7.9"));
		assertFalse(unecrypted.isPassphraseProtected());
		
		SshPrivateKeyFile encrypted = SshPrivateKeyFileFactory.create(unecrypted.toKeyPair(""), "1234567890", "", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
		assertTrue(encrypted.isPassphraseProtected());
		
		SshPrivateKeyFile parsed = SshPrivateKeyFileFactory.parse(encrypted.getFormattedKey());
		assertTrue(parsed.isPassphraseProtected());
		
		assertEquals(unecrypted.toKeyPair("").getPublicKey(), parsed.toKeyPair("1234567890").getPublicKey());
		assertEquals(unecrypted.toKeyPair("").getPrivateKey(), parsed.toKeyPair("1234567890").getPrivateKey());
		
	}
	
	public void testEd25519PrivateKeyOpenSSH7_9Signatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/ed25519_7.9"), null), 10000);
	}
		
	public void testEd25519PrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/id_ed25519"), null, "SHA256:BRlz+o+dFFHkpKQ6I9P/cEoM/QQu1iPJX3xS3AaxEDc");
	}
	
	public void testEd2x5519PrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/id_ed25519-encrypted"), "1234567890", "SHA256:BRlz+o+dFFHkpKQ6I9P/cEoM/QQu1iPJX3xS3AaxEDc");
	}

	public void testEd25519Signatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(testKeyGeneration("ed25519", 0), 10000);
	}
	
	public void testEd25519KeySave() throws IOException, InvalidPassphraseException {
		
		SshPrivateKeyFile unecrypted = SshPrivateKeyFileFactory.parse(getClass().getResourceAsStream("/openssh/id_ed25519"));
		assertFalse(unecrypted.isPassphraseProtected());
		
		SshPrivateKeyFile encrypted = SshPrivateKeyFileFactory.create(unecrypted.toKeyPair(""), "1234567890", "", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
		assertTrue(encrypted.isPassphraseProtected());
		
		SshPrivateKeyFile parsed = SshPrivateKeyFileFactory.parse(encrypted.getFormattedKey());
		assertTrue(parsed.isPassphraseProtected());
		
		assertEquals(unecrypted.toKeyPair("").getPublicKey(), parsed.toKeyPair("1234567890").getPublicKey());
		assertEquals(unecrypted.toKeyPair("").getPrivateKey(), parsed.toKeyPair("1234567890").getPrivateKey());
		
	}

	public void testEd25519KeyGenerationNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ED25519, 256, null, "ED25519 public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testEd25519KeyGenerationWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ED25519, 256, "1234567890", "ED25519 public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
}

