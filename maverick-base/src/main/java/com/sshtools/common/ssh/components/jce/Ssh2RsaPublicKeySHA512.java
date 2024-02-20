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
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.components.SshRsaPublicKey;

public class Ssh2RsaPublicKeySHA512 extends Ssh2RsaPublicKey {
	
	private static final String ALGORITHM = "rsa-sha2-512";
	
	public static class Ssh2RsaPublicKeySHA512Factory implements SshPublicKeyFactory<Ssh2RsaPublicKeySHA512> {

		@Override
		public Ssh2RsaPublicKeySHA512 create() throws NoSuchAlgorithmException, IOException {
			return new Ssh2RsaPublicKeySHA512();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public Ssh2RsaPublicKeySHA512() {
		super();
	}

	public Ssh2RsaPublicKeySHA512(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(modulus, publicExponent);
	}

	public Ssh2RsaPublicKeySHA512(RSAPublicKey pubKey) {
		super(pubKey);
	}

	public Ssh2RsaPublicKeySHA512(SshRsaPublicKey publicKey) {
		this((RSAPublicKey)publicKey.getJCEPublicKey());
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.STRONG;
	}
	
	@Override
	public int getPriority() {
		return 2200;
	}

	@Override
	public String getSigningAlgorithm() {
		return "rsa-sha2-512";
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM;
	}
	
	public String getEncodingAlgorithm() {
		return "ssh-rsa";
	}
}
