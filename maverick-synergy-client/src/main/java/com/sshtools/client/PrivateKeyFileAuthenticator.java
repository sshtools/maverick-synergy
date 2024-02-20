package com.sshtools.client;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.sshtools.common.publickey.InvalidPassphraseException;
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
	private Path path;
	
	public PrivateKeyFileAuthenticator(File path, String passphrase) throws IOException {
		this(path.toPath(), passphrase);
	}
	
	public PrivateKeyFileAuthenticator(File path, PassphrasePrompt passphrase) throws IOException {
		this(path.toPath(), passphrase);
	}
	
	public PrivateKeyFileAuthenticator(Path path, String passphrase) throws IOException {
		this(path);
		this.passphrase = (keyinfo) -> passphrase;
	}
	
	public PrivateKeyFileAuthenticator(Path path, PassphrasePrompt passphrase) throws IOException {
		this(path);
		this.passphrase = passphrase;
	}
	
	public PrivateKeyFileAuthenticator(File path) throws IOException {
		this(path.toPath());
	}

	public PrivateKeyFileAuthenticator(Path path) throws IOException {
		this.keyfile = SshPrivateKeyFileFactory.parse(path);
		this.path = path;
	}
	
	public String getPassphrase() {
		return passphrase.getPasshrase(String.format("Passphrase for %s: ", path.getFileName().toString()));
	}

	@Override
	protected SshPublicKey getNextKey() throws IOException {
		try {
			if(keyfile.isPassphraseProtected()) {
				pair = keyfile.toKeyPair(getPassphrase());
			} else {
				pair = keyfile.toKeyPair(null);
			}
		} catch (IOException | InvalidPassphraseException e) {
			throw new IOException(e.getMessage(), e);
		}
		return pair.getPublicKey();
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException {
		return pair;
	}

	@Override
	protected boolean hasCredentialsRemaining() {
		return pair==null;
	}


}
