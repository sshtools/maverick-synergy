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
import java.io.IOException;
import java.nio.file.Path;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication, taking a java.io.File object as the source private key.
 */
public class PrivateKeyFileAuthenticator extends PublicKeyAuthenticator {

	private SshPrivateKeyFile keyfile;
	private PassphrasePrompt passphrase;
	private SshKeyPair pair;
	
	public PrivateKeyFileAuthenticator(File keyfile, String passphrase) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile.toPath());
		this.passphrase = (keyinfo) -> passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile, PassphrasePrompt passphrase) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile.toPath());
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(Path keyfile, String passphrase) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile);
		this.passphrase = (keyinfo) -> passphrase;
	}
	
	public PrivateKeyFileAuthenticator(Path keyfile, PassphrasePrompt passphrase) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile);
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File keyfile) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile.toPath());
	}

	public PrivateKeyFileAuthenticator(Path keyfile) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(keyfile);
	}
	
	public String getPassphrase() {
		return passphrase.getPasshrase(SshKeyUtils.getFingerprint(pair.getPublicKey()));
	}

	@Override
	protected SshPublicKey getPublicKey() throws IOException, InvalidPassphraseException {
		pair = keyfile.toKeyPair(getPassphrase());
		return pair.getPublicKey();
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException {
		return pair;
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		return false;
	}


}
