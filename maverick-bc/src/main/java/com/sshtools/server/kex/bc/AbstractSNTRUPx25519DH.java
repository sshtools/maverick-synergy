package com.sshtools.server.kex.bc;

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
