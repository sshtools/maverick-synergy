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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Implements public key authentication, taking a java.io.File object as the source private key.
 */
public class IdentityFileAuthenticator extends PublicKeyAuthenticator {

	private List<Path> identities;
	private PassphrasePrompt passphrase;
	
	private Path currentPath;
	private SshPublicKey currentKey;
	private String lastPassphrase;
	
	public IdentityFileAuthenticator(Collection<Path> identities, PassphrasePrompt passphrase) {
		this.identities = new ArrayList<>(identities);
		this.passphrase = passphrase;
	}
	
	public IdentityFileAuthenticator(PassphrasePrompt passphrase) throws IOException {
		this.identities = collectIdentities(true);
		this.passphrase = passphrase;
	}
	
	public String getPassphrase(String keyinfo) {
		return passphrase.getPasshrase(keyinfo);
	}
	
	public Path getCurrentPath() {
		return currentPath;
	}

	public SshPublicKey getCurrentKey() {
		return currentKey;
	}

	public static List<Path> collectIdentities(boolean onlyWellKnown) throws IOException {
		
		final Path dir = Paths.get(System.getProperty("user.home"), ".ssh");
		
		if(onlyWellKnown) {
			return new ArrayList<>(Arrays.asList(
					dir.resolve("id_ed25519.pub"),
					dir.resolve("id_ed448.pub"),
					dir.resolve("id_rsa.pub"),
					dir.resolve("id_ecdsa.pub")));
		} else {
			
			var filter = dir.getFileSystem().getPathMatcher("glob:**/*.pub");
			try (var stream = Files.list(dir)) {
			    return stream.filter(filter::matches).collect(Collectors.toList());
			}
		}
	}

	@Override
	public synchronized void done(boolean success) {
		try {
			super.done(success);
		}
		finally {
			passphrase.completed(success, lastPassphrase, this);
		}
	}

	@Override
	protected SshPublicKey getNextKey() throws IOException {
		return currentKey;
	}

	@Override
	protected SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException {
		
		Path name = currentPath.getName(currentPath.getNameCount()-1);
		String keyinfo = name.toString();
		keyinfo = keyinfo.substring(0, keyinfo.length()-4);
		String privatePath = currentPath.toAbsolutePath().toString();
		privatePath = privatePath.substring(0, privatePath.length() - 4);
		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(Paths.get(privatePath));
		
		if(file.isPassphraseProtected()) {
			return file.toKeyPair(lastPassphrase = getPassphrase(keyinfo));
		} else {
			return file.toKeyPair(lastPassphrase = null);
		}
	}

	@Override
	protected boolean hasCredentialsRemaining() {

		while(!identities.isEmpty()) {
			try {
				currentPath = identities.remove(0);
				
				if(currentPath.toFile().exists()) {
					if(Log.isDebugEnabled()) {
						Log.debug("Trying identity file {}", currentPath.toString());
					}
					
					currentKey = SshKeyUtils.getPublicKey(currentPath.toAbsolutePath());
					
					if(Log.isDebugEnabled()) {
						Log.debug("Authenticating with key {}", SshKeyUtils.getFingerprint(currentKey));
					}
					
					return true;
				}
			} catch (IOException e) {
				Log.error("Failed to parse identity file", e);
			}
		}
		
		return false;
	}

}
