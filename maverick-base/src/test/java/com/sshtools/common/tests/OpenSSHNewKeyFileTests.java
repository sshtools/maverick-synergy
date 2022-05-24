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
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;

public class OpenSSHNewKeyFileTests extends AbstractOpenSSHKeyFileTests {

	public void testEd25519Keys() throws IOException, InvalidPassphraseException, SshException {
		
		JCEProvider.enableBouncyCastle(false);
		ComponentManager.reset();
		
		performPrivateKeyTests("openssh/new/ed25519", "ed25519-nopass.key", "ed25519.key.pub", 
				"ed25519.fp", "ed25519.bb", "ed25519.key");
		
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	public void testEd448Keys() throws IOException, InvalidPassphraseException, SshException {
		
		JCEProvider.enableBouncyCastle(false);
		ComponentManager.reset();
		
		performPrivateKeyTests("openssh/new/ed448", "ed448-nopass.key", "ed448.pub", 
				"ed448.fp", "ed448.bb", "ed448-withpass.key");
		
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	public void testEcdsa() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/new/ecdsa/256", "ecdsa-nopass.key", "ecdsa.pub", 
				"ecdsa.fp", "ecdsa.bb", "ecdsa-withpass.key");
	}

	public void testRsa2048BitsKeys() throws IOException, InvalidPassphraseException, SshException {
		performPrivateKeyTests("openssh/new/rsa/2048", "rsa-nopass.key", "rsa.key.pub", 
				"rsa.fp", "rsa.bb", "rsa.key");
	}

}
