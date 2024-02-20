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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;

import com.sshtools.common.ssh.components.SshRsaPrivateKey;

/**
 * RSA private key implementation for the SSH2 protocol.
 *
 * @author Lee David Painter
 *
 */
public class Ssh2RsaPrivateKey extends Ssh2BaseRsaPrivateKey implements SshRsaPrivateKey {

	public Ssh2RsaPrivateKey(RSAPrivateKey prv) {
		super(prv);
	}

	public Ssh2RsaPrivateKey(BigInteger modulus, BigInteger privateExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(null);
		KeyFactory keyFactory = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_RSA) : KeyFactory.getInstance(JCEAlgorithms.JCE_RSA,
				JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
		RSAPrivateKeySpec spec = new RSAPrivateKeySpec(modulus, privateExponent);
		this.prv = (RSAPrivateKey) keyFactory.generatePrivate(spec);

	}

	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		return super.doSign(data, signingAlgorithm);
	}

	public String getAlgorithm() {
		return "ssh-rsa";
	}

	public BigInteger getModulus() {
		return ((RSAPrivateKey)prv).getModulus();
	}

	public BigInteger getPrivateExponent() {
		return ((RSAPrivateKey)prv).getPrivateExponent();
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return prv;
	}

	@Override
	public int hashCode() {
		return prv.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		}
		if(obj instanceof Ssh2RsaPrivateKey) {
			Ssh2RsaPrivateKey other = (Ssh2RsaPrivateKey)obj;
			if(other.prv!=null) {
				return other.prv.equals(prv);
			}
		}
		return false;
	}
}
