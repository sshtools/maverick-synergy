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

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;

import junit.framework.TestCase;

@Ignore
public class AbstractOpenSSHKeyFileTests extends TestCase {


	protected void performPrivateKeyTests(String privateFilename, String publicFilename, String fingerprintFilename, String bbFilename, String encryptedFilename) throws IOException, InvalidPassphraseException, SshException {
		

		/**
		 * Load the public key file
		 */
		SshPublicKeyFile pubkeyFile = SshPublicKeyFileFactory.parse(
				getClass().getResourceAsStream(String.format("/testdata/%s", publicFilename)));
		
		SshPublicKey pub = pubkeyFile.toPublicKey();
		
		/**
		 * Load the unencrypted private key
		 */
		SshPrivateKeyFile unencryptedFile = SshPrivateKeyFileFactory.parse(
				getClass().getResourceAsStream(String.format("/testdata/%s", privateFilename)));
		SshKeyPair unencryptedKey = unencryptedFile.toKeyPair(null);
		
		/**
		 * Check the public key corresponds to the private key
		 */
		assertEquals(String.format("%s private key does not correspond to the public key file %s", privateFilename, publicFilename), 
				unencryptedKey.getPublicKey(), pub);
		
		/**
		 * Load the expected SHA256 fingerprint
		 */
		String expectedFingerprint = IOUtils.toString(getClass().getResourceAsStream(String.format("/testdata/%s", fingerprintFilename)), "UTF-8").trim();
		
		/**
		 * Generate the public key's SHA256 fingerprint
		 */
		String actualFingerprint = SshKeyUtils.getFingerprint(pub);
		
		/**
		 * Check the fingerprints are equal
		 */
		assertEquals(String.format("%s fingerprint %s does not match %s", publicFilename, actualFingerprint, expectedFingerprint), 
				expectedFingerprint, actualFingerprint);
		
		/**
		 * Load the expected BubbleBabble for the public key
		 */
		String expectedBubbleBabble = IOUtils.toString(getClass().getResourceAsStream(String.format("/testdata/%s", bbFilename)), "UTF-8").trim();
		
		/**
		 * Generate the public keys BubbleBabble
		 */
		String actualBubbleBabble = SshKeyUtils.getBubbleBabble(pub);
		
		/**
		 * Check the BubbleBabble output is correct
		 */
		assertEquals(String.format("%s bubblebabble %s does not match %s", publicFilename, expectedBubbleBabble, actualBubbleBabble), 
				expectedBubbleBabble, actualBubbleBabble);
	
		/**
		 * Load the passphrase for the encrypted private key
		 */
		String passphrase = IOUtils.toString(getClass().getResourceAsStream(String.format("/testdata/pw")), "UTF-8").trim();
		
		/**
		 * Load the encrypted private key
		 */
		SshPrivateKeyFile encryptedFile = SshPrivateKeyFileFactory.parse(
				getClass().getResourceAsStream(String.format("/testdata/%s", encryptedFilename)));
		
		SshKeyPair encryptedKey = encryptedFile.toKeyPair(passphrase);
		
		/**
		 * Check the encrypted private key corresponds to the public key
		 */
		assertEquals(String.format("%s private key does not correspond to the public key file %s", encryptedFilename, publicFilename), 
				encryptedKey.getPublicKey(), pub);


		/**
		 * Write the private key back to a temporary file through our own code
		 */
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.create(encryptedKey, passphrase);
		
		/**
		 * Reload so we can test it
		 */
		SshKeyPair reloadedPair = SshPrivateKeyFileFactory.parse(file.getFormattedKey()).toKeyPair(passphrase);
		
		/**
		 * Sign some data with the reloaded key and check it
		 */
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeString("Testing signature");
			byte[] data = baw.toByteArray();
			byte[] signature = reloadedPair.getPrivateKey().sign(data);
			assertTrue(String.format("Error trying to write private key %s and verify it reloaded correctly", encryptedFilename), encryptedKey.getPublicKey().verifySignature(signature, data));
		}	
	}
}
