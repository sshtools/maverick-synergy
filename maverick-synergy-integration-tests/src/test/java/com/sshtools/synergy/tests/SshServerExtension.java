package com.sshtools.synergy.tests;

/*-
 * #%L
 * Integration Tests
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.InMemoryPublicKeyAuthenticator;
import com.sshtools.server.SshServer;

/**
 * JUnit 5 extension that starts an in-process {@link SshServer} before all
 * tests in a class and shuts it down afterwards.  Designed for use with
 * {@code @RegisterExtension static} so the server is shared across all test
 * methods in the class but isolated from other test classes.
 *
 * <p>The server binds to port 0 (OS-assigned random port).  Call
 * {@link #getPort()} after the extension is activated to obtain the actual
 * port.
 *
 * <p>A temporary directory is created for every activation and is used as the
 * SFTP/virtual-filesystem root.  It is deleted recursively in
 * {@code afterAll}.
 */
public class SshServerExtension implements BeforeAllCallback, AfterAllCallback {

    /** Default test username registered in the in-memory authenticators. */
    public static final String TEST_USER = "testuser";

    /** Password for {@link #TEST_USER}. */
    public static final String TEST_PASSWORD = "s3cr3tT3st!";

    private SshServer server;
    private SshKeyPair serverHostKey;
    private SshKeyPair clientKeyPair;
    private Path tempDir;

    // -----------------------------------------------------------------------
    // Extension lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        tempDir = Files.createTempDirectory("ssh-it-");

        // Generate test key material
        serverHostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        clientKeyPair  = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

        // Build and configure the server
        server = new SshServer(0);   // port 0 → OS picks a free port
        server.addHostKey(serverHostKey);

        server.addAuthenticator(
            new InMemoryPasswordAuthenticator()
                .addUser(TEST_USER, TEST_PASSWORD.toCharArray()));

        server.addAuthenticator(
            new InMemoryPublicKeyAuthenticator()
                .addAuthorizedKey(TEST_USER, clientKeyPair.getPublicKey()));

        final Path root = tempDir;
        server.setFileFactory(con ->
            NioFileFactoryBuilder.create()
                .withHome(root)
                .withoutSandbox()
                .build());

        server.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (server != null && server.isRunning()) {
            server.stop();
        }
        deleteTempDir();
    }

    // -----------------------------------------------------------------------
    // Accessors used by test classes
    // -----------------------------------------------------------------------

    /** Returns the actual TCP port the server is listening on. */
    public int getPort() {
        return server.getPort();
    }

    /**
     * Returns the ephemeral ED25519 key pair that should be used as a client
     * identity in public-key authentication tests.  The matching public key
     * is already registered with the server's {@link InMemoryPublicKeyAuthenticator}.
     */
    public SshKeyPair getClientKeyPair() {
        return clientKeyPair;
    }

    /**
     * Returns the temporary directory used as the SFTP virtual-filesystem root
     * for the lifetime of this extension activation.
     */
    public Path getTempDir() {
        return tempDir;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void deleteTempDir() {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) { }
                });
        } catch (IOException ignored) { }
    }
}
