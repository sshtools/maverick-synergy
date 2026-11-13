package com.sshtools.server.tests;

/*-
 * #%L
 * Server API Tests
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.SshEngine;
import com.sshtools.synergy.ssh.SshContext;

@DisplayName("Future host key configuration")
class FutureHostKeysTest {

    @Test
    @DisplayName("addFutureHostKey rejects duplicate algorithms")
    void addFutureHostKeyRejectsDuplicateAlgorithms() throws Exception {
        SshServerContext ctx = new SshServerContext(new SshEngine());
        SshKeyPair key1 = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);
        SshKeyPair key2 = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

        ctx.addFutureHostKey(key1);

        assertThrows(IOException.class, () -> ctx.addFutureHostKey(key2));
    }

    @Test
    @DisplayName("ssh-rsa future host key is upgraded to RSA SHA2 variants")
    void addFutureHostKeyRsaUpgrade() throws Exception {
        SshServerContext ctx = new SshServerContext(new SshEngine());
        SshKeyPair rsa = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 2048);

        ctx.addFutureHostKey(rsa);

        Set<String> algorithms = Arrays.stream(ctx.getFutureHostKeys())
                .map(k -> k.getPublicKey().getAlgorithm())
                .collect(Collectors.toSet());

        assertTrue(algorithms.contains(SshContext.PUBLIC_KEY_SSHRSA), "Expected ssh-rsa entry");
        if (ctx.supportedPublicKeys().contains(SshContext.PUBLIC_KEY_RSA_SHA256)) {
            assertTrue(algorithms.contains(SshContext.PUBLIC_KEY_RSA_SHA256), "Expected rsa-sha2-256 upgrade");
        }
        if (ctx.supportedPublicKeys().contains(SshContext.PUBLIC_KEY_RSA_SHA512)) {
            assertTrue(algorithms.contains(SshContext.PUBLIC_KEY_RSA_SHA512), "Expected rsa-sha2-512 upgrade");
        }
    }

    @Test
    @DisplayName("explicit RSA SHA2 future key is rejected when implied by existing ssh-rsa future key")
    void addFutureHostKeyRejectsDuplicateRsaUpgradeAlgorithm() throws Exception {
        SshServerContext ctx = new SshServerContext(new SshEngine());
        SshKeyPair rsa = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 2048);
        SshKeyPair explicitSha256 = SshKeyUtils.makeRSAWithSHA256Signature(
                SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 2048));

        ctx.addFutureHostKey(rsa);

        if (ctx.supportedPublicKeys().contains(SshContext.PUBLIC_KEY_RSA_SHA256)) {
            assertThrows(IOException.class, () -> ctx.addFutureHostKey(explicitSha256));
        }
    }
}
