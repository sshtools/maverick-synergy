package com.sshtools.synergy.virtual.tests;

/*-
 * #%L
 * Virtual Connection Tests
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
import java.time.Duration;

import com.sshtools.client.SshClientContext;
import com.sshtools.common.files.memory.InMemoryFileFactory;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;
import com.sshtools.server.vsession.ShellCommandFactory;
import com.sshtools.server.vsession.VirtualChannelFactory;
import com.sshtools.server.vsession.VirtualSessionPolicy.VirtualSessionPolicyBuilder;
import com.sshtools.synergy.nio.ConnectRequestFuture;

import junit.framework.TestCase;

/**
 * Abstract base class for tests that exercise the SSH stack via an in-memory
 * {@link com.sshtools.synergy.nio.VirtualSelectableChannel} rather than a real
 * TCP socket.
 *
 * <h2>Subclassing</h2>
 * <ol>
 *   <li>Extend this class.</li>
 *   <li>Implement {@link #configureServer(SshServer)} to add host keys (or rely
 *       on the auto-generated Ed25519 key provided here), authenticators,
 *       channel factories, command factories, etc.</li>
 *   <li>Implement {@link #configureClientContext(SshClientContext)} to add
 *       client-side authenticators (password, public-key, keyboard-interactive)
 *       and any other client options.</li>
 *   <li>Call {@link #connectVirtual()} inside your {@code testXxx()} methods to
 *       obtain a connected {@link ConnectRequestFuture} and retrieve the
 *       {@link com.sshtools.synergy.ssh.Connection} from it.</li>
 * </ol>
 *
 * <h2>Server lifecycle</h2>
 * A fresh {@link SshServer} (without any listening socket) is started before
 * every test method and stopped afterwards. The server shares the same
 * {@link com.sshtools.synergy.nio.SshEngine} that drives virtual connections,
 * so no real network port is opened.
 */
public abstract class AbstractVirtualConnectionTests extends TestCase {

	/**
	 * Default connection timeout used by {@link #connectVirtual()}.
	 * Override {@link #connectionTimeout()} to change it per subclass.
	 */
	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

	/** The server instance created fresh for each test. */
	protected SshServer server;

	// -------------------------------------------------------------------------
	// JUnit lifecycle
	// -------------------------------------------------------------------------

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		server = new SshServer();

        configureServer(server);

		// Start without binding a TCP listening interface – virtual connections
		// do not need one.
		server.start(false);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (server != null && server.isRunning()) {
				server.stop();
			}
		} finally {
			server = null;
			super.tearDown();
		}
	}

	// -------------------------------------------------------------------------
	// Abstract configuration hooks
	// -------------------------------------------------------------------------

	/**
	 * Configure the {@link SshServer} before it is started.
	 *
	 * <p>Typical uses:</p>
	 * <ul>
	 *   <li>Adding custom {@link com.sshtools.common.auth.Authenticator} instances
	 *       via {@code server.addAuthenticator(...)}</li>
	 *   <li>Registering a channel factory with
	 *       {@code server.setChannelFactory(...)}</li>
	 *   <li>Setting up forwarding policies, file system factories, etc.</li>
	 *   <li>Replacing the auto-generated host key with a deterministic one for
	 *       reproducible tests (call {@code server.addHostKey(...)} again).</li>
	 * </ul>
	 *
	 * @param server the server instance, not yet started
	 * @throws IOException  if key material cannot be loaded
	 * @throws SshException if the server cannot be configured
	 */
	protected void configureServer(SshServer server) throws IOException, SshException {
        // Provide a fresh Ed25519 host key so the server can complete key exchange
		// even if configureServer() does not add its own key.
		SshKeyPair defaultHostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519);
		server.addHostKey(defaultHostKey);

        server.addAuthenticator(new InMemoryPasswordAuthenticator()
            .addUser("admin", "admin".toCharArray()));
		
        VirtualFileFactory vff;
        try {
            vff = new VirtualFileFactory(
                new VirtualMountTemplate("/", "/", new InMemoryFileFactory(), true));
        } catch (PermissionDeniedException e) {
            throw new IOException("Failed to create VirtualFileFactory", e);
        }
        server.setFileFactory(con -> vff);

        server.setChannelFactory(new VirtualChannelFactory(
            new ShellCommandFactory()));

        server.setDefaultPolicies(VirtualSessionPolicyBuilder.create()
            .build());
        
    }

	/**
	 * Configure the {@link SshClientContext} that will be used to make a virtual
	 * connection.
	 *
	 * <p>You <strong>must</strong> add at least one
	 * {@link com.sshtools.client.ClientAuthenticator} (e.g.
	 * {@link com.sshtools.client.PasswordAuthenticator}) and set a username via
	 * {@code ctx.setUsername("...")} for authentication to succeed.</p>
	 *
	 * <p>The host-key verification callback is pre-configured to accept any key,
	 * which is intentional for in-process tests. Override
	 * {@link #createClientContext()} if you need stricter verification.</p>
	 *
	 * @param ctx the client context, not yet connected
	 * @throws IOException  if key material cannot be loaded
	 * @throws SshException if the context cannot be configured
	 */
	protected abstract void configureClientContext(SshClientContext ctx) throws IOException, SshException;

	// -------------------------------------------------------------------------
	// Helper methods
	// -------------------------------------------------------------------------

	/**
	 * Create a {@link SshClientContext} wired to the running server's engine,
	 * with a permissive host-key verifier suitable for in-process tests.
	 *
	 * <p>Calls {@link #configureClientContext(SshClientContext)} before
	 * returning.</p>
	 *
	 * @return the configured (but not yet connected) client context
	 * @throws IOException  propagated from context creation
	 * @throws SshException propagated from context creation
	 */
	protected SshClientContext createClientContext() throws IOException, SshException {
		SshClientContext ctx = new SshClientContext(server.getEngine());
		// Accept any host key in-process; no MITM risk in a local test.
		ctx.setHostKeyVerification((host, pk) -> true);
		configureClientContext(ctx);
		return ctx;
	}

	/**
	 * Establish a virtual (in-memory) SSH connection to the server, wait up to
	 * {@link #connectionTimeout()} for it to complete, and return the future.
	 *
	 * <p>Use the returned future to inspect success or failure and obtain the
	 * connected {@link com.sshtools.synergy.ssh.Connection}:</p>
	 * <pre>{@code
	 * ConnectRequestFuture future = connectVirtual();
	 * assertTrue("Connection should succeed", future.isSuccess());
	 * Connection<?> conn = future.getConnection();
	 * }</pre>
	 *
	 * @return the completed future for the client side of the connection
	 * @throws IOException          if the virtual channel pair cannot be created
	 * @throws SshException         if an SSH-layer error occurs
	 * @throws InterruptedException if the calling thread is interrupted while waiting
	 */
	protected ConnectRequestFuture connectVirtual()
			throws IOException, SshException, InterruptedException {
		SshClientContext ctx = createClientContext();
		ConnectRequestFuture future = server.acceptVirtualConnection(ctx);
		future.waitFor(connectionTimeout());
		return future;
	}

	/**
	 * Timeout applied when waiting for the virtual connection to complete.
	 * Defaults to 30 seconds. Override in subclasses that need a different value.
	 *
	 * @return the maximum time to wait for a connection
	 */
	protected Duration connectionTimeout() {
		return DEFAULT_TIMEOUT;
	}
}
