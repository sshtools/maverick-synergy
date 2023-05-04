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
package com.sshtools.common.knownhosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.util.Arrays;
import java.util.Set;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshX509PublicKey;

/**
 * This is a {@link HostKeyVerification} implementation that will validate an X509 public key.
 * @author lee
 *
 */
public class X509HostKeyVerification implements HostKeyVerification {

	

	PKIXParameters params;

	/**
	 * This creates a verification instance that will check the validation of a certificate
	 * against the Java runtime's trusted CA certs keystore.
	 * @param enableRevocation
	 * @throws IOException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws InvalidAlgorithmParameterException
	 */
	public X509HostKeyVerification(boolean enableRevocation)
			throws IOException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, InvalidAlgorithmParameterException {
		String filename = System.getProperty("java.home")
				+ "/lib/security/cacerts".replace('/', File.separatorChar);
		FileInputStream is = new FileInputStream(filename);
		KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		String password = System.getProperty("maverick.trustedCACertsPassword", "changeit");
		keystore.load(is, password.toCharArray());
		params = new PKIXParameters(keystore);
		params.setRevocationEnabled(enableRevocation);
	}

	/**
	 * This creates a verification instance against a specific set of TrustAnchors
	 * @param trustAnchors
	 * @param enableRevocation
	 * @throws InvalidAlgorithmParameterException
	 */
	public X509HostKeyVerification(Set<TrustAnchor> trustAnchors,
			boolean enableRevocation) throws InvalidAlgorithmParameterException {
		params = new PKIXParameters(trustAnchors);
		params.setRevocationEnabled(enableRevocation);
	}

	public boolean verifyHost(String host, SshPublicKey pk) throws SshException {

		if (pk instanceof SshX509PublicKey) {
			SshX509PublicKey x509 = (SshX509PublicKey) pk;
			try {
				return validateChain(x509.getCertificateChain());
			} catch (Exception e) {
				Log.error("Failed to validate certificate chain", e);
			}
		}

		return false;
	}

	private boolean validateChain(Certificate[] certificates)
			throws CertificateException, NoSuchAlgorithmException,
			CertPathValidatorException, InvalidAlgorithmParameterException {

		CertPath certPath;
		CertPathValidator certPathValidator;
		Boolean valid = Boolean.FALSE;

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		certPath = cf.generateCertPath(Arrays.asList(certificates));
		certPathValidator = CertPathValidator.getInstance("PKIX");

		PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) certPathValidator
				.validate(certPath, params);

		if (null != result) {
			valid = Boolean.TRUE;
		}
		return valid;
	}

}
