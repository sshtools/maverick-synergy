package com.sshtools.common.knownhosts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class KnownHostsFile extends KnownHostsKeyVerification {

	public static Path defaultKnownHostsFile() {
		return Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts");
	}

	private Path file;

	public KnownHostsFile(File file) throws SshException {
		this(file.toPath());
	}
	
	public KnownHostsFile(Path file) throws SshException {
		this.file = file;
		try(var in = Files.newInputStream(file)) {
			load(in);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}
	
	public void store() throws IOException {
		try(var out = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {
			out.write(toString());
		}
			
	}
	
	public Path getFile() {
		return file;
	}
	
	public File getKnownHostsFile() {
		return file.toFile();
	}
	
	public boolean isHostFileWriteable() {
		return Files.isReadable(file);
	}
	
	public KnownHostsFile() throws SshException {
		this(defaultKnownHostsFile());
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
