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
package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class SshX509EcdsaSha2Nist256Rfc6187 extends
		SshX509EcdsaSha2NistPublicKeyRfc6187 {

	public SshX509EcdsaSha2Nist256Rfc6187(ECPublicKey pk) throws IOException {
		super(pk, "secp256r1");
	}

	public SshX509EcdsaSha2Nist256Rfc6187() {
		super("ecdsa-sha2-nistp256", JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1", "nistp256");
	}

	public SshX509EcdsaSha2Nist256Rfc6187(Certificate[] chain)
			throws IOException {
		super(chain, "secp256r1");
	}

	@Override
	public String getAlgorithm() {
		return "x509v3-ecdsa-sha2-nistp256";
	}

}
