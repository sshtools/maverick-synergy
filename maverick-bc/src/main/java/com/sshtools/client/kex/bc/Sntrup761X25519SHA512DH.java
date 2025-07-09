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