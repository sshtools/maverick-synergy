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
package com.sshtools.server.kex.bc;

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

import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.server.components.jce.DiffieHellmanEcdh;

public abstract class AbstractMlkemNistpKex extends DiffieHellmanEcdh {

	private final MLKEMParameters kemParameters;
	private final int kemPublicKeyBytes;
	private final String ecCurve;

	protected SecretWithEncapsulation encaps;
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
	protected byte[] decodeQC(byte[] q_c) {
		if (q_c.length < kemPublicKeyBytes) {
			throw new IllegalArgumentException("ML-KEM+NISTP Q_C too short: " + q_c.length);
		}
		byte[] kemPublic = Arrays.copyOf(q_c, kemPublicKeyBytes);
		ecPublicClient = Arrays.copyOfRange(q_c, kemPublicKeyBytes, q_c.length);

		MLKEMGenerator kemGenerator = new MLKEMGenerator(JCEComponentManager.getSecureRandom());
		MLKEMPublicKeyParameters params = new MLKEMPublicKeyParameters(kemParameters, kemPublic);
		encaps = kemGenerator.generateEncapsulated(params);
		return q_c;
	}

	@Override
	protected byte[] encodeQS() {
		ecPublicServer = super.encodeQS();
		byte[] encapsulation = encaps.getEncapsulation();
		byte[] combined = Arrays.copyOf(encapsulation, encapsulation.length + ecPublicServer.length);
		System.arraycopy(ecPublicServer, 0, combined, encapsulation.length, ecPublicServer.length);
		return combined;
	}

	@Override
	protected byte[] generateSecret() throws InvalidKeyException, IllegalStateException, InvalidKeySpecException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
		keyAgreement.doPhase(ECUtils.decodeKey(ecPublicClient, ecCurve), true);
		byte[] ecdhSecret = keyAgreement.generateSecret();

		try {
			Digest keyHash = (Digest) JCEComponentManager.getInstance().supportedDigests().getInstance(hashAlgorithm);
			keyHash.putBytes(encaps.getSecret());
			keyHash.putBytes(ecdhSecret);
			return keyHash.doFinal();
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
