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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.client.kex.bc;

import java.util.Arrays;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeKEMExtractor;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeKeyPairGenerator;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeParameters;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimePrivateKeyParameters;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimePublicKeyParameters;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.synergy.ssh.SshContext;

public class Sntrup761X25519SHA512DH extends Abstractx25519KEMDH {

	public Sntrup761X25519SHA512DH() {
		super(SshContext.KEX_SNTRUP761_25519_SHA512, JCEAlgorithms.JCE_SHA512, SecurityLevel.PARANOID, 0);
	}
	
	public Sntrup761X25519SHA512DH(String algorithmName) {
		super(algorithmName, JCEAlgorithms.JCE_SHA512, SecurityLevel.PARANOID, 0);	}

	@Override
	protected byte[] encodeQC() {
		
		SNTRUPrimeKeyPairGenerator gen = new SNTRUPrimeKeyPairGenerator();
        gen.init(new SNTRUPrimeKeyGenerationParameters(JCEComponentManager.getSecureRandom(), SNTRUPrimeParameters.sntrup761));
        AsymmetricCipherKeyPair pair = gen.generateKeyPair();
        extractor = new SNTRUPrimeKEMExtractor((SNTRUPrimePrivateKeyParameters) pair.getPrivate());
        SNTRUPrimePublicKeyParameters publicKey = (SNTRUPrimePublicKeyParameters) pair.getPublic();

		byte[] q_c = publicKey.getEncoded();
		int l = q_c.length;
		q_c = Arrays.copyOf(q_c, l + K_A.length);
		System.arraycopy(K_A, 0, q_c, l, K_A.length);
		return q_c;
	}

}