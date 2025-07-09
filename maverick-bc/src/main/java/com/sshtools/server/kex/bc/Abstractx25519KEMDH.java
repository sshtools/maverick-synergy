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
package com.sshtools.server.kex.bc;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import org.bouncycastle.crypto.SecretWithEncapsulation;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.server.components.jce.DiffieHellmanEcdh;

public abstract class Abstractx25519KEMDH extends DiffieHellmanEcdh {

	protected SecretWithEncapsulation encaps;

	byte[] K_A;
	byte[] K_B;

	static final int KEY_SIZE = 32;
	static final byte[] KEY_HEADER = new byte[] { 0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00 };
	
	protected Abstractx25519KEMDH(String name, String hashAlgorithm, SecurityLevel level, int priority) {
		super(name, "X25519", hashAlgorithm, level, priority);
	}

	protected void initCrypto()
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		super.initCrypto();

		byte[] publicKeyInfo = keyPair.getPublic().getEncoded();
		K_B = Arrays.copyOfRange(publicKeyInfo, publicKeyInfo.length - KEY_SIZE, publicKeyInfo.length);
	}

	@Override
	protected byte[] encodeQS() {
		byte[] q_s = encaps.getEncapsulation();
		int l = q_s.length;
		q_s = Arrays.copyOf(q_s, l + K_B.length);
		System.arraycopy(K_B, 0, q_s, l, K_B.length);
		return q_s;
	}

	@Override
	protected String getKeyAgreementAlgorithm() {
		return JCEAlgorithms.JCE_X25519;
	}

	@Override
	protected String getKeyPairGeneratorAlgorithm() {
		return JCEAlgorithms.JCE_X25519;
	}

	@Override
	protected byte[] generateSecret() throws InvalidKeyException, IllegalStateException, InvalidKeySpecException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {

		keyAgreement.doPhase(encode(K_A), true);
		byte[] k = keyAgreement.generateSecret();

		try {
			Digest keyHash = (Digest) JCEComponentManager.getInstance().supportedDigests().getInstance(hashAlgorithm);
			keyHash.putBytes(encaps.getSecret());
			keyHash.putBytes(k);
			return keyHash.doFinal();
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private Key encode(byte[] key) throws InvalidKeySpecException, NoSuchAlgorithmException {

		int off = key.length - KEY_SIZE;

		if (off < 0 || off > 1) {
			throw new InvalidKeySpecException(getAlgorithm() + " key has wrong length! [" + key.length + " bytes]");
		} else if (off == 1) {
			if (key[0] != 0) {
				throw new InvalidKeySpecException(getAlgorithm()
						+ " key has additional non-zero byte! 0x" 
						+ Integer.toHexString(key[0] & 0xFF));
			}
		}

		byte[] encoded = Arrays.copyOf(KEY_HEADER, KEY_HEADER.length + KEY_SIZE);
		System.arraycopy(key, off, encoded, KEY_HEADER.length, KEY_SIZE);
		return KeyFactory.getInstance(getKeyPairGeneratorAlgorithm()).generatePublic(new X509EncodedKeySpec(encoded));
	}

}