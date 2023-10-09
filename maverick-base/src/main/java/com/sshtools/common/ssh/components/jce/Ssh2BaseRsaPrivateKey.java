package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;

import com.sshtools.common.ssh.components.SshPrivateKey;

public abstract class Ssh2BaseRsaPrivateKey extends Ssh2BaseJCEPrivateKey implements SshPrivateKey {


    public Ssh2BaseRsaPrivateKey(PrivateKey prv) {
    	super(prv);
    }

    public Ssh2BaseRsaPrivateKey(PrivateKey prv, Provider customProvider) {
        super(prv, customProvider);
    }

    protected byte[] doSign(byte[] data, String signingAlgorithm) throws IOException {
    	
    	Signature l_sig;
    	
		switch(signingAlgorithm) {
		case "rsa-sha2-256":
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA256WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		case "rsa-sha2-512":
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA512WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		default:
			try {
				l_sig = getJCESignature(JCEAlgorithms.JCE_SHA1WithRSA);
				break;
			} catch (Exception e) {
				throw new IOException("Failed to sign data! " + e.getMessage());
			}
		}

		try {
			l_sig.initSign(prv);
			l_sig.update(data);
			return l_sig.sign();
		} catch (SignatureException | InvalidKeyException e) {
			throw new IOException(e.getMessage(), e);
		}
    }

	
}



