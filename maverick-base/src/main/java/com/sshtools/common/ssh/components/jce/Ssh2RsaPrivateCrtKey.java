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
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;

import javax.crypto.Cipher;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshRsaPrivateCrtKey;

/**
 * RSA co-efficient private key implementation for SSH2 protocol. 
 * @author Lee David Painter
 *
 */
public class Ssh2RsaPrivateCrtKey implements SshRsaPrivateCrtKey {

	protected RSAPrivateCrtKey prv;

	public Ssh2RsaPrivateCrtKey(RSAPrivateCrtKey prv) {
		this.prv = prv;
	}
	
	public Ssh2RsaPrivateCrtKey(BigInteger modulus, BigInteger publicExponent,
			BigInteger privateExponent, BigInteger primeP, BigInteger primeQ,
			BigInteger primeExponentP, BigInteger primeExponentQ,
			BigInteger crtCoefficient) throws NoSuchAlgorithmException,
			InvalidKeySpecException {

		KeyFactory keyFactory = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_RSA)
				: KeyFactory.getInstance(JCEAlgorithms.JCE_RSA, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
		RSAPrivateCrtKeySpec spec = new RSAPrivateCrtKeySpec(modulus,
				publicExponent, privateExponent, primeP, primeQ,
				primeExponentP, primeExponentQ, crtCoefficient);
		prv = (RSAPrivateCrtKey) keyFactory.generatePrivate(spec);
	}

	public BigInteger doPrivate(BigInteger input) throws SshException {
		try {

			Cipher cipher = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING) == null ? Cipher
					.getInstance(JCEAlgorithms.JCE_RSANONEPKCS1PADDING) : Cipher.getInstance(
							JCEAlgorithms.JCE_RSANONEPKCS1PADDING, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING));

			cipher.init(Cipher.DECRYPT_MODE, prv, JCEProvider.getSecureRandom());

			return new BigInteger(cipher.doFinal(input.toByteArray()));
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}

	public BigInteger getCrtCoefficient() {
		return prv.getCrtCoefficient();
	}

	public BigInteger getPrimeExponentP() {
		return prv.getPrimeExponentP();
	}

	public BigInteger getPrimeExponentQ() {
		return prv.getPrimeExponentQ();
	}

	public BigInteger getPrimeP() {
		return prv.getPrimeP();
	}

	public BigInteger getPrimeQ() {
		return prv.getPrimeQ();
	}

	public BigInteger getPublicExponent() {
		return prv.getPublicExponent();
	}

	public BigInteger getModulus() {
		return prv.getModulus();
	}

	public BigInteger getPrivateExponent() {
		return prv.getPrivateExponent();
	}

	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	public byte[] sign(byte[] msg, String signingAlgorithm) throws IOException {
		
		switch(signingAlgorithm) {
		case "rsa-sha2-256":
			try {
				Signature l_sig = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA256WithRSA)== null ? Signature
						.getInstance(JCEAlgorithms.JCE_SHA256WithRSA)
						: Signature.getInstance(JCEAlgorithms.JCE_SHA256WithRSA, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA256WithRSA));
				l_sig.initSign(prv);
				l_sig.update(msg);
	
				return l_sig.sign();
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		case "rsa-sha2-512":
			try {
				Signature l_sig = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA512WithRSA)== null ? Signature
						.getInstance(JCEAlgorithms.JCE_SHA512WithRSA)
						: Signature.getInstance(JCEAlgorithms.JCE_SHA512WithRSA, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA512WithRSA));
				l_sig.initSign(prv);
				l_sig.update(msg);
	
				return l_sig.sign();
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		default:
			try {
				Signature l_sig = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA)== null ? Signature
						.getInstance(JCEAlgorithms.JCE_SHA1WithRSA)
						: Signature.getInstance(JCEAlgorithms.JCE_SHA1WithRSA, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA));
				l_sig.initSign(prv);
				l_sig.update(msg);
	
				return l_sig.sign();
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		}
	}

	public String getAlgorithm() {
		return "ssh-rsa";
	}

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
		if(obj instanceof Ssh2RsaPrivateCrtKey) {
			Ssh2RsaPrivateCrtKey other = (Ssh2RsaPrivateCrtKey)obj;
			if(other.prv!=null) {
				return other.prv.equals(prv);
			}
		}
		return false;
	}
}
