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
package com.sshtools.common.ssh.x509;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class SshX509EcdsaSha2Nist384Rfc6187 extends
		SshX509EcdsaSha2NistPublicKeyRfc6187 {

	private static final String ALGORITHM = "x509v3-ecdsa-sha2-nistp384";
	
	public static class SshX509EcdsaSha2Nist384Rfc6187Factory implements SshPublicKeyFactory<SshX509EcdsaSha2Nist384Rfc6187> {

		@Override
		public SshX509EcdsaSha2Nist384Rfc6187 create() throws NoSuchAlgorithmException, IOException {
			return new SshX509EcdsaSha2Nist384Rfc6187();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  ALGORITHM };
		}
	}

	public SshX509EcdsaSha2Nist384Rfc6187(ECPublicKey pk) throws IOException {
		super(pk, "secp384r1");
	}

	public SshX509EcdsaSha2Nist384Rfc6187() {
		super("ecdsa-sha2-nistp384", JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1", "nistp384");
	}

	public SshX509EcdsaSha2Nist384Rfc6187(Certificate[] chain)
			throws IOException {
		super(chain, "secp384r1");
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}

}
