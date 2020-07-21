/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
	public KnownHostsFile(File file) throws SshException, IOException {
		this.file = file;
		try(InputStream in = new FileInputStream(file)) {
			load(in);
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
	
	public KnownHostsFile() throws SshException, IOException {
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
		try {
			store();
		} catch (IOException e) {
			Log.error("Failed to store known_hosts file", e);
		}
	}
}
