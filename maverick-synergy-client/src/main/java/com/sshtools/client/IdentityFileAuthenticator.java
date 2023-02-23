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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		this.identities = collectIdentities();
		this.passphrase = passphrase;
	}
	
	public String getPassphrase(String keyinfo) {
		return passphrase.getPasshrase(keyinfo);
	}
	
	public static List<Path> collectIdentities() throws IOException {
		
		final Path dir = Paths.get(System.getProperty("user.home"), ".ssh");
		final PathMatcher filter = dir.getFileSystem().getPathMatcher("glob:**/*.pub");
		try (final Stream<Path> stream = Files.list(dir)) {
		    return stream.filter(filter::matches).collect(Collectors.toList());
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
				
				if(Log.isDebugEnabled()) {
					Log.debug("Trying identity file {}", currentPath.toString());
				}
				
				currentKey = SshKeyUtils.getPublicKey(currentPath.toAbsolutePath());
				
				if(Log.isDebugEnabled()) {
					Log.debug("Authenticating with key {}", SshKeyUtils.getFingerprint(currentKey));
				}
				
				return true;
			} catch (IOException e) {
				Log.error("Failed to parse identity file", e);
			}
		}
		
		return false;
	}

}
