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

import org.junit.Ignore;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;

@Ignore
public abstract class RsaPublicKeyTests extends AbstractPublicKeyTests {
	
	public void testRsaKeyGeneration1024bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 1024, null, "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testRsaKeyNonStandard2016bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 2016, null, 
				"RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT), 1000);
	}
	
	public void testRsaKeyGeneration2048bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 2048, null, "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testRsaKeyGeneration4096bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 4096, null, "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testRsaKeyGeneration4048bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 4048, null, "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testRsaKeyGeneration1024bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 1024, "1234567890", "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testRsaKeyGeneration2048bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 2048, "1234567890", "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testRsaKeyGeneration4096bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_RSA, 4096, "1234567890", "RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testRsaPrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_rsa_key"), null, "SHA256:ME3i02AmOyzEgovDCypZeF/Lm3OF4IPqH10TG6SkLPc");
	}
	
	public void testRsaPublicKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_rsa_key.pub"), "SHA256:ME3i02AmOyzEgovDCypZeF/Lm3OF4IPqH10TG6SkLPc");
	}
	
	public void testRsa1024bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa1024"), "1234567890", "SHA256:6LrVwJc1Zd8aNvm5UPCkeYeryVCHznOfnr+pv6Nem8s");
	}
	
	public void testRsa1024bitPrivateKeyFileWithInvalidPassphrase() throws IOException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa1024"), "xxxxxxxx", "SHA256:6LrVwJc1Zd8aNvm5UPCkeYeryVCHznOfnr+pv6Nem8s");
			fail();
		} catch(InvalidPassphraseException e) {
			return;
		}
	}
	
	public void testRsa1024bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/rsa1024.pub"), "SHA256:6LrVwJc1Zd8aNvm5UPCkeYeryVCHznOfnr+pv6Nem8s");
	}
	
	public void testRsa2048bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890", "SHA256:FycmXoyuAIkW5iAbuFSRHqEeE6+kJUhddFwbz+UZVfI");
	}
	
	public void testRsa2048bitPrivateKeyFileWithInvalidPassphrase() throws IOException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa2048"), "xxxxxxxxx", "SHA256:FycmXoyuAIkW5iAbuFSRHqEeE6+kJUhddFwbz+UZVfI");
			fail();
		} catch(InvalidPassphraseException e) {
			return;
		}
	}
	
	public void testRsa2048bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/rsa2048.pub"), "SHA256:FycmXoyuAIkW5iAbuFSRHqEeE6+kJUhddFwbz+UZVfI");
	}
	
	public void testRsa4096bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa4096"), "1234567890", "SHA256:T3MrsOw5J3RR2QTaAqASniJ/R+EFER+v5MMMuK5PwQY");
	}
	
	public void testRsa4096bitPrivateKeyFileWithInvalidPassphrase() throws IOException, InvalidPassphraseException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/rsa4096"), "xxxxxxxxx", "SHA256:T3MrsOw5J3RR2QTaAqASniJ/R+EFER+v5MMMuK5PwQY");
			fail();
		} catch(InvalidPassphraseException e) {
			return;
		}
	}
	
	public void testRsa4096bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/rsa4096.pub"), "SHA256:T3MrsOw5J3RR2QTaAqASniJ/R+EFER+v5MMMuK5PwQY");
	}

	public void testRsa1024bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa1024"), "1234567890"), 1000);
	}
	
	public void testRsa2048bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890"), 1000);
	}
	
	public void testRsa4096bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa4096"), "1234567890"), 1000);
	}
	
	public void testRsa4048bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa4048"), "1234567890"), 1000);
	}
	
	public void testRsa2048bitSHA256Signatures() throws IOException, InvalidPassphraseException, SshException {
		SshKeyPair pair = loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890");
		pair.setPublicKey(new Ssh2RsaPublicKeySHA256((SshRsaPublicKey)pair.getPublicKey()));
		testSignatures(pair, 1000);
	}
	
	public void testRsa2048bitSHA512Signatures() throws IOException, InvalidPassphraseException, SshException {
		SshKeyPair pair = loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890");
		pair.setPublicKey(new Ssh2RsaPublicKeySHA512((SshRsaPublicKey)pair.getPublicKey()));
		testSignatures(pair, 1000);
	}
}
