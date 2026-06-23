package com.sshtools.examples.simplesshd;

/*-
 * #%L
 * Simple SSHD
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

import com.sshtools.common.files.memory.InMemoryFileFactory;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.vsession.ShellCommandFactory;
import com.sshtools.server.vsession.VirtualChannelFactory;
import com.sshtools.server.vsession.VirtualSessionPolicy.VirtualSessionPolicyBuilder;

public final class SimpleSshdMain {

	private static final int DEFAULT_PORT = 2222;

	private SimpleSshdMain() {
	}

	public static void main(String[] args) throws Exception {
		int port = parsePort(args);

		JCEProvider.enableBouncyCastle(true);

		SshServer server = new ResilientHostKeySshServer(port);
		server.addAuthenticator(new InMemoryPasswordAuthenticator().addUser("admin", "admin".toCharArray()));

		VirtualFileFactory vff = createVirtualFileFactory();
		server.setFileFactory(con -> vff);
		server.setChannelFactory(new VirtualChannelFactory(new ShellCommandFactory()));
		server.setDefaultPolicies(VirtualSessionPolicyBuilder.create().build());

		server.start();

		Log.info("simple-sshd started on port {} (user=admin, password=admin)", server.getPort());

		Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "simple-sshd-shutdown"));

        server.getShutdownFuture().waitForever();

	}

	private static VirtualFileFactory createVirtualFileFactory() throws IOException {
		try {
			return new VirtualFileFactory(new VirtualMountTemplate("/", "/", new InMemoryFileFactory(), true));
		}
		catch (PermissionDeniedException e) {
			throw new IOException("Failed to create in-memory virtual filesystem", e);
		}
	}

	private static int parsePort(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("--port".equals(arg) && i + 1 < args.length) {
				return parsePortValue(args[i + 1], DEFAULT_PORT);
			}
			if (arg.startsWith("--port=")) {
				return parsePortValue(arg.substring("--port=".length()), DEFAULT_PORT);
			}
		}
		return DEFAULT_PORT;
	}

	private static int parsePortValue(String value, int fallback) {
		try {
			int parsed = Integer.parseInt(value);
			if (parsed < 1 || parsed > 65535) {
				Log.warn("Ignoring invalid port '{}', using {}", value, fallback);
				return fallback;
			}
			return parsed;
		}
		catch (NumberFormatException nfe) {
			Log.warn("Ignoring invalid port '{}', using {}", value, fallback);
			return fallback;
		}
	}

	private static final class ResilientHostKeySshServer extends SshServer {

		private File configFolder = new File(".");

		ResilientHostKeySshServer(int port) throws IOException {
			super(port);
		}

		@Override
		public void setConfigFolder(File confFolder) {
			super.setConfigFolder(confFolder);
			this.configFolder = confFolder;
		}

		@Override
		protected synchronized void configureHostKeys(SshServerContext sshContext, SocketChannel sc)
				throws IOException, SshException {
			if (!getHostKeys().isEmpty()) {
				sshContext.addHostKeys(getHostKeys());
				return;
			}

			File rsaFile = new File(configFolder, "ssh_host_rsa");
			File ecdsa256File = new File(configFolder, "ssh_host_ecdsa_256");
			File ecdsa384File = new File(configFolder, "ssh_host_ecdsa_384");
			File ecdsa521File = new File(configFolder, "ssh_host_ecdsa_521");
			File ed25519File = new File(configFolder, "ssh_host_ed25519");
			File ed448File = new File(configFolder, "ssh_host_ed448");

			List<KeyLoadAttempt> attempts = Arrays.asList(
				new KeyLoadAttempt("ssh-rsa", () -> sshContext.loadOrGenerateHostKey(rsaFile, SshKeyPairGenerator.SSH2_RSA, 2048)),
				new KeyLoadAttempt("rsa-sha2-256", () -> SshKeyUtils.getRSAPrivateKeyWithSHA256Signature(rsaFile, null)),
				new KeyLoadAttempt("rsa-sha2-512", () -> SshKeyUtils.getRSAPrivateKeyWithSHA512Signature(rsaFile, null)),
				new KeyLoadAttempt("ecdsa-sha2-nistp256", () -> sshContext.loadOrGenerateHostKey(ecdsa256File, SshKeyPairGenerator.ECDSA, 256)),
				new KeyLoadAttempt("ecdsa-sha2-nistp384", () -> sshContext.loadOrGenerateHostKey(ecdsa384File, SshKeyPairGenerator.ECDSA, 384)),
				new KeyLoadAttempt("ecdsa-sha2-nistp521", () -> sshContext.loadOrGenerateHostKey(ecdsa521File, SshKeyPairGenerator.ECDSA, 521)),
				new KeyLoadAttempt("ssh-ed25519", () -> sshContext.loadOrGenerateHostKey(ed25519File, SshKeyPairGenerator.ED25519, 0)),
				new KeyLoadAttempt("ssh-ed448", () -> sshContext.loadOrGenerateHostKey(ed448File, SshKeyPairGenerator.ED448, 0))
			);

			for (KeyLoadAttempt attempt : attempts) {
				try {
					SshKeyPair pair = attempt.loader.load();
					sshContext.addHostKey(pair);
					addHostKey(pair);
					Log.info("Loaded host key type {}", attempt.name);
				}
				catch (IOException | InvalidPassphraseException | SshException e) {
					Log.warn("Host key type {} unavailable: {}", attempt.name, e.getMessage());
				}
			}

			if (getHostKeys().isEmpty()) {
				throw new IOException("No host keys could be loaded or generated.");
			}
		}
	}

	@FunctionalInterface
	private interface KeyLoader {
		SshKeyPair load() throws IOException, InvalidPassphraseException, SshException;
	}

	private static final class KeyLoadAttempt {
		private final String name;
		private final KeyLoader loader;

		private KeyLoadAttempt(String name, KeyLoader loader) {
			this.name = name;
			this.loader = loader;
		}
	}
}
