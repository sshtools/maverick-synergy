package com.sshtools.client;

import java.io.File;
import java.io.FileInputStream;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Implements public key authentication, taking a java.io.File object as the source private key.
 */
public class PrivateKeyFileAuthenticator extends PublicKeyAuthenticator {

	File keyfile;
	String passphrase;
	
	public PrivateKeyFileAuthenticator(File keyfile, String passphrase) {
		this.keyfile = keyfile;
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile) {
		this.keyfile = keyfile;
	}
	
	public String getPassphrase() {
		return passphrase;
	}
	
	@Override
	public void authenticate(TransportProtocolClient transport, String username) {
		
		try {
			SshPrivateKeyFile privateKeyFile = SshPrivateKeyFileFactory.parse(new FileInputStream(keyfile));
			SshKeyPair pair;
			if(privateKeyFile.isPassphraseProtected()) {
				pair = privateKeyFile.toKeyPair(getPassphrase());
			} else {
				pair = privateKeyFile.toKeyPair(null);
			}
			setKeyPair(pair);
			super.authenticate(transport, username);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
