package com.sshtools.common.ssh.x509;

/*-
 * #%L
 * X509 Certificate Support
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.Ssh2DsaPrivateKey;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPrivateKey;

public class X509Helper {

	/**
	 * Load a keystore for use as an SSH host key. This will create a public key
	 * using the X509V3_SIGN_RSA and X509V3_SIGN_RSA_SHA1 host key types. DSA
	 * keys are currently not supported.
	 * 
	 * @param keystoreFile
	 *            path to the keystore file
	 * @param alias
	 *            the alias of the key in the keystore
	 * @param storePassphrase
	 *            the passphrase of the keystore
	 * @param keyPassphrase
	 *            the passphrase of the key
	 * @throws IOException
	 */
	public static SshKeyPair[] loadKeystore(InputStream in, String alias, String storePassphrase, String keyPassphrase)
			throws IOException {
		return loadKeystore(in, alias, storePassphrase, keyPassphrase, "PKCS12");
	}

	/**
	 * Load a keystore for use as an SSH host key. This will create a public key
	 * using the X509V3_SIGN_RSA and X509V3_SIGN_RSA_SHA1 host key types. DSA
	 * keys are currently not supported.
	 * 
	 * @param keystoreFile
	 *            path to the keystore file
	 * @param alias
	 *            the alias of the key in the keystore
	 * @param storePassphrase
	 *            the passphrase of the keystore
	 * @param keyPassphrase
	 *            the passphrase of the key
	 * @throws IOException
	 */
	public static SshKeyPair[] loadKeystore(InputStream in, String alias, String storePassphrase, String keyPassphrase,
			String storeType) throws IOException {
		try {
			KeyStore keystore = KeyStore.getInstance(storeType);

			keystore.load(in, storePassphrase.toCharArray());

			Key prv = keystore.getKey(alias, keyPassphrase.toCharArray());

			X509Certificate x509 = (X509Certificate) keystore.getCertificate(alias);

			Certificate[] chain = keystore.getCertificateChain(alias);

			String algorithm = prv.getAlgorithm();

			SshKeyPair pair = new SshKeyPair();

			if (algorithm.equals("RSA")) {

				if (x509.getSigAlgName().equalsIgnoreCase("SHA1WithRSA")) {
					pair.setPublicKey(new SshX509RsaSha1PublicKey(x509));
					pair.setPrivateKey(new Ssh2RsaPrivateKey((RSAPrivateKey) prv));

					SshKeyPair pair2 = new SshKeyPair();
					pair2.setPublicKey(new SshX509RsaPublicKey(x509));
					pair2.setPrivateKey(new Ssh2RsaPrivateKey((RSAPrivateKey) prv));

					SshKeyPair pair3 = new SshKeyPair();
					pair3.setPublicKey(new SshX509RsaPublicKeyRfc6187(chain));
					pair3.setPrivateKey(new Ssh2RsaPrivateKey((RSAPrivateKey) prv));

					return new SshKeyPair[] { pair, pair2, pair3 };

				} else if (x509.getSigAlgName().equalsIgnoreCase("SHA256WithRSA")
						&& ((RSAPublicKey) x509.getPublicKey()).getModulus().bitLength() >= 2048) {

					pair.setPublicKey(new SshX509Rsa2048Sha256Rfc6187(chain));
					pair.setPrivateKey(new Ssh2RsaPrivateKey((RSAPrivateKey) prv));

					if (Boolean.getBoolean("maverick.backwardCompatibleSHA2")) {
						SshKeyPair pair2 = new SshKeyPair();
						pair2.setPublicKey(new SshX509RsaPublicKey(x509));
						pair2.setPrivateKey(new Ssh2RsaPrivateKey((RSAPrivateKey) prv));

						return new SshKeyPair[] { pair, pair2 };
					} else {
						return new SshKeyPair[] { pair };
					}
				}

			} else if (algorithm.equals("DSA")) {
				pair.setPublicKey(new SshX509DsaPublicKey(x509));
				pair.setPrivateKey(new Ssh2DsaPrivateKey((DSAPrivateKey) prv, (DSAPublicKey) x509.getPublicKey()));

				SshKeyPair pair2 = new SshKeyPair();
				pair2.setPublicKey(new SshX509DsaPublicKeyRfc6187(chain));
				pair2.setPrivateKey(new Ssh2DsaPrivateKey((DSAPrivateKey) prv));
				return new SshKeyPair[] { pair, pair2 };
			}

			throw new IOException(algorithm + " is an unsupported certificate type");
		} catch (Throwable ex) {
			throw new IOException("Could not load keystore from stream: " + ex.getMessage());
		}
	}

	public static SshKeyPair[] loadKeystore(File keystoreFile, String alias, String storePassphrase, String keyPassphrase)
			throws IOException {
		return loadKeystore(keystoreFile, alias, storePassphrase, keyPassphrase, "PKCS12");

	}

	public static SshKeyPair[] loadKeystore(File keystoreFile, String alias, String storePassphrase, String keyPassphrase,
			String storeType) throws IOException {
		return loadKeystore(new FileInputStream(keystoreFile), alias, storePassphrase, keyPassphrase, storeType);

	}
}
