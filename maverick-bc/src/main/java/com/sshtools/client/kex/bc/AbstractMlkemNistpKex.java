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

/*-
 * #%L
 * BouncyCastle JCE Support
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

import com.sshtools.client.components.DiffieHellmanEcdh;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

public abstract class AbstractMlkemNistpKex extends DiffieHellmanEcdh {

	private final MLKEMParameters kemParameters;
	private final int kemPublicKeyBytes;
	private final String ecCurve;

	protected MLKEMExtractor extractor;
	protected byte[] ecPublicClient;
	protected byte[] ecPublicServer;

	protected AbstractMlkemNistpKex(String algorithmName, String ecCurve, String hashAlgorithm,
			SecurityLevel securityLevel, int priority, MLKEMParameters kemParameters, int kemPublicKeyBytes) {
		super(algorithmName, ecCurve, hashAlgorithm, securityLevel, priority);
		this.ecCurve = ecCurve;
		this.kemParameters = kemParameters;
		this.kemPublicKeyBytes = kemPublicKeyBytes;
	}

	@Override
	protected byte[] encodeQC() {
		ecPublicClient = super.encodeQC();

		MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
		generator.init(new MLKEMKeyGenerationParameters(JCEComponentManager.getSecureRandom(), kemParameters));
		AsymmetricCipherKeyPair pair = generator.generateKeyPair();

		extractor = new MLKEMExtractor((MLKEMPrivateKeyParameters) pair.getPrivate());
		MLKEMPublicKeyParameters publicKey = (MLKEMPublicKeyParameters) pair.getPublic();

		byte[] kemPublic = publicKey.getEncoded();
		byte[] combined = Arrays.copyOf(kemPublic, kemPublic.length + ecPublicClient.length);
		System.arraycopy(ecPublicClient, 0, combined, kemPublic.length, ecPublicClient.length);
		return combined;
	}

	@Override
	protected byte[] decodeQS(byte[] q_s) {
		int encapsulationBytes = extractor.getEncapsulationLength();
		if (q_s.length < encapsulationBytes) {
			throw new IllegalArgumentException("ML-KEM+NISTP Q_S too short: " + q_s.length);
		}
		ecPublicServer = Arrays.copyOfRange(q_s, encapsulationBytes, q_s.length);
		return q_s;
	}

	@Override
	protected byte[] generateSecret() throws InvalidKeyException, IllegalStateException, InvalidKeySpecException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
		keyAgreement.doPhase(ECUtils.decodeKey(ecPublicServer, ecCurve), true);
		byte[] ecdhSecret = keyAgreement.generateSecret();
		byte[] encapsulation = Arrays.copyOfRange(Q_S, 0, extractor.getEncapsulationLength());

		try {
			Digest keyHash = (Digest) JCEComponentManager.getInstance().supportedDigests().getInstance(hashAlgorithm);
			keyHash.putBytes(extractor.extractSecret(encapsulation));
			keyHash.putBytes(ecdhSecret);
			return keyHash.doFinal();
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	protected byte[] getKemPublicKeyFromClient(byte[] q_c) {
		if (q_c.length < kemPublicKeyBytes) {
			throw new IllegalArgumentException("ML-KEM+NISTP Q_C too short: " + q_c.length);
		}
		return Arrays.copyOf(q_c, kemPublicKeyBytes);
	}

	protected byte[] getEcPublicKeyFromClient(byte[] q_c) {
		if (q_c.length < kemPublicKeyBytes) {
			throw new IllegalArgumentException("ML-KEM+NISTP Q_C too short: " + q_c.length);
		}
		return Arrays.copyOfRange(q_c, kemPublicKeyBytes, q_c.length);
	}
}
