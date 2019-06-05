/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
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
				"RSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT), 10000);
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
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa1024"), "1234567890"), 10000);
	}
	
	public void testRsa2048bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890"), 10000);
	}
	
	public void testRsa4096bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa4096"), "1234567890"), 10000);
	}
	
	public void testRsa4048bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/rsa4048"), "1234567890"), 10000);
	}
	
	public void testRsa2048bitSHA256Signatures() throws IOException, InvalidPassphraseException, SshException {
		SshKeyPair pair = loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890");
		pair.setPublicKey(new Ssh2RsaPublicKeySHA256((SshRsaPublicKey)pair.getPublicKey()));
		testSignatures(pair, 10000);
	}
	
	public void testRsa2048bitSHA512Signatures() throws IOException, InvalidPassphraseException, SshException {
		SshKeyPair pair = loadKeyPair(getClass().getResourceAsStream("/openssh/rsa2048"), "1234567890");
		pair.setPublicKey(new Ssh2RsaPublicKeySHA512((SshRsaPublicKey)pair.getPublicKey()));
		testSignatures(pair, 10000);
	}
}
