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

@Ignore
public abstract class DsaPublicKeyTests extends AbstractPublicKeyTests {

	
	public void testDsaKeyGeneration1024bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_DSA, 1024, null, "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testDsaKeyGeneration2048bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_DSA, 2048, null, "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testDsaKeyGeneration1024bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_DSA, 1024, "1234567890", "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testDsaKeyGeneration2048bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.SSH2_DSA, 2048, "1234567890", "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testDsaPrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_dsa_key"), null, "SHA256:ypzMi59TWdQCxJcDVUMEHweMq93aZ/rOabXuAOFNyew");
	}
	
	public void testDsaPublicKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_dsa_key.pub"), "SHA256:ypzMi59TWdQCxJcDVUMEHweMq93aZ/rOabXuAOFNyew");
	}
	
	public void testDsaPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/dsa1024"), "1234567890", "SHA256:rzrq0epBt+SgjuYdaR/lFJFG343wz7YWdxEgc/gVY1U");
	}
	
	public void testDsaPrivateKeyFileWithIncorrectPassphrase() throws IOException, InvalidPassphraseException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/dsa1024"), "xxxxxxxx", "SHA256:rzrq0epBt+SgjuYdaR/lFJFG343wz7YWdxEgc/gVY1U");
			fail("Expected InvalidPassphraseException");
		} catch (InvalidPassphraseException e) {
		}
	}
	
	public void testDsaPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/dsa1024.pub"), "SHA256:rzrq0epBt+SgjuYdaR/lFJFG343wz7YWdxEgc/gVY1U");
	}

	public void testDsaSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/dsa1024"), "1234567890"), 10000);
	}
}
