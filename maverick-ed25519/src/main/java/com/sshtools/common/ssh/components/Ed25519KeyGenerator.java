package com.sshtools.common.ssh.components;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.KeyGenerator;
import com.sshtools.common.ssh.SshException;

public class Ed25519KeyGenerator implements KeyGenerator {

	@Override
	public SshKeyPair generateKey(int bits) throws SshException {
		
		try {

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EdDSA", "EdDSA");
			KeyPair kp = keyGen.generateKeyPair();

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(new SshEd25519PrivateKey(kp.getPrivate()));
			pair.setPublicKey(new SshEd25519PublicKey(kp.getPublic()));
			
			return pair;
		} catch (NoSuchAlgorithmException
				| NoSuchProviderException e) {
			if(Log.isErrorEnabled()) {
				Log.error("ed25519 keys are not supported with the current configuration", e);
			}
			throw new SshException("ed25519 keys are not supported with the current configuration", SshException.BAD_API_USAGE);
		}
	}

}
