/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.ssh.components.jce;

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
