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
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;

public class OpenSSHED25519CertificateAuthorityTests extends AbstractCertificateAuthorityTests {

	public void setUp() {
		JCEProvider.enableBouncyCastle(true);
		ComponentManager.reset();
	}
	
	public void tearDown() {
		JCEProvider.disableBouncyCastle();
		ComponentManager.reset();
	}
	
	@Override
	protected String getTestingJCE() {
		return "BC";
	}
	
	protected boolean isJCETested() {
		return false;
	}

	public void testCertificateGenerations() throws IOException, SshException, InvalidPassphraseException {
		testCertificateGenerations(SshKeyPairGenerator.ED25519, 256, "1234567890");
	}
	
	public void testHostSigningCAPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testHostSigningCAPrivateKey("/ca/ed25519/ca_host_key", "bluemars73", "SHA256:KhD74LZJIdXrryLx79o/Z8f/eSqkpkwLHUW9UTVBIhU");
	}
	
	public void testHostSigningCAPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testHostSigningCAPublicKey("/ca/ed25519/ca_host_key.pub", "SHA256:KhD74LZJIdXrryLx79o/Z8f/eSqkpkwLHUW9UTVBIhU");
	}
	
	public void testUnencryptedHostPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUnencryptedHostPrivateKey("/ca/ed25519/host_key", "SHA256:EcSfGTIEJ56+VgUgkEghiBWqrauOzXbVrvJO0ZfA4TM");
	}
	
	public void testHostPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testHostPublicKey("/ca/ed25519/host_key.pub", "SHA256:EcSfGTIEJ56+VgUgkEghiBWqrauOzXbVrvJO0ZfA4TM");
	}
	
	public void testHostCertificate() throws IOException, InvalidPassphraseException, SshException {
		testHostCertificate("/ca/ed25519/host_key-cert.pub", "SHA256:EcSfGTIEJ56+VgUgkEghiBWqrauOzXbVrvJO0ZfA4TM",
				"/ca/ed25519/ca_host_key.pub", "SHA256:KhD74LZJIdXrryLx79o/Z8f/eSqkpkwLHUW9UTVBIhU");
	}
	
	public void testUserSigningCAPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUserSigningCAPrivateKey("/ca/ed25519/ca_user_key", "bluemars73", "SHA256:LxJm5a7faXz9qaP9y22oVUKYOYRk5t4rbvtE32FqXNQ");
	}
	
	public void testUserSigningCAPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testUserSigningCAPublicKey("/ca/ed25519/ca_user_key.pub", "SHA256:LxJm5a7faXz9qaP9y22oVUKYOYRk5t4rbvtE32FqXNQ");
	}
	
	public void testUsersPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUsersPrivateKey("/ca/ed25519/user_key", "bluemars73", "SHA256:oHunfxOZDv1T3MzgmeqrvpZflPamshGRgKoaVXxBLdw");
	}
	
	public void testUsersPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testUsersPublicKey("/ca/ed25519/user_key.pub", "SHA256:oHunfxOZDv1T3MzgmeqrvpZflPamshGRgKoaVXxBLdw");
	}
	
	public void testUserCertificate() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/ed25519/user_key-cert.pub", "SHA256:oHunfxOZDv1T3MzgmeqrvpZflPamshGRgKoaVXxBLdw",
				"/ca/ed25519/ca_user_key.pub", "SHA256:LxJm5a7faXz9qaP9y22oVUKYOYRk5t4rbvtE32FqXNQ");
	}

	public void testCertificateWithExtensionsGenerations() throws IOException, SshException, InvalidPassphraseException, InterruptedException {
		testCertificateGenerationsWithExtensions(SshKeyPairGenerator.ED25519, 256, "1234567890");
	}
	
	public void testUserCertificateWithExtensions() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/ed25519/user_extended-cert.pub", "SHA256:FOSynqWp2ZXIopv9Q2jiLIoed4I0dY5+ZxYFMUSG+FQ",
				"/ca/ed25519/ca_user_key.pub", "SHA256:LxJm5a7faXz9qaP9y22oVUKYOYRk5t4rbvtE32FqXNQ");
	}

	public void testUserCertificateWithMultipleExtensions() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/ed25519/user_multiple_extended-cert.pub", "SHA256:j68iPgCzgCqsDkRyitYa1FCS951Dun04xTZlwJqm3Cg",
				"/ca/ed25519/ca_user_key.pub", "SHA256:LxJm5a7faXz9qaP9y22oVUKYOYRk5t4rbvtE32FqXNQ");
	}
}
