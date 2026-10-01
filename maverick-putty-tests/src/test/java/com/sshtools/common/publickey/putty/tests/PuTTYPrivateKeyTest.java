package com.sshtools.common.publickey.putty.tests;

/*-
 * #%L
 * PuTTY Key Format Tests
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
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.putty.PuTTYPrivateKeyProvider;
import com.sshtools.common.ssh.SshException;
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
    // PuTTY v3 write round-trip tests
    // ---------------------------------------------------------------

    @Test
    void writeV3_ed25519_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        assertRoundTrip("ssh-ed25519", original);
    }

    @Test
    void writeV3_ed448_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd448KeyPair();
        assertRoundTrip("ssh-ed448", original);
    }

    @Test
    void writeV3_rsa2048_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateRsaKeyPair(2048, 2);
        assertRoundTrip("ssh-rsa", original);
    }

    @Test
    void writeV3_ecdsa256_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEcdsaKeyPair(256);
        assertRoundTrip("ecdsa-sha2-nistp256", original);
    }

    @Test
    void writeV3_ecdsa384_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEcdsaKeyPair(384);
        assertRoundTrip("ecdsa-sha2-nistp384", original);
    }

    @Test
    void writeV3_ecdsa521_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEcdsaKeyPair(521);
        assertRoundTrip("ecdsa-sha2-nistp521", original);
    }

    @Test
    void writeV3_dsa1024_roundTrip() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateDsaKeyPair(1024);
        assertRoundTrip("ssh-dss", original);
    }

    @Test
    void writeV3_commentIsPreserved() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, "my-key-comment");
        byte[] formatted = keyFile.getFormattedKey();
        // The comment should appear in the formatted text
        String text = new String(formatted, "UTF-8");
        assertTrue(text.contains("Comment: my-key-comment"), "Formatted key must contain the comment");
        // Verify the comment survives the round-trip via getComment()
        SshPrivateKeyFile reparsed = provider.create(formatted);
        reparsed.toKeyPair(null); // trigger comment parsing
        assertEquals("my-key-comment", reparsed.getComment());
        // Public key must also survive
        SshKeyPair roundTripped = provider.create(formatted).toKeyPair(null);
        assertEquals(original.getPublicKey().getFingerprint(),
                roundTripped.getPublicKey().getFingerprint());
    }

    @Test
    void writeV3_nullComment_treatedAsEmpty() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, null);
        byte[] formatted = keyFile.getFormattedKey();
        String text = new String(formatted, "UTF-8");
        assertTrue(text.contains("Comment: \n"), "Null comment must produce empty comment line");
    }

    @Test
    void writeV3_formattedKeyStartsWithV3Header() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        byte[] formatted = provider.create(original, "test").getFormattedKey();
        String text = new String(formatted, "UTF-8");
        assertTrue(text.startsWith("PuTTY-User-Key-File-3:"),
                "Formatted key must start with PuTTY-User-Key-File-3: header");
    }

    @Test
    void writeV3_notPassphraseProtected() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, "test");
        // v3 unencrypted keys have Encryption: none so must not be passphrase protected
        assertFalse(keyFile.isPassphraseProtected(), "v3 unencrypted key must not be passphrase protected");
    }

    @Test
    void changePassphrase_v3UnencryptedToEncrypted_roundTripsAndRequiresPassphrase() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, "passphrase-change");

        keyFile.changePassphrase(null, "secret-pass");

        assertTrue(keyFile.supportsPassphraseChange());
        assertTrue(keyFile.isPassphraseProtected());
        assertThrows(IOException.class, () -> keyFile.toKeyPair("wrong-pass"));

        SshKeyPair decrypted = keyFile.toKeyPair("secret-pass");
        assertEquals(original.getPublicKey().getFingerprint(), decrypted.getPublicKey().getFingerprint());
    }

    @Test
    void changePassphrase_v3EncryptedToEncrypted_rotatesPassphrase() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, "rotate-passphrase");

        keyFile.changePassphrase(null, "old-pass");
        keyFile.changePassphrase("old-pass", "new-pass");

        assertThrows(IOException.class, () -> keyFile.toKeyPair("old-pass"));
        SshKeyPair decrypted = keyFile.toKeyPair("new-pass");
        assertEquals(original.getPublicKey().getFingerprint(), decrypted.getPublicKey().getFingerprint());
    }

    @Test
    void changePassphrase_v3EncryptedToUnencrypted_removesPassphrase() throws Exception {
        SshKeyPair original = JCEComponentManager.getDefaultInstance().generateEd25519KeyPair();
        SshPrivateKeyFile keyFile = provider.create(original, "remove-passphrase");

        keyFile.changePassphrase(null, "secret-pass");
        keyFile.changePassphrase("secret-pass", "");

        assertFalse(keyFile.isPassphraseProtected());
        SshKeyPair decrypted = keyFile.toKeyPair(null);
        assertEquals(original.getPublicKey().getFingerprint(), decrypted.getPublicKey().getFingerprint());
    }

    @Test
    void changePassphrase_v2Key_throwsUnsupported() throws Exception {
        SshPrivateKeyFile v2 = provider.create(ppkV2Ed25519Bytes);
        assertFalse(v2.supportsPassphraseChange());
        assertThrows(IOException.class, () -> v2.changePassphrase(null, "new-pass"));
    }

    /**
     * Helper: encode key pair to PuTTY v3, parse it back, verify algorithm and
     * that the private key can sign data verifiable by the round-tripped public key.
     */
    private void assertRoundTrip(String expectedAlgorithm, SshKeyPair original)
            throws IOException, SshException, InvalidPassphraseException {

        SshPrivateKeyFile keyFile = provider.create(original, "round-trip-test");
        byte[] formatted = keyFile.getFormattedKey();
        assertTrue(provider.isFormatted(formatted), "Output must be recognised as PuTTY format");

        SshKeyPair roundTripped = provider.create(formatted).toKeyPair(null);
        assertNotNull(roundTripped, "Round-tripped key pair must not be null");
        assertEquals(expectedAlgorithm, roundTripped.getPublicKey().getAlgorithm(),
                "Algorithm must be preserved through round-trip");

        // Public key fingerprint must survive the round-trip
        assertEquals(original.getPublicKey().getFingerprint(),
                roundTripped.getPublicKey().getFingerprint(),
                "Public key fingerprint must be identical after round-trip");

        // Signature produced by original private key must verify with round-tripped public key
        byte[] data = "round-trip-test-payload".getBytes("UTF-8");
        byte[] sig = original.getPrivateKey().sign(data, expectedAlgorithm);
        assertTrue(roundTripped.getPublicKey().verifySignature(
                wrapSignature(expectedAlgorithm, sig), data),
                "Signature from original key must verify with round-tripped public key");
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
