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
package com.sshtools.common.tests;

import org.junit.Ignore;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshHmac;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

import junit.framework.TestCase;

@Ignore
public abstract class AbstractHmacTests extends TestCase {

    protected abstract String getTestingJCE();

    protected void testHmac(SshHmac hmac) throws SshException {
        byte[] key = new byte[1024];
        JCEComponentManager.getSecureRandom().nextBytes(key);
        hmac.init(key);

        byte[] data = new byte[256];
        JCEComponentManager.getSecureRandom().nextBytes(data);
        byte[] mac = new byte[hmac.getMacLength()];

        for (long seqNo = 0; seqNo < 10000; seqNo++) {
            hmac.generate(seqNo, data, 0, data.length, mac, 0);
            assertTrue("MAC verify failed for " + hmac.getAlgorithm() + " at seqNo=" + seqNo,
                    hmac.verify(seqNo, data, 0, data.length, mac, 0));
        }
    }

    /**
     * Verify an RFC 4418 Appendix test vector for a UMAC instance.
     *
     * All vectors use K = "abcdefghijklmnop" (16 bytes, ASCII) and
     * N = "bcdefghi" (8 bytes ASCII, treated as a big-endian uint64 nonce).
     *
     * The nonce is passed as the SSH sequence number via generate().
     *
     * @param hmac        the UMAC instance to test (already specifies taglen via getMacLength())
     * @param message     the plaintext bytes (pass null for the empty-message vector)
     * @param expectedHex expected tag as upper-case hex string
     */
    protected void testRfcVector(SshHmac hmac, byte[] message, String expectedHex) throws Exception {
        byte[] key = "abcdefghijklmnop".getBytes("ASCII");
        // RFC nonce "bcdefghi" as a big-endian uint64
        long seqNo = readUint64("bcdefghi".getBytes("ASCII"), 0);
        hmac.init(key);
        if (message == null) message = new byte[0];
        byte[] output = new byte[hmac.getMacLength()];
        hmac.generate(seqNo, message, 0, message.length, output, 0);
        assertEquals("RFC vector mismatch for " + hmac.getAlgorithm(), expectedHex, bytesToHex(output));
    }

    private static long readUint64(byte[] b, int off) {
        return ((b[off]     & 0xFFL) << 56)
             | ((b[off + 1] & 0xFFL) << 48)
             | ((b[off + 2] & 0xFFL) << 40)
             | ((b[off + 3] & 0xFFL) << 32)
             | ((b[off + 4] & 0xFFL) << 24)
             | ((b[off + 5] & 0xFFL) << 16)
             | ((b[off + 6] & 0xFFL) << 8)
             |  (b[off + 7] & 0xFFL);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    /** Helper: create a byte array with the given byte value repeated n times. */
    protected static byte[] repeat(byte value, int n) {
        byte[] b = new byte[n];
        java.util.Arrays.fill(b, value);
        return b;
    }

    /** Helper: repeat a byte pattern n times. */
    protected static byte[] repeat(byte[] pattern, int times) {
        byte[] b = new byte[pattern.length * times];
        for (int i = 0; i < times; i++) {
            System.arraycopy(pattern, 0, b, i * pattern.length, pattern.length);
        }
        return b;
    }
}
