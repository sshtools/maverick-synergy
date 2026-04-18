package com.sshtools.server.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sshtools.server.components.SshKeyExchangeServerFactory;
import com.sshtools.server.components.jce.Curve25519SHA256LibSshServer;
import com.sshtools.server.components.jce.Curve25519SHA256Server;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp256;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp384;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp521;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha256JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup15Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup16Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup17Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup18Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup1Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha256JCE;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

import java.util.stream.Stream;

@DisplayName("Server key-exchange component algorithm names")
class ServerKeyExchangeAlgorithmTest {

    @SuppressWarnings("rawtypes")
    static Stream<SshKeyExchangeServerFactory> factories() {
        return Stream.of(
            new Curve25519SHA256Server.Curve25519SHA256ServerFactory(),
            new Curve25519SHA256LibSshServer.Curve25519SHA256LibSshServerFactory(),
            new DiffieHellmanGroup1Sha1JCE.DiffieHellmanGroup1Sha1JCEFactory(),
            new DiffieHellmanGroup14Sha1JCE.DiffieHellmanGroup14Sha1JCEFactory(),
            new DiffieHellmanGroup14Sha256JCE.DiffieHellmanGroup14Sha256JCEFactory(),
            new DiffieHellmanGroup15Sha512JCE.DiffieHellmanGroup15Sha512JCEFactory(),
            new DiffieHellmanGroup16Sha512JCE.DiffieHellmanGroup16Sha512JCEFactory(),
            new DiffieHellmanGroup17Sha512JCE.DiffieHellmanGroup17Sha512JCEFactory(),
            new DiffieHellmanGroup18Sha512JCE.DiffieHellmanGroup18Sha512JCEFactory(),
            new DiffieHellmanGroupExchangeSha1JCE.DiffieHellmanGroupExchangeSha1JCEFactory(),
            new DiffieHellmanGroupExchangeSha256JCE.DiffieHellmanGroupExchangeSha256JCEFactory(),
            new DiffieHellmanEcdhNistp256.DiffieHellmanEcdhNistp256Factory(),
            new DiffieHellmanEcdhNistp384.DiffieHellmanEcdhNistp384Factory(),
            new DiffieHellmanEcdhNistp521.DiffieHellmanEcdhNistp521Factory()
        );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @ParameterizedTest(name = "{0}")
    @MethodSource("factories")
    @DisplayName("factory creates instance with non-empty algorithm name")
    void factory_createsInstance_withValidAlgorithmName(SshKeyExchangeServerFactory factory)
            throws NoSuchAlgorithmException, IOException {

        // Factory must declare at least one algorithm key
        String[] keys = factory.getKeys();
        assertNotNull(keys, "getKeys() must not return null");
        assertFalse(keys.length == 0, "getKeys() must return at least one algorithm name");
        for (String key : keys) {
            assertNotNull(key, "Algorithm key must not be null");
            assertFalse(key.isBlank(), "Algorithm key must not be blank");
        }

        // create() must succeed without throwing
        SshKeyExchange instance = (SshKeyExchange) factory.create();
        assertNotNull(instance, "create() must return a non-null instance");
    }
}
