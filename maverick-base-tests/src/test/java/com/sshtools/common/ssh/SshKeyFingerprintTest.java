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
package com.sshtools.common.ssh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEProvider;

/**
 * Verifies that {@link SshKeyFingerprint} output for SHA256, MD5 and Bubble
 * Babble exactly matches the values reported by {@code ssh-keygen} for the
 * same keys.  A temporary RSA 2048 and Ed25519 key pair are generated once per
 * test run and deleted on completion.
 */
public class SshKeyFingerprintTest {

    private static Path tmpDir;
    private static File rsaPubKeyFile;
    private static File ed25519PubKeyFile;

    // -----------------------------------------------------------------------
    // Class-level setup / teardown
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void setUpClass() throws Exception {
        JCEProvider.disableBouncyCastle();
        tmpDir = Files.createTempDirectory("sshkeyfingerprint-test-");
        rsaPubKeyFile     = generateKeyPair(tmpDir, "test_rsa",     "rsa",     "2048");
        ed25519PubKeyFile = generateKeyPair(tmpDir, "test_ed25519", "ed25519", null);
    }

    @AfterClass
    public static void tearDownClass() {
        if (tmpDir != null) {
            deleteRecursive(tmpDir.toFile());
        }
    }

    // -----------------------------------------------------------------------
    // RSA — SHA256
    // -----------------------------------------------------------------------

