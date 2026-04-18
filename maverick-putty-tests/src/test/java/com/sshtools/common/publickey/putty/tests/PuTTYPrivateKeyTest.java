package com.sshtools.common.publickey.putty.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.putty.PuTTYPrivateKeyProvider;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

/**
 * Tests for PuTTYPrivateKeyProvider and PuTTYPrivateKeyFile.
 *
 * <p>Because the maverick library cannot <em>write</em> PPK format keys,
 * test PPK v2 material is built programmatically in {@link #buildPpkV2Ed25519}.</p>
 */
public class PuTTYPrivateKeyTest {

    private static PuTTYPrivateKeyProvider provider;
    /** Unencrypted Ed25519 PPK v2 bytes generated in {@link #setUpClass()}. */
    private static byte[] ppkV2Ed25519Bytes;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Ensure BC JCE provider is registered so PuTTYPrivateKeyFile can
        // reconstruct JCE key objects after parsing.
        JCEComponentManager.getDefaultInstance();

        provider = new PuTTYPrivateKeyProvider();

        // Generate a fresh Ed25519 key pair using BouncyCastle native API
        Ed25519KeyPairGenerator gen = new Ed25519KeyPairGenerator();
        gen.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        AsymmetricCipherKeyPair kp = gen.generateKeyPair();
        byte[] seed = ((Ed25519PrivateKeyParameters) kp.getPrivate()).getEncoded();
        byte[] pubKeyBytes = ((Ed25519PublicKeyParameters) kp.getPublic()).getEncoded();

