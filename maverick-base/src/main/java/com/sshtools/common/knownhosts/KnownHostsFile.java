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

package com.sshtools.common.knownhosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.IOUtils;

public class KnownHostsFile extends KnownHostsKeyVerification {

	File file;
	
	public KnownHostsFile(File file) throws SshException {
		this.file = file;
		try(InputStream in = new FileInputStream(file)) {
			load(in);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}
	
	public void store() throws IOException {
		IOUtils.writeStringToFile(file, toString(), "UTF-8");
	}
	
	public File getKnownHostsFile() {
		return file;
	}
	
	public boolean isHostFileWriteable() {
		return file.canWrite();
	}
	
	public KnownHostsFile() throws SshException {
		this(new File(new File(System.getProperty("user.home"), ".ssh"), "known_hosts"));
	}
	
	@Override
	protected void onInvalidHostEntry(String entry) throws SshException {

	}

	@Override
	protected void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
			throws SshException {

	}

	@Override
	protected void onUnknownHost(String host, SshPublicKey key) throws SshException {

	}

	@Override
	protected void onRevokedKey(String host, SshPublicKey key) {

	}

	@Override
	protected void onHostKeyUpdated(Set<String> names, SshPublicKey key) {
		save();
	}
	
	@Override
	protected void onHostKeyAdded(Set<String> names, SshPublicKey key) {
		save();
	}

	@Override
	protected void onHostKeyRemoved(Set<String> names, SshPublicKey key) {
		save();
	}

	protected void save() {
		try {
			store();
		} catch (IOException e) {
			Log.error("Failed to store known_hosts file", e);
		}
	}
}
