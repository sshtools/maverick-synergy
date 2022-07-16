/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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