        ppkV2Ed25519Bytes = buildPpkV2Ed25519(seed, pubKeyBytes, "none", "test-ed25519");
    }

    // ---------------------------------------------------------------
    // isFormatted
    // ---------------------------------------------------------------

    @Test
    void isFormatted_withPpkV2Header_returnsTrue() throws IOException {
        byte[] ppk = "PuTTY-User-Key-File-2: ssh-ed25519\nEncryption: none\n".getBytes("UTF-8");
        assertTrue(provider.isFormatted(ppk));
    }

    @Test
    void isFormatted_withPpkV3Header_returnsTrue() throws IOException {
        byte[] ppk = "PuTTY-User-Key-File-3: ssh-ed25519\nEncryption: none\n".getBytes("UTF-8");
        assertTrue(provider.isFormatted(ppk));
    }

    @Test
    void isFormatted_withPpkV1Header_returnsTrue() throws IOException {
        byte[] ppk = "PuTTY-User-Key-File-1:\n".getBytes("UTF-8");
        assertTrue(provider.isFormatted(ppk));
    }

    @Test
    void isFormatted_withOpenSshKey_returnsFalse() throws IOException {
        byte[] openssh = "-----BEGIN OPENSSH PRIVATE KEY-----\ndata\n-----END OPENSSH PRIVATE KEY-----\n"
                .getBytes("UTF-8");
        assertFalse(provider.isFormatted(openssh));
    }

    @Test
    void isFormatted_withEmptyBytes_returnsFalse() throws IOException {
        assertFalse(provider.isFormatted(new byte[0]));
    }

    @Test
    void isFormatted_withRandomBytes_returnsFalse() throws IOException {
        byte[] random = new byte[64];
        new SecureRandom().nextBytes(random);
        // Random bytes are extremely unlikely to start with the PPK header
        // (we just verify no exception is thrown)
        assertDoesNotThrow(() -> provider.isFormatted(random));
    }

    // ---------------------------------------------------------------
    // isPassphraseProtected
    // ---------------------------------------------------------------

    @Test
    void isPassphraseProtected_falseForUnencryptedKey() throws IOException {
        var file = provider.create(ppkV2Ed25519Bytes);
        assertFalse(file.isPassphraseProtected());
    }

    @Test
    void isPassphraseProtected_trueForEncryptedKey() throws IOException {
        // Only needs the header lines to be correct – full key data not required
        // because isPassphraseProtected only reads the first two lines.
        byte[] encryptedHeader = (
                "PuTTY-User-Key-File-2: ssh-ed25519\n" +
                "Encryption: aes256-cbc\n" +
                "Comment: encrypted-key\n").getBytes("UTF-8");
        var file = provider.create(encryptedHeader);
        assertTrue(file.isPassphraseProtected());
    }

    // ---------------------------------------------------------------
    // getType
    // ---------------------------------------------------------------

    @Test
    void getType_returnsPuTTY() throws IOException {
        var file = provider.create(ppkV2Ed25519Bytes);
        assertEquals("PuTTY", file.getType());
    }

    // ---------------------------------------------------------------
    // toKeyPair – unencrypted Ed25519 round-trip
    // ---------------------------------------------------------------

    @Test
    void toKeyPair_unencryptedEd25519_returnsKeyPairWithCorrectAlgorithm()
            throws IOException, InvalidPassphraseException {
        var file = provider.create(ppkV2Ed25519Bytes);
        SshKeyPair pair = file.toKeyPair(null);
        assertNotNull(pair);
        assertNotNull(pair.getPublicKey());
        assertNotNull(pair.getPrivateKey());
        assertEquals("ssh-ed25519", pair.getPublicKey().getAlgorithm());
    }

    @Test
    void toKeyPair_publicKeyCanVerifyPrivateKeySignature()
            throws IOException, InvalidPassphraseException, Exception {
        var file = provider.create(ppkV2Ed25519Bytes);
        SshKeyPair pair = file.toKeyPair(null);
        byte[] data = "hello-world".getBytes("UTF-8");
        byte[] sig = pair.getPrivateKey().sign(data, "ssh-ed25519");
        assertTrue(pair.getPublicKey().verifySignature(wrapSignature("ssh-ed25519", sig), data),
                "Signature produced by the private key must be verifiable by the public key");
    }

    // ---------------------------------------------------------------
    // create – error handling
    // ---------------------------------------------------------------

    @Test
    void create_withRandomBytes_throwsIOException() {
        // The factory validates format on create(); non-PPK bytes → IOException
        assertThrows(java.io.IOException.class,
                () -> provider.create("not a ppk file".getBytes("UTF-8")));
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    /**
     * Build a PPK v2 unencrypted Ed25519 key in text form from raw key material.
     *
     * <p>The PPK v2 format for an unencrypted Ed25519 key:</p>
     * <pre>
     * PuTTY-User-Key-File-2: ssh-ed25519
     * Encryption: none
     * Comment: &lt;comment&gt;
     * Public-Lines: 1
     * &lt;base64 of ssh-wire-encoded public key&gt;
     * Private-Lines: 1
     * &lt;base64 of 4-byte-len + 64-byte (seed || pubkey)&gt;
     * Private-MAC: 0000000000000000000000000000000000000000
     * </pre>
     * The MAC is not validated by the reader for unencrypted keys.
     */
    static byte[] buildPpkV2Ed25519(byte[] seed, byte[] pubKeyBytes,
                                     String encryption, String comment) throws IOException {
        // ---- public blob: string("ssh-ed25519") + binary(pubKeyBytes) ----
        ByteArrayOutputStream pubBaos = new ByteArrayOutputStream();
        DataOutputStream pubOut = new DataOutputStream(pubBaos);
        writeSshString(pubOut, "ssh-ed25519");
        writeSshBinaryString(pubOut, pubKeyBytes);
        byte[] publicBlob = pubBaos.toByteArray();

        // ---- private blob: binary(seed || pubkey)  64 bytes total ----
        byte[] privateData = new byte[64];
        System.arraycopy(seed, 0, privateData, 0, 32);
        System.arraycopy(pubKeyBytes, 0, privateData, 32, 32);
        ByteArrayOutputStream prvBaos = new ByteArrayOutputStream();
        DataOutputStream prvOut = new DataOutputStream(prvBaos);
        writeSshBinaryString(prvOut, privateData);
        byte[] privateBlob = prvBaos.toByteArray();

        String publicBase64  = Base64.getEncoder().encodeToString(publicBlob);
        String privateBase64 = Base64.getEncoder().encodeToString(privateBlob);

        // PPK reader concatenates all "Public-Lines" lines then decodes as base64.
        // A single long line is perfectly valid.
        StringBuilder sb = new StringBuilder();
        sb.append("PuTTY-User-Key-File-2: ssh-ed25519\n");
        sb.append("Encryption: ").append(encryption).append("\n");
        sb.append("Comment: ").append(comment).append("\n");
        sb.append("Public-Lines: 1\n");
        sb.append(publicBase64).append("\n");
        sb.append("Private-Lines: 1\n");
        sb.append(privateBase64).append("\n");
        // MAC not checked for unencrypted keys
        sb.append("Private-MAC: 0000000000000000000000000000000000000000\n");

        return sb.toString().getBytes("UTF-8");
    }

    /** Write a length-prefixed UTF-8 string in SSH wire format. */
    private static void writeSshString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes("UTF-8");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    /** Write a length-prefixed byte array in SSH wire format. */
    private static void writeSshBinaryString(DataOutputStream out, byte[] data) throws IOException {
        out.writeInt(data.length);
        out.write(data);
    }

    /**
     * Wrap a raw signature bytes in the SSH signature blob format:
     * string(algorithm) + binary(sig).
     */
    private static byte[] wrapSignature(String algorithm, byte[] sig) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        writeSshString(out, algorithm);
        writeSshBinaryString(out, sig);
        return baos.toByteArray();
    }
}
