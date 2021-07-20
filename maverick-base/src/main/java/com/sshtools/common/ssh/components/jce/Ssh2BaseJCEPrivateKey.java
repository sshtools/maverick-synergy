
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
