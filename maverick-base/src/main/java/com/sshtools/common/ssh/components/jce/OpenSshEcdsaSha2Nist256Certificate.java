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

public class OpenSshEcdsaSha2Nist256Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp256-cert-v01@openssh.com";
	
	public static class OpenSshEcdsaSha2Nist256CertificateFactory implements SshPublicKeyFactory<OpenSshEcdsaSha2Nist256Certificate> {
		@Override
		public OpenSshEcdsaSha2Nist256Certificate create() throws NoSuchAlgorithmException, IOException {
			return new OpenSshEcdsaSha2Nist256Certificate();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}
	
	public OpenSshEcdsaSha2Nist256Certificate() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}
