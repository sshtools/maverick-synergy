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

import java.util.Arrays;

import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeKEMGenerator;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimeParameters;
import org.bouncycastle.pqc.crypto.ntruprime.SNTRUPrimePublicKeyParameters;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

public class AbstractSNTRUPx25519DH extends Abstractx25519KEMDH {

	private static final int SNTRUP_KEY_SIZE = 1158;
	
	protected AbstractSNTRUPx25519DH(String name, String hashAlgorithm, SecurityLevel level, int priority) {
		super(name, hashAlgorithm, level, priority);
	}

     protected int getPublicKeyLength() {
         return SNTRUPrimeParameters.sntrup761.getPublicKeyBytes();
     }
     
     @Override
 	protected byte[] decodeQC(byte[] q_c) {

 		if(q_c.length < SNTRUP_KEY_SIZE) {
 			throw new IllegalArgumentException("SNTRUP Q_C too short: " + q_c.length);
 		}
 		byte[] pk = Arrays.copyOf(q_c, SNTRUP_KEY_SIZE);
 		SNTRUPrimeKEMGenerator kemGenerator = new SNTRUPrimeKEMGenerator(JCEComponentManager.getSecureRandom());
        SNTRUPrimePublicKeyParameters params = new SNTRUPrimePublicKeyParameters(SNTRUPrimeParameters.sntrup761, pk);
        encaps = kemGenerator.generateEncapsulated(params);
        K_A = Arrays.copyOfRange(q_c, SNTRUP_KEY_SIZE, q_c.length);
        return q_c;
	}
}
