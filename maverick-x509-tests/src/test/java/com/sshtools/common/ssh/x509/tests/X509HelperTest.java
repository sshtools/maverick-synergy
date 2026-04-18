package com.sshtools.common.ssh.x509.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKey;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaSha1PublicKey;
import com.sshtools.common.ssh.x509.X509Helper;

/**
 * Tests for {@link X509Helper#loadKeystore} using a programmatically
 * created PKCS12 key store containing a self-signed RSA certificate.
 */
public class X509HelperTest {

    private static final String ALIAS      = "test-key";
    private static final String STORE_PASS = "storepass";
    private static final String KEY_PASS   = "storepass";

    private static byte[] pkcs12Sha1Bytes;
    private static byte[] pkcs12Sha256Bytes;

    @BeforeAll
    static void buildKeyStores() throws Exception {
        // Register BC as security provider for certificate building
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }

        pkcs12Sha1Bytes   = buildPkcs12("SHA1WithRSA",   2048);
        pkcs12Sha256Bytes = buildPkcs12("SHA256WithRSA", 2048);
    }

    // ---------------------------------------------------------------
    // SHA-1 signed certificate → 3 key pairs
    // ---------------------------------------------------------------

    @Test
    void loadKeystore_sha1RsaCert_returnsThreeKeyPairs() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha1Bytes);
        assertEquals(3, pairs.length,
                "SHA1WithRSA certificate should produce 3 key pairs");
    }

    @Test
    void loadKeystore_sha1RsaCert_firstPairIsSha1Type() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha1Bytes);
        assertTrue(pairs[0].getPublicKey() instanceof SshX509RsaSha1PublicKey,
                "First key pair should use x509v3-sign-rsa-sha1");
    }

    @Test
    void loadKeystore_sha1RsaCert_secondPairIsSignRsaType() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha1Bytes);
        assertTrue(pairs[1].getPublicKey() instanceof SshX509RsaPublicKey,
                "Second key pair should use x509v3-sign-rsa");
    }

    @Test
    void loadKeystore_sha1RsaCert_thirdPairIsRfc6187Type() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha1Bytes);
        assertTrue(pairs[2].getPublicKey() instanceof SshX509RsaPublicKeyRfc6187,
                "Third key pair should use x509v3-ssh-rsa (RFC 6187)");
    }

    // ---------------------------------------------------------------
    // SHA-256 signed certificate → 1 key pair
    // ---------------------------------------------------------------

    @Test
    void loadKeystore_sha256RsaCert_returnsAtLeastOneKeyPair() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha256Bytes);
        assertTrue(pairs.length >= 1,
                "SHA256WithRSA certificate should produce at least one key pair");
    }

    @Test
    void loadKeystore_sha256RsaCert_allPrivateKeysNonNull() throws Exception {
        SshKeyPair[] pairs = loadKeystore(pkcs12Sha256Bytes);
        for (SshKeyPair pair : pairs) {
            assertNotNull(pair.getPrivateKey(),
                    "Private key must not be null in returned key pair");
        }
    }

    // ---------------------------------------------------------------
    // Error case
    // ---------------------------------------------------------------

    @Test
    void loadKeystore_badPassword_throwsIOException() {
        assertThrows(java.io.IOException.class, () ->
            X509Helper.loadKeystore(new ByteArrayInputStream(pkcs12Sha1Bytes),
                    ALIAS, "wrong-password", KEY_PASS));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private SshKeyPair[] loadKeystore(byte[] pkcs12Bytes) throws Exception {
        return X509Helper.loadKeystore(
                new ByteArrayInputStream(pkcs12Bytes), ALIAS, STORE_PASS, KEY_PASS);
    }

    private static byte[] buildPkcs12(String sigAlgorithm, int keySize) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(keySize);
        KeyPair kp = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=test, O=maverick-test");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L);
        Date notAfter  = new Date(System.currentTimeMillis() + 86_400_000L);

        JcaX509v3CertificateBuilder certBuilder =
                new JcaX509v3CertificateBuilder(subject, serial, notBefore, notAfter, subject,
                        kp.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder(sigAlgorithm)
                .setProvider("BC")
                .build(kp.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(ALIAS, kp.getPrivate(), KEY_PASS.toCharArray(),
                new Certificate[]{cert});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ks.store(baos, STORE_PASS.toCharArray());
        return baos.toByteArray();
    }
}