    @Test
    public void testRSASHA256FingerprintMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(rsaPubKeyFile, "-E", "sha256");
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String actual = SshKeyUtils.getFingerprint(pub);
        assertEquals("RSA SHA256 fingerprint must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // RSA — MD5
    // -----------------------------------------------------------------------

    @Test
    public void testRSAMD5FingerprintMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(rsaPubKeyFile, "-E", "md5");
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String actual = SshKeyFingerprint.getFingerprint(pub.getEncoded(), SshKeyFingerprint.MD5_FINGERPRINT);
        assertEquals("RSA MD5 fingerprint must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // RSA — Bubble Babble
    // -----------------------------------------------------------------------

    @Test
    public void testRSABubbleBabbleMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(rsaPubKeyFile, "-B");
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String actual = SshKeyUtils.getBubbleBabble(pub);
        assertEquals("RSA bubble babble must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // Ed25519 — SHA256
    // -----------------------------------------------------------------------

    @Test
    public void testEd25519SHA256FingerprintMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(ed25519PubKeyFile, "-E", "sha256");
        SshPublicKey pub = SshKeyUtils.getPublicKey(ed25519PubKeyFile);
        String actual = SshKeyUtils.getFingerprint(pub);
        assertEquals("Ed25519 SHA256 fingerprint must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // Ed25519 — MD5
    // -----------------------------------------------------------------------

    @Test
    public void testEd25519MD5FingerprintMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(ed25519PubKeyFile, "-E", "md5");
        SshPublicKey pub = SshKeyUtils.getPublicKey(ed25519PubKeyFile);
        String actual = SshKeyFingerprint.getFingerprint(pub.getEncoded(), SshKeyFingerprint.MD5_FINGERPRINT);
        assertEquals("Ed25519 MD5 fingerprint must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // Ed25519 — Bubble Babble
    // -----------------------------------------------------------------------

    @Test
    public void testEd25519BubbleBabbleMatchesOpenSSH() throws Exception {
        String expected = sshKeygenFingerprint(ed25519PubKeyFile, "-B");
        SshPublicKey pub = SshKeyUtils.getPublicKey(ed25519PubKeyFile);
        String actual = SshKeyUtils.getBubbleBabble(pub);
        assertEquals("Ed25519 bubble babble must match ssh-keygen", expected, actual);
    }

    // -----------------------------------------------------------------------
    // Output format sanity checks
    // -----------------------------------------------------------------------

    @Test
    public void testSHA256FingerprintHasCorrectFormat() throws Exception {
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String fp = SshKeyUtils.getFingerprint(pub);
        assertTrue("SHA256 fingerprint must start with 'SHA256:'", fp.startsWith("SHA256:"));
        assertFalse("SHA256 fingerprint must not contain base64 padding '='", fp.contains("="));
    }

    @Test
    public void testMD5FingerprintHasCorrectFormat() throws Exception {
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String fp = SshKeyFingerprint.getFingerprint(pub.getEncoded(), SshKeyFingerprint.MD5_FINGERPRINT);
        assertTrue("MD5 fingerprint must start with 'MD5:'", fp.startsWith("MD5:"));
        // 16 bytes → 32 hex chars + 15 colons = 47 chars after the "MD5:" prefix
        assertEquals("MD5 fingerprint hex part must be 47 chars", 47, fp.substring(4).length());
    }

    @Test
    public void testBubbleBabbleHasCorrectFormat() throws Exception {
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        String bb = SshKeyUtils.getBubbleBabble(pub);
        assertTrue("Bubble babble must start with 'x'", bb.startsWith("x"));
        assertTrue("Bubble babble must end with 'x'", bb.endsWith("x"));
        assertTrue("Bubble babble must contain '-' separators", bb.contains("-"));
    }

    // -----------------------------------------------------------------------
    // Default algorithm can be changed and restored
    // -----------------------------------------------------------------------

    @Test
    public void testSetDefaultHashAlgorithmAffectsOutput() throws Exception {
        SshPublicKey pub = SshKeyUtils.getPublicKey(rsaPubKeyFile);
        SshKeyFingerprint.setDefaultHashAlgorithm(SshKeyFingerprint.MD5_FINGERPRINT);
        try {
            String fp = SshKeyUtils.getFingerprint(pub);
            assertTrue("After setting default to MD5, fingerprint must start with 'MD5:'", fp.startsWith("MD5:"));
        } finally {
            SshKeyFingerprint.setDefaultHashAlgorithm(SshKeyFingerprint.SHA256_FINGERPRINT);
        }
    }

    // -----------------------------------------------------------------------
    // Algorithm constant presence
    // -----------------------------------------------------------------------

    @Test
    public void testAlgorithmConstantsExist() {
        assertNotNull(SshKeyFingerprint.MD5_FINGERPRINT);
        assertNotNull(SshKeyFingerprint.SHA1_FINGERPRINT);
        assertNotNull(SshKeyFingerprint.SHA256_FINGERPRINT);
        assertFalse(SshKeyFingerprint.MD5_FINGERPRINT.isEmpty());
        assertFalse(SshKeyFingerprint.SHA1_FINGERPRINT.isEmpty());
        assertFalse(SshKeyFingerprint.SHA256_FINGERPRINT.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Generates an SSH key pair under {@code dir} and returns the {@code .pub} file.
     *
     * @param dir   directory to write files into
     * @param name  base filename (no extension)
     * @param type  key type passed to {@code -t}, e.g. {@code "rsa"} or {@code "ed25519"}
     * @param bits  bit size passed to {@code -b}, or {@code null} for key types that
     *              do not accept a bit-length (e.g. Ed25519)
     */
    private static File generateKeyPair(Path dir, String name, String type, String bits) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ssh-keygen");
        cmd.add("-t"); cmd.add(type);
        if (bits != null) {
            cmd.add("-b"); cmd.add(bits);
        }
        cmd.add("-N"); cmd.add("");   // no passphrase
        cmd.add("-f"); cmd.add(dir.resolve(name).toString());

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        int exit = p.waitFor();
        if (exit != 0) {
            String out = new String(p.getInputStream().readAllBytes());
            throw new RuntimeException("ssh-keygen failed for " + name + ": " + out);
        }
        return dir.resolve(name + ".pub").toFile();
    }

    /**
     * Runs {@code ssh-keygen -l [extraArgs] -f pubKeyFile} and returns the
     * second whitespace-separated token from the output line, which is the
     * fingerprint (e.g. {@code "SHA256:xxxx"}, {@code "MD5:xx:yy:..."}, or the
     * raw bubble-babble string when {@code -B} is used).
     */
    private static String sshKeygenFingerprint(File pubKeyFile, String... extraArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ssh-keygen");
        cmd.add("-l");
        for (String arg : extraArgs) {
            cmd.add(arg);
        }
        cmd.add("-f"); cmd.add(pubKeyFile.getAbsolutePath());

        Process p = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes()).trim();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new RuntimeException("ssh-keygen -l failed: " + output);
        }
        // Output format: "2048 SHA256:xxx comment (RSA)"
        //            or: "2048 MD5:xx:yy:... comment (RSA)"
        //            or: "256 xosos-... comment (ED25519)"  (bubble babble, no prefix)
        String[] tokens = output.split("\\s+");
        if (tokens.length < 2) {
            throw new IOException("Unexpected ssh-keygen output: " + output);
        }
        return tokens[1];
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        f.delete();
    }
}
