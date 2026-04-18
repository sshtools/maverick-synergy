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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.client.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sshtools.client.SshKeyExchangeClientFactory;
import com.sshtools.client.components.Curve25519SHA256Client.Curve25519SHA256ClientFactory;
import com.sshtools.client.components.Curve25519SHA256LibSshClient.Curve25519SHA256LibSshClientFactory;
import com.sshtools.client.components.DiffieHellmanEcdhNistp256.DiffieHellmanEcdhNistp256Factory;
import com.sshtools.client.components.DiffieHellmanEcdhNistp384.DiffieHellmanEcdhNistp384Factory;
import com.sshtools.client.components.DiffieHellmanEcdhNistp521.DiffieHellmanEcdhNistp521Factory;
import com.sshtools.client.components.DiffieHellmanGroup14Sha1JCE.DiffieHellmanGroup14Sha1JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup14Sha256JCE.DiffieHellmanGroup14Sha256JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup15Sha512JCE.DiffieHellmanGroup15Sha512JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup16Sha512JCE.DiffieHellmanGroup16Sha512JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup17Sha512JCE.DiffieHellmanGroup17Sha512JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup18Sha512JCE.DiffieHellmanGroup18Sha512JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroup1Sha1JCE.DiffieHellmanGroup1Sha1JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroupExchangeSha1JCE.DiffieHellmanGroupExchangeSha1JCEFactory;
import com.sshtools.client.components.DiffieHellmanGroupExchangeSha256JCE.DiffieHellmanGroupExchangeSha256JCEFactory;
import com.sshtools.client.components.Rsa1024Sha1.Rsa1024Sha1Factory;
import com.sshtools.client.components.Rsa2048Sha256.Rsa2048Sha256Factory;

/**
 * Parameterized test covering all client-side key-exchange algorithm factory
 * classes.  For each factory the test verifies:
 * <ul>
 *   <li>{@code getKeys()} returns a non-null, non-empty array with no blank entries</li>
 *   <li>{@code create()} returns a non-null instance</li>
 * </ul>
 */
public class ClientKeyExchangeAlgorithmTest {

    static Stream<SshKeyExchangeClientFactory<?>> factories() {
        return Stream.of(
            new Curve25519SHA256ClientFactory(),
            new Curve25519SHA256LibSshClientFactory(),
            new DiffieHellmanGroup1Sha1JCEFactory(),
            new DiffieHellmanGroup14Sha1JCEFactory(),
            new DiffieHellmanGroup14Sha256JCEFactory(),
            new DiffieHellmanGroup15Sha512JCEFactory(),
            new DiffieHellmanGroup16Sha512JCEFactory(),
            new DiffieHellmanGroup17Sha512JCEFactory(),
            new DiffieHellmanGroup18Sha512JCEFactory(),
            new DiffieHellmanGroupExchangeSha1JCEFactory(),
            new DiffieHellmanGroupExchangeSha256JCEFactory(),
            new DiffieHellmanEcdhNistp256Factory(),
            new DiffieHellmanEcdhNistp384Factory(),
            new DiffieHellmanEcdhNistp521Factory(),
            new Rsa1024Sha1Factory(),
            new Rsa2048Sha256Factory()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void getKeys_nonNullAndNonEmpty(SshKeyExchangeClientFactory<?> factory) {
        String[] keys = factory.getKeys();
        assertNotNull(keys, "getKeys() must not return null");
        assertTrue(keys.length > 0, "getKeys() must return at least one key name");
        for (String key : keys) {
            assertTrue(key != null && !key.isBlank(),
                "Each key name must be non-null and non-blank; got: " + key);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    public void create_returnsNonNull(SshKeyExchangeClientFactory<?> factory)
            throws NoSuchAlgorithmException, IOException {
        assertNotNull(factory.create(), "create() must return a non-null instance");
    }
}
