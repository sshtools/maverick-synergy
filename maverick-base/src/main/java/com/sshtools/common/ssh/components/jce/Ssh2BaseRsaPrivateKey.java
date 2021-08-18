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



