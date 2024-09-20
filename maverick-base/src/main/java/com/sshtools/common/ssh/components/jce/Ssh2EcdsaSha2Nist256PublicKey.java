package com.sshtools.common.ssh.components.jce;

/*-
 * #%L
 * Base API
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class Ssh2EcdsaSha2Nist256PublicKey extends Ssh2EcdsaSha2NistPublicKey {
	private static final String CERT_TYPE = "ecdsa-sha2-nistp256";
	
	public static class Ssh2EcdsaSha2Nist256PublicKeyFactory implements SshPublicKeyFactory<Ssh2EcdsaSha2Nist256PublicKey> {

		@Override
		public Ssh2EcdsaSha2Nist256PublicKey create() throws NoSuchAlgorithmException, IOException {
			return new Ssh2EcdsaSha2Nist256PublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}

	public Ssh2EcdsaSha2Nist256PublicKey() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1", "nistp256");
	}
	
	public byte[] getOid() {
		return new byte[] { 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07};
	}
}
