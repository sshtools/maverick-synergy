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

package com.sshtools.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Supplier;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Implements public key authentication, taking a java.io.File object as the source private key.
 */
public class PrivateKeyFileAuthenticator extends PublicKeyAuthenticator {

	private File keyfile;
	private Supplier<String> passphrase;
	
	public PrivateKeyFileAuthenticator(File keyfile, String passphrase) {
		this.keyfile = keyfile;
		this.passphrase = () -> passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile, Supplier<String> passphrase) {
		this.keyfile = keyfile;
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile) {
		this.keyfile = keyfile;
	}
	
	public String getPassphrase() {
		return passphrase.get();
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
