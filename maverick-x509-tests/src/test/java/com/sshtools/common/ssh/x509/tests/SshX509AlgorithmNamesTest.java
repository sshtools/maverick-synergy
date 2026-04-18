package com.sshtools.common.ssh.x509.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sshtools.common.ssh.x509.*;

/**
 * Tests that every {@code SshPublicKeyFactory} in the maverick-x509 module
 * reports the correct SSH algorithm identifiers via {@code getKeys()}.
 */
public class SshX509AlgorithmNamesTest {

    // ---------------------------------------------------------------
    // Parameterised: factory getKeys() returns expected algorithm
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource({
        "x509v3-sign-rsa,      x509v3-sign-rsa",
        "x509v3-ssh-rsa,       x509v3-ssh-rsa",
        "x509v3-rsa2048-sha256,x509v3-rsa2048-sha256",
        "x509v3-sign-dss,      x509v3-sign-dss",
        "x509v3-ssh-dss,       x509v3-ssh-dss",
        "x509v3-sign-rsa-sha1, x509v3-sign-rsa-sha1",
        "x509v3-ecdsa-sha2-nistp256,x509v3-ecdsa-sha2-nistp256",
        "x509v3-ecdsa-sha2-nistp384,x509v3-ecdsa-sha2-nistp384",
        "x509v3-ecdsa-sha2-nistp521,x509v3-ecdsa-sha2-nistp521"
    })
    void factory_getKeys_returnsExpectedAlgorithm(String label, String expectedAlgorithm) throws Exception {
        String[] keys = factoryForLabel(label.trim()).getKeys();
        assertNotNull(keys, "getKeys() must not return null for " + label);
        assertTrue(keys.length > 0, "getKeys() must return at least one entry for " + label);
        assertEquals(expectedAlgorithm.trim(), keys[0],
                "First algorithm for factory " + label + " should be " + expectedAlgorithm.trim());
    }

    // ---------------------------------------------------------------
    // Individual algorithm constant tests (compile-time guard)
    // ---------------------------------------------------------------

    @Test
    void x509RsaPublicKey_algorithmConstant() {
        assertEquals("x509v3-sign-rsa", SshX509RsaPublicKey.X509V3_SIGN_RSA);
    }

    @Test
    void x509RsaPublicKeyRfc6187_algorithmConstant() {
        assertEquals("x509v3-ssh-rsa", SshX509RsaPublicKeyRfc6187.X509V3_SSH_RSA);
    }

    @Test
    void x509Rsa2048Sha256Rfc6187_algorithmConstant() {
        assertEquals("x509v3-rsa2048-sha256", SshX509Rsa2048Sha256Rfc6187.X509V3_SSH_RSA);
    }

    @Test
    void x509DsaPublicKey_algorithmConstant() {
        assertEquals("x509v3-sign-dss", SshX509DsaPublicKey.X509V3_SIGN_DSA);
    }

    @Test
    void x509DsaPublicKeyRfc6187_algorithmConstant() {
        assertEquals("x509v3-ssh-dss", SshX509DsaPublicKeyRfc6187.X509V3_SSH_DSS);
    }

    @Test
    void x509RsaSha1PublicKey_algorithmConstant() {
        assertEquals("x509v3-sign-rsa-sha1", SshX509RsaSha1PublicKey.X509V3_SIGN_RSA_SHA1);
    }

    // ---------------------------------------------------------------
    // helper
    // ---------------------------------------------------------------

    private com.sshtools.common.ssh.components.SshPublicKeyFactory<?> factoryForLabel(String label)
            throws Exception {
        switch (label) {
            case "x509v3-sign-rsa":
                return new SshX509RsaPublicKey.SshX509RsaPublicKeyFactory();
            case "x509v3-ssh-rsa":
                return new SshX509RsaPublicKeyRfc6187.SshX509RsaPublicKeyRfc6187Factory();
            case "x509v3-rsa2048-sha256":
                return new SshX509Rsa2048Sha256Rfc6187.SshX509Rsa2048Sha256Rfc6187Factory();
            case "x509v3-sign-dss":
                return new SshX509DsaPublicKey.SshX509DsaPublicKeyFactory();
            case "x509v3-ssh-dss":
                return new SshX509DsaPublicKeyRfc6187.SshX509DsaPublicKeyRfc6187Factory();
            case "x509v3-sign-rsa-sha1":
                return new SshX509RsaSha1PublicKey.SshX509RsaSha1PublicKeyFactory();
            case "x509v3-ecdsa-sha2-nistp256":
                return new SshX509EcdsaSha2Nist256Rfc6187.SshX509EcdsaSha2Nist256Rfc6187Factory();
            case "x509v3-ecdsa-sha2-nistp384":
                return new SshX509EcdsaSha2Nist384Rfc6187.SshX509EcdsaSha2Nist384Rfc6187Factory();
            case "x509v3-ecdsa-sha2-nistp521":
                return new SshX509EcdsaSha2Nist521Rfc6187.SshX509EcdsaSha2Nist521Rfc6187Factory();
            default:
                throw new IllegalArgumentException("Unknown label: " + label);
        }
    }
}
