/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

public class OpenSSHRSACertificateAuthorityTests extends AbstractCertificateAuthorityTests {

	@Override
	protected String getTestingJCE() {
		return "";
	}
	
	protected boolean isJCETested() {
		return false;
	}

	public void testCertificateGenerations() throws IOException, SshException, InvalidPassphraseException {
		testCertificateGenerations(SshKeyPairGenerator.SSH2_RSA, 2048, "1234567890");
	}
	
	public void testHostSigningCAPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testHostSigningCAPrivateKey("/ca/rsa/ca_host_key", "bluemars73", "SHA256:mLet3p6Fwp9oEYWaLFumO9bXppgyOTc/rV28EVRp0EA");
	}
	
	public void testHostSigningCAPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testHostSigningCAPublicKey("/ca/rsa/ca_host_key.pub", "SHA256:mLet3p6Fwp9oEYWaLFumO9bXppgyOTc/rV28EVRp0EA");
	}
	
	public void testUnencryptedHostPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUnencryptedHostPrivateKey("/ca/rsa/host_key", "SHA256:zIv7akRq0Xcb+xyG/ygH/zW4uKxGPQTCAPW24XgNLzw");
	}
	
	public void testHostPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testHostPublicKey("/ca/rsa/host_key.pub", "SHA256:zIv7akRq0Xcb+xyG/ygH/zW4uKxGPQTCAPW24XgNLzw");
	}
	
	public void testHostCertificate() throws IOException, InvalidPassphraseException, SshException {
		testHostCertificate("/ca/rsa/host_key-cert.pub", "SHA256:zIv7akRq0Xcb+xyG/ygH/zW4uKxGPQTCAPW24XgNLzw",
				"/ca/rsa/ca_host_key.pub", "SHA256:mLet3p6Fwp9oEYWaLFumO9bXppgyOTc/rV28EVRp0EA");
	}
	
	public void testUserSigningCAPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUserSigningCAPrivateKey("/ca/rsa/ca_user_key", "bluemars73", "SHA256:/uoU5wUQxMFApk1smxF3YUSrN/UhognhjLW/xncSe3Y");
	}
	
	public void testUserSigningCAPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testUserSigningCAPublicKey("/ca/rsa/ca_user_key.pub", "SHA256:/uoU5wUQxMFApk1smxF3YUSrN/UhognhjLW/xncSe3Y");
	}
	
	public void testUsersPrivateKey() throws IOException, InvalidPassphraseException, SshException {
		testUsersPrivateKey("/ca/rsa/user_key", "bluemars73", "SHA256:0uRVlDe6QHkmPevpKnGpwrmvk9psssd3hyfUn7/fcgU");
	}
	
	public void testUsersPublicKey() throws IOException, InvalidPassphraseException, SshException {
		testUsersPublicKey("/ca/rsa/user_key.pub", "SHA256:0uRVlDe6QHkmPevpKnGpwrmvk9psssd3hyfUn7/fcgU");
	}
	
	public void testUserCertificate() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/rsa/user_key-cert.pub", "SHA256:0uRVlDe6QHkmPevpKnGpwrmvk9psssd3hyfUn7/fcgU",
				"/ca/rsa/ca_user_key.pub", "SHA256:/uoU5wUQxMFApk1smxF3YUSrN/UhognhjLW/xncSe3Y");
	}
	
	public void testCertificateWithExtensionsGenerations() throws IOException, SshException, InvalidPassphraseException, InterruptedException {
		testCertificateGenerationsWithExtensions(SshKeyPairGenerator.SSH2_RSA, 2048, "1234567890");
	}
	
	public void testUserCertificateWithExtensions() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/rsa/user_extended-cert.pub", "SHA256:Kp6Mr3y8X/5evibLlkQ6IDs+hOby+GRa8N9JCpFnpTA",
				"/ca/rsa/ca_user_key.pub", "SHA256:/uoU5wUQxMFApk1smxF3YUSrN/UhognhjLW/xncSe3Y");
	}

	public void testUserCertificateWithMultipleExtensions() throws IOException, InvalidPassphraseException, SshException {
		testUserCertificate("/ca/rsa/user_multiple_extended-cert.pub", "SHA256:dPx4oA/fCA7toDTOEuUFsa8IXVQOZzsFBQ8cdZEkuXA",
				"/ca/rsa/ca_user_key.pub", "SHA256:/uoU5wUQxMFApk1smxF3YUSrN/UhognhjLW/xncSe3Y");
	}
}
