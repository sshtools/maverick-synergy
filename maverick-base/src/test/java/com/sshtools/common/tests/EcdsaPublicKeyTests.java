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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.tests;

import java.io.IOException;

import org.junit.Ignore;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshException;

@Ignore
public abstract class EcdsaPublicKeyTests extends AbstractPublicKeyTests {


	public void testEcdsaKeyGeneration256bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 256, null, "ECDSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testEcdsaKeyGeneration384bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 384, null, "ECDSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testEcdsaKeyGeneration521bitsNoPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 521, null, "ECDSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testEcdsaKeyGeneration256bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 256, "1234567890", "ECDSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testEcdsaKeyGeneration384bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 384, "1234567890", "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}
	
	public void testEcdsaKeyGeneration521bitsWithPassphrase() throws IOException, SshException, InvalidPassphraseException {
		testKeyGeneration(SshKeyPairGenerator.ECDSA, 521, "1234567890", "DSA public key tests", SshPrivateKeyFileFactory.OPENSSH_FORMAT);
	}

	public void testEcdsa256bitPrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_256"), null, "SHA256:3aDGq4aYKMkqCWOYuYzfwtjyiMEwXim2xBmVfq5DJWg");
	}
	
	public void testEcdsa256bitPublicKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_256.pub"), "SHA256:3aDGq4aYKMkqCWOYuYzfwtjyiMEwXim2xBmVfq5DJWg");
	}
	
	public void testEcdsa384bitPrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_384"), null, "SHA256:qo0R7H0yD7VCIBfgjvhkNCN0RXn9DJ742D/I/OrvlZ0");
	}
	
	public void testEcdsa384bitPublicKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_384.pub"), "SHA256:qo0R7H0yD7VCIBfgjvhkNCN0RXn9DJ742D/I/OrvlZ0");
	}
	
	public void testEcdsa521bitPrivateKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_521"), null, "SHA256:/6urcvSIzsuwWGEYbFlcvhBvxzMPfboHi6QAgBhsnOY");
	}
	
	public void testEcdsa521bitPublicKeyFileNoPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ssh_host_key_ecdsa_521.pub"), "SHA256:/6urcvSIzsuwWGEYbFlcvhBvxzMPfboHi6QAgBhsnOY");
	}
	
	public void testEcdsa256bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa256"), "1234567890", "SHA256:sowglxgZDVmev8CUZ2EK6WG+gDQKwaWw2E7Gy2+qZCU");
	}
	
	public void testEcdsa256bitPrivateKeyFileWithInvalidPassphrase() throws IOException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa256"), "xxxxxxxx", "SHA256:sowglxgZDVmev8CUZ2EK6WG+gDQKwaWw2E7Gy2+qZCU");
			fail("Expected InvalidPassphraseException");
		} catch (InvalidPassphraseException e) {
		}
	}
	
	public void testEcdsa256bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ecdsa256.pub"), "SHA256:sowglxgZDVmev8CUZ2EK6WG+gDQKwaWw2E7Gy2+qZCU");
	}
	
	public void testEcdsa384bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa384"), "1234567890", "SHA256:jiyrrPxndYdOHfNAArH6V8ScPZqr0jUQ88eZZrsCqd0");
	}
	
	public void testEcdsa384bitPrivateKeyFileWithInvalidPassphrase() throws IOException, InvalidPassphraseException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa384"), "xxxxxxxx", "SHA256:jiyrrPxndYdOHfNAArH6V8ScPZqr0jUQ88eZZrsCqd0");
			fail("Expected InvalidPassphraseException");
		} catch (InvalidPassphraseException e) {
		}
	}
	
	public void testEcdsa384bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ecdsa384.pub"), "SHA256:jiyrrPxndYdOHfNAArH6V8ScPZqr0jUQ88eZZrsCqd0");
	}
	
	public void testEcdsa521bitPrivateKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa521"), "1234567890", "SHA256:tXfvYApivOB6Redm5hZKjE3O0/T3lyva0PqoX/ip9uc");
	}
	
	public void testEcdsa521bitPrivateKeyFileWithInvalidPassphrase() throws IOException, InvalidPassphraseException, SshException {
		try {
			testPrivateKeyFile(getClass().getResourceAsStream("/openssh/ecdsa521"), "xxxxxxxxxxx", "SHA256:tXfvYApivOB6Redm5hZKjE3O0/T3lyva0PqoX/ip9uc");
			fail("Expected InvalidPassphraseException");
		} catch (InvalidPassphraseException e) {
		}
	}
	
	public void testEcdsa521bitPublicKeyFileWithPassphrase() throws IOException, InvalidPassphraseException, SshException {
		testPublicKeyFile(getClass().getResourceAsStream("/openssh/ecdsa521.pub"), "SHA256:tXfvYApivOB6Redm5hZKjE3O0/T3lyva0PqoX/ip9uc");
	}

	public void testEcdsa256bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/ecdsa256"), "1234567890"), 1000);
	}
	
	public void testEcdsa384bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/ecdsa384"), "1234567890"), 1000);
	}
	
	public void testEcdsa521bitSignatures() throws IOException, SshException, InvalidPassphraseException {
		testSignatures(loadKeyPair(getClass().getResourceAsStream("/openssh/ecdsa521"), "1234567890"), 1000);
	}
	
}
