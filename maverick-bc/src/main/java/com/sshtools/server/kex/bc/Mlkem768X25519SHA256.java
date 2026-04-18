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

import java.util.Arrays;

import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.synergy.ssh.SshContext;

public class Mlkem768X25519SHA256 extends Abstractx25519KEMDH {

	static final int MLKEM_KEY_SIZE = 1184;
	
	public Mlkem768X25519SHA256() {
		super(SshContext.KEX_MLKEM768_X25519_SHA256, JCEAlgorithms.JCE_SHA256, SecurityLevel.PARANOID, 99999);
	}

	@Override
	protected byte[] decodeQC(byte[] q_c) {

		if(q_c.length < MLKEM_KEY_SIZE) {
			throw new IllegalArgumentException("MLKEM Q_C too short: " + q_c.length);
		}
		byte[] pk = Arrays.copyOf(q_c, MLKEM_KEY_SIZE);
		MLKEMGenerator kemGenerator = new MLKEMGenerator(JCEComponentManager.getSecureRandom());
		MLKEMPublicKeyParameters params = new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, pk);
		encaps = kemGenerator.generateEncapsulated(params);
		K_A = Arrays.copyOfRange(q_c, MLKEM_KEY_SIZE, q_c.length);
		return q_c;
	}

}
