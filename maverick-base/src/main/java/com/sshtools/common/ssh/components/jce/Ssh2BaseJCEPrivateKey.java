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

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;

import com.sshtools.common.ssh.components.SshPrivateKey;

public abstract class Ssh2BaseJCEPrivateKey implements SshPrivateKey {

	protected PrivateKey prv;
	protected Provider customProvider;
	
	public Ssh2BaseJCEPrivateKey(PrivateKey prv) {
		this.prv = prv;
	}
	
	public Ssh2BaseJCEPrivateKey(PrivateKey prv, Provider customProvider) {
		this.prv = prv;
		this.customProvider = customProvider;
	}
	
	public PrivateKey getJCEPrivateKey() {
		return prv;
	}
	
	protected Signature getJCESignature(String algorithm) throws NoSuchAlgorithmException {
		
		Signature  sig = null;
		if(customProvider!=null) {
			try {
				sig = Signature.getInstance(algorithm, customProvider);
			} catch(NoSuchAlgorithmException e) {
			}
		}
		
		if(sig==null) {
			sig = JCEProvider.getProviderForAlgorithm(algorithm)== null ? 
						Signature.getInstance(algorithm)
					:   Signature.getInstance(algorithm, 
						JCEProvider.getProviderForAlgorithm(algorithm));
		}
		return sig;
	}
}
