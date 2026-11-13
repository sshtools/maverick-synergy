package com.sshtools.synergy.virtual.tests;

/*-
 * #%L
 * Virtual Connection Tests
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

import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.knownhosts.KnownHostsFile;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.server.SshServer;
import com.sshtools.synergy.nio.ConnectRequestFuture;

public class HostKeyUpdateVirtualConnectionTests extends AbstractVirtualConnectionTests {

    private static final String VIRTUAL_KNOWN_HOST = "[virtual]:0";

    private Path knownHostsPath;
    private TrustingKnownHostsFile knownHosts;
    private SshKeyPair activeHostKey;
    private SshKeyPair futureHostKey;

    @Override
    protected void setUp() throws Exception {
        knownHostsPath = Files.createTempFile("known-hosts-", ".txt");
        Files.writeString(knownHostsPath, "");

        activeHostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        futureHostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ECDSA, 256);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            if (knownHostsPath != null) {
                Files.deleteIfExists(knownHostsPath);
                knownHostsPath = null;
            }
        }
    }

    @Override
    protected void configureServer(SshServer server) throws IOException, SshException {
        super.configureServer(server);

        server.getHostKeys().clear();
        server.addHostKey(activeHostKey);
        server.addFutureHostKey(futureHostKey);
    }

    @Override
    protected void configureClientContext(SshClientContext ctx) throws IOException, SshException {
        knownHosts = new TrustingKnownHostsFile(knownHostsPath);
        ctx.setHostKeyVerification(knownHosts);
        ctx.setUsername("admin");
        ctx.addAuthenticator(PasswordAuthenticator.forPassword("admin"));
    }

    public void testHostKeyUpdateAddsFutureKeyWithoutReplacingCurrentKey() throws Exception {
        ConnectRequestFuture future = connectVirtual();
        assertTrue("Virtual connection did not succeed", future.isSuccess());

        var con = future.getConnection();
        con.getAuthenticatedFuture().waitForever();
        assertTrue("Authentication did not succeed", con.getAuthenticatedFuture().isSuccess());

        waitForKnownHost(VIRTUAL_KNOWN_HOST, futureHostKey.getPublicKey(), 5000L);

        assertTrue("Current host key should remain trusted",
                knownHosts.isKnownHost(VIRTUAL_KNOWN_HOST, activeHostKey.getPublicKey()));
        assertTrue("Future host key should be trusted after proof validation",
                knownHosts.isKnownHost(VIRTUAL_KNOWN_HOST, futureHostKey.getPublicKey()));

        TrustingKnownHostsFile reloaded = new TrustingKnownHostsFile(knownHostsPath);
        assertTrue("Reloaded known_hosts should still trust current host key",
                reloaded.isKnownHost(VIRTUAL_KNOWN_HOST, activeHostKey.getPublicKey()));
        assertTrue("Reloaded known_hosts should trust future host key",
                reloaded.isKnownHost(VIRTUAL_KNOWN_HOST, futureHostKey.getPublicKey()));
    }

    private void waitForKnownHost(String host, SshPublicKey key, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (knownHosts.isKnownHost(host, key)) {
                return;
            }
            Thread.sleep(25L);
        }
        fail("Timed out waiting for future host key to be added for " + host);
    }

    private static final class TrustingKnownHostsFile extends KnownHostsFile {

        private TrustingKnownHostsFile(Path file) throws SshException {
            super(file);
        }

        @Override
        protected void onUnknownHost(String host, SshPublicKey key) throws SshException {
            allowHost(host, key, true);
        }

        @Override
        protected void onHostKeyMismatch(String host, java.util.List<SshPublicKey> allowedHostKeys,
                SshPublicKey actualHostKey) throws SshException {
            allowHost(host, actualHostKey, true);
        }
    }
}
