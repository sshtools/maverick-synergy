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
package com.sshtools.common.ssh.components.jce;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshHmac;

/**
 * Abstract base class for UMAC message authentication implementations per
 * RFC 4418 and draft-miller-secsh-umac-01.
 *
 * UMAC is a fast MAC based on universal hashing. The SSH sequence number is
 * used as the nonce (8-byte big-endian uint64). The key is always 16 bytes
 * (AES-128). This is a pure Java implementation using JCE AES/ECB as the
 * underlying block cipher.
 */
public abstract class AbstractUmac implements SshHmac {

    // ---- RFC 4418 constants ----
    private static final int BLOCKLEN = 16;       // AES block size = 16 bytes
    private static final int KEYLEN   = 16;       // AES-128 key length
    private static final int L1_KEY_BYTES = 1024; // NH key length in bytes

    // prime(36)  = 2^36 - 5
    private static final long PRIME36 = 0xFFFFFFFBL; // 4294967291, but we use full 36-bit: 68719476731L
    private static final long P36 = 68719476731L;    // 2^36 - 5 (36-bit prime, used in L3)

    // prime(64)  = 2^64 - 59  (used as BigInteger for POLY-64 mod)
    private static final BigInteger P64  = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.valueOf(59));
    // prime(128) = 2^128 - 159 (used as BigInteger for POLY-128 mod)
    private static final BigInteger P128 = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.valueOf(159));

    // Masks for L2-HASH key clamping
    private static final BigInteger MASK64  = new BigInteger("01ffffff01ffffff", 16);
    private static final BigInteger MASK128 = new BigInteger("01ffffff01ffffff01ffffff01ffffff", 16);

    // ---- Instance state ----
    protected final int macLength;
    private final SecurityLevel securityLevel;
    private final int priority;

    // Derived per-key material (set in init())
    private byte[] l1Key;    // 1024 + (iters-1)*16 bytes
    private byte[] l2Key;    // iters * 24 bytes
    private byte[] l3Key1;   // iters * 64 bytes
    private byte[] l3Key2;   // iters * 4 bytes
    private byte[] pdfKey;   // 16 bytes (AES key for PDF)

    // Original 16-byte key retained for PDF cipher
    private byte[] masterKey;

    protected AbstractUmac(int macLength, SecurityLevel securityLevel, int priority) {
        this.macLength = macLength;
        this.securityLevel = securityLevel;
        this.priority = priority;
    }

    // =========================================================================
    // SshHmac interface
    // =========================================================================

    @Override
    public void init(byte[] keydata) throws SshException {
        try {
            masterKey = Arrays.copyOf(keydata, KEYLEN);
            int iters = macLength / 4;
            l1Key  = kdf(masterKey, 1, L1_KEY_BYTES + (iters - 1) * KEYLEN);
            l2Key  = kdf(masterKey, 2, iters * 24);
            l3Key1 = kdf(masterKey, 3, iters * 64);
            l3Key2 = kdf(masterKey, 4, iters * 4);
            pdfKey = kdf(masterKey, 0, KEYLEN);
        } catch (GeneralSecurityException e) {
            throw new SshException(e);
        }
    }

    @Override
    public void generate(long sequenceNo, byte[] data, int offset, int len, byte[] output, int start) {
        try {
            byte[] msg = Arrays.copyOfRange(data, offset, offset + len);
            byte[] hash = uhash(msg);
            byte[] pad  = pdf(sequenceNo);
            for (int i = 0; i < macLength; i++) {
                output[start + i] = (byte) (hash[i] ^ pad[i]);
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("UMAC generate failed", e);
        }
    }

    @Override
    public boolean verify(long sequenceNo, byte[] data, int start, int len, byte[] mac, int offset) {
        byte[] generated = new byte[macLength];
        generate(sequenceNo, data, start, len, generated, 0);
        int diff = 0;
        for (int i = 0; i < macLength; i++) {
            diff |= (mac[offset + i] ^ generated[i]);
        }
        return diff == 0;
    }

    @Override
    public void update(byte[] b) {
        // Not used in UMAC (stateless per-packet MAC)
    }

    @Override
    public byte[] doFinal() {
        return new byte[macLength];
    }

    @Override
    public int getMacSize() {
        return KEYLEN;
    }

    @Override
    public int getMacLength() {
        return macLength;
    }

    @Override
    public boolean isETM() {
        return false;
    }

    @Override
    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    // =========================================================================
    // RFC 4418 KDF
    // =========================================================================

    /**
     * KDF(K, index, numbytes) — Section 5.1 of RFC 4418.
     * Generates pseudo-random key material by AES-encrypting counter blocks.
     */
    private static byte[] kdf(byte[] k, int index, int numbytes) throws GeneralSecurityException {
        Cipher aes = aesCipher(k);
        int n = (numbytes + BLOCKLEN - 1) / BLOCKLEN;
        byte[] output = new byte[n * BLOCKLEN];
        byte[] block = new byte[BLOCKLEN];
        for (int i = 1; i <= n; i++) {
            // T = uint2str(index, BLOCKLEN-8) || uint2str(i, 8)
            // First 8 bytes = index as big-endian int64
            writeUint64(block, 0, index);
            // Last 8 bytes = i as big-endian int64
            writeUint64(block, 8, i);
            byte[] enc = aes.doFinal(block);
            System.arraycopy(enc, 0, output, (i - 1) * BLOCKLEN, BLOCKLEN);
        }
        return Arrays.copyOf(output, numbytes);
    }

    // =========================================================================
    // RFC 4418 PDF (Pad Derivation Function)
    // =========================================================================

    /**
     * PDF(K, Nonce, taglen) — Section 5.2 of RFC 4418.
     * Derives the pad that is XOR'd with UHASH output to form the tag.
     * The nonce is the SSH sequence number encoded as uint64 (8 bytes).
     */
    private byte[] pdf(long sequenceNo) throws GeneralSecurityException {
        byte[] nonce = toUint64(sequenceNo); // 8 bytes

        int index = 0;
        if (macLength == 4 || macLength == 8) {
            // index = str2uint(Nonce) mod (BLOCKLEN / taglen)
            long nonceVal = readUint64(nonce, 0);
            long modulus = BLOCKLEN / macLength;
            // Use unsigned mod
            index = (int) Long.remainderUnsigned(nonceVal, modulus);
            // Nonce = Nonce XOR uint2str(index, 8)
            byte[] indexBytes = toUint64(index);
            for (int i = 0; i < 8; i++) {
                nonce[i] ^= indexBytes[i];
            }
        }

        // Nonce = Nonce || zeroes(BLOCKLEN - 8)  → pad to 16 bytes
        byte[] noncePadded = new byte[BLOCKLEN];
        System.arraycopy(nonce, 0, noncePadded, 0, 8);

        Cipher aes = aesCipher(pdfKey);
        byte[] t = aes.doFinal(noncePadded);

        if (macLength == 4 || macLength == 8) {
            return Arrays.copyOfRange(t, index * macLength, (index + 1) * macLength);
        } else {
            return Arrays.copyOf(t, macLength);
        }
    }

    // =========================================================================
    // RFC 4418 UHASH
    // =========================================================================

    /**
     * UHASH(K, M, taglen) — Section 5.3 of RFC 4418.
     * Produces taglen bytes of hash output using L1/L2/L3 hash layers.
     */
    private byte[] uhash(byte[] m) {
        int iters = macLength / 4;
        byte[] y = new byte[macLength]; // iters * 4 bytes

        for (int i = 0; i < iters; i++) {
            // Extract per-iteration sub-keys
            // L1Key_i: starts at (i)*16 within l1Key, length 1024
            byte[] l1KeyI = Arrays.copyOfRange(l1Key, i * KEYLEN, i * KEYLEN + L1_KEY_BYTES);
            byte[] l2KeyI = Arrays.copyOfRange(l2Key, i * 24, (i + 1) * 24);
            byte[] l3Key1I = Arrays.copyOfRange(l3Key1, i * 64, (i + 1) * 64);
            byte[] l3Key2I = Arrays.copyOfRange(l3Key2, i * 4, (i + 1) * 4);

            byte[] a = l1Hash(l1KeyI, m);

            byte[] b;
            // If message fits in one L1 block (≤ 1024 bytes), skip L2
            if (m.length <= L1_KEY_BYTES) {
                // B = zeroes(8) || A
                b = new byte[16];
                System.arraycopy(a, 0, b, 8, 8);
            } else {
                b = l2Hash(l2KeyI, a);
            }

            byte[] c = l3Hash(l3Key1I, l3Key2I, b);
            System.arraycopy(c, 0, y, i * 4, 4);
        }
        return y;
    }

    // =========================================================================
    // L1-HASH / NH
    // =========================================================================

    /**
     * L1-HASH(K, M) — Section 5.3.1 of RFC 4418.
     * Splits M into 1024-byte (8192-bit) chunks, applies NH to each, returns
     * an 8 * ceil(bitlen(M)/8192) byte result (minimum 8 bytes).
     */
    private static byte[] l1Hash(byte[] k, byte[] m) {
        // Number of 1024-byte blocks (at least 1 even for empty message)
        int mlen = m.length;
        // t = max(ceil(bitlength(M) / 8192), 1) = max(ceil(mlen / 1024), 1)
        int t = Math.max(1, (mlen + 1023) / 1024);
        // Each NH call on 8192-bit (1024-byte) chunk produces 8 bytes
        // Last block may be shorter and is zero-padded to 32 bytes
        byte[] y = new byte[t * 8];

        for (int blk = 0; blk < t; blk++) {
            int blockStart = blk * 1024;
            int blockEnd = Math.min(blockStart + 1024, mlen);
            byte[] chunk;
            if (blk < t - 1) {
                // Full 1024-byte block
                chunk = Arrays.copyOfRange(m, blockStart, blockEnd);
            } else {
                // Last block — zero-pad to multiple of 32 bytes
                chunk = Arrays.copyOfRange(m, blockStart, blockEnd);
            }

            // Compute bit-length of this chunk (for the Len field)
            long bitLen = (long) chunk.length * 8;
            if (blk < t - 1) {
                bitLen = 8192; // full block
            }

            // Zero-pad to multiple of 32
            int paddedLen = ((chunk.length + 31) / 32) * 32;
            if (paddedLen == 0) paddedLen = 32;
            byte[] paddedChunk = Arrays.copyOf(chunk, paddedLen);

            // ENDIAN-SWAP: byte-reverse each 4-byte word
            endianSwap(paddedChunk);

            long nhResult = nh(k, paddedChunk);

            // Y_blk = NH(K, M_blk) +_64 Len
            long wordLen = bitLen;
            long sum = nhResult + wordLen; // mod 2^64 (natural Java long overflow)

            writeUint64(y, blk * 8, sum);
        }
        return y;
    }

    /**
     * NH(K, M) — Section 5.3.1 of RFC 4418.
     * M must be a multiple of 32 bytes. Returns a 64-bit value as long.
     */
    private static long nh(byte[] k, byte[] m) {
        int t = m.length / 4;   // number of 32-bit words in M
        long y = 0L;
        // Process 8 words at a time (2 groups of 4)
        int i = 0;
        while (i + 7 < t) {
            long m0 = readUint32(m, i * 4);
            long m1 = readUint32(m, (i + 1) * 4);
            long m2 = readUint32(m, (i + 2) * 4);
            long m3 = readUint32(m, (i + 3) * 4);
            long m4 = readUint32(m, (i + 4) * 4);
            long m5 = readUint32(m, (i + 5) * 4);
            long m6 = readUint32(m, (i + 6) * 4);
            long m7 = readUint32(m, (i + 7) * 4);

            long k0 = readUint32(k, i * 4);
            long k1 = readUint32(k, (i + 1) * 4);
            long k2 = readUint32(k, (i + 2) * 4);
            long k3 = readUint32(k, (i + 3) * 4);
            long k4 = readUint32(k, (i + 4) * 4);
            long k5 = readUint32(k, (i + 5) * 4);
            long k6 = readUint32(k, (i + 6) * 4);
            long k7 = readUint32(k, (i + 7) * 4);

            // +_32 is mod 2^32 (Java int addition then zero-extend to long)
            y += ((m0 + k0) & 0xFFFFFFFFL) * ((m4 + k4) & 0xFFFFFFFFL);
            y += ((m1 + k1) & 0xFFFFFFFFL) * ((m5 + k5) & 0xFFFFFFFFL);
            y += ((m2 + k2) & 0xFFFFFFFFL) * ((m6 + k6) & 0xFFFFFFFFL);
            y += ((m3 + k3) & 0xFFFFFFFFL) * ((m7 + k7) & 0xFFFFFFFFL);
            i += 8;
        }
        return y;
    }

    /**
     * ENDIAN-SWAP: byte-reverse each 4-byte word in the array (in-place).
     */
    private static void endianSwap(byte[] data) {
        for (int i = 0; i + 3 < data.length; i += 4) {
            byte tmp = data[i];
            data[i] = data[i + 3];
            data[i + 3] = tmp;
            tmp = data[i + 1];
            data[i + 1] = data[i + 2];
            data[i + 2] = tmp;
        }
    }

    // =========================================================================
    // L2-HASH / POLY
    // =========================================================================

    /**
     * L2-HASH(K, M) — Section 5.3.2 of RFC 4418.
     * K is 24 bytes. M is the L1-HASH output. Returns 16 bytes.
     */
    private static byte[] l2Hash(byte[] k, byte[] m) {
        // Clamp keys
        BigInteger k64  = toBigInteger(k, 0, 8).and(MASK64);
        BigInteger k128 = toBigInteger(k, 8, 16).and(MASK128);

        BigInteger y;
        if (m.length <= (1 << 17)) {
            // Short: POLY(64, 2^64-2^32, k64, M)
            BigInteger maxword = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE.shiftLeft(32));
            y = poly(64, maxword, k64, m);
        } else {
            // Long: split into M_1 (first 2^17 bytes) and M_2 (rest)
            byte[] m1 = Arrays.copyOf(m, 1 << 17);
            byte[] m2tail = Arrays.copyOfRange(m, 1 << 17, m.length);
            // M_2 = zeropad(M_2 || {0x80}, 16)
            byte[] m2pre = Arrays.copyOf(m2tail, m2tail.length + 1);
            m2pre[m2tail.length] = (byte) 0x80;
            int padLen = ((m2pre.length + 15) / 16) * 16;
            byte[] m2 = Arrays.copyOf(m2pre, padLen);

            BigInteger maxword64  = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE.shiftLeft(32));
            BigInteger maxword128 = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE.shiftLeft(96));

            y = poly(64, maxword64, k64, m1);
            // Prepend y as uint128 (big-endian 16 bytes) to M_2
            byte[] yBytes = toBytesBigEndian(y, 16);
            byte[] m2full = new byte[16 + m2.length];
            System.arraycopy(yBytes, 0, m2full, 0, 16);
            System.arraycopy(m2, 0, m2full, 16, m2.length);
            y = poly(128, maxword128, k128, m2full);
        }

        return toBytesBigEndian(y, 16);
    }

    /**
     * POLY(wordbits, maxwordrange, k, M) — Section 5.3.2 of RFC 4418.
     * wordbits is 64 or 128.
     */
    private static BigInteger poly(int wordbits, BigInteger maxwordrange, BigInteger k, byte[] m) {
        BigInteger p = (wordbits == 64) ? P64 : P128;
        int wordBytes = wordbits / 8;
        BigInteger offset = BigInteger.ONE.shiftLeft(wordbits).subtract(p); // 2^wordbits - p
        BigInteger marker = p.subtract(BigInteger.ONE);

        BigInteger y = BigInteger.ONE;
        int words = m.length / wordBytes;
        for (int i = 0; i < words; i++) {
            BigInteger mi = toBigInteger(m, i * wordBytes, wordBytes);
            if (mi.compareTo(maxwordrange) >= 0) {
                y = k.multiply(y).add(marker).mod(p);
                y = k.multiply(y).add(mi.subtract(offset)).mod(p);
            } else {
                y = k.multiply(y).add(mi).mod(p);
            }
        }
        return y;
    }

    // =========================================================================
    // L3-HASH
    // =========================================================================

    /**
     * L3-HASH(K1, K2, M) — Section 5.3.3 of RFC 4418.
     * K1 is 64 bytes, K2 is 4 bytes, M is 16 bytes. Returns 4 bytes.
     */
    private static byte[] l3Hash(byte[] k1, byte[] k2, byte[] m) {
        long y = 0;
        for (int i = 0; i < 8; i++) {
            // m_i: 2 bytes (16 bits) from M
            long mi = ((m[i * 2] & 0xFFL) << 8) | (m[i * 2 + 1] & 0xFFL);
            // k_i: 8 bytes from K1, mod prime(36)
            long ki = Long.remainderUnsigned(readUint64(k1, i * 8), P36);
            y += mi * ki;
        }
        y = y % P36;
        y = y & 0xFFFFFFFFL; // mod 2^32

        byte[] result = new byte[4];
        result[0] = (byte) ((y >> 24) & 0xFF);
        result[1] = (byte) ((y >> 16) & 0xFF);
        result[2] = (byte) ((y >> 8) & 0xFF);
        result[3] = (byte) (y & 0xFF);

        // XOR with K2
        for (int i = 0; i < 4; i++) {
            result[i] ^= k2[i];
        }
        return result;
    }

    // =========================================================================
    // AES helper
    // =========================================================================

    private static Cipher aesCipher(byte[] key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher;
    }

    // =========================================================================
    // Primitive helpers
    // =========================================================================

    private static byte[] toUint64(long value) {
        byte[] b = new byte[8];
        writeUint64(b, 0, value);
        return b;
    }

    private static void writeUint64(byte[] b, int off, long value) {
        b[off]     = (byte) (value >> 56);
        b[off + 1] = (byte) (value >> 48);
        b[off + 2] = (byte) (value >> 40);
        b[off + 3] = (byte) (value >> 32);
        b[off + 4] = (byte) (value >> 24);
        b[off + 5] = (byte) (value >> 16);
        b[off + 6] = (byte) (value >> 8);
        b[off + 7] = (byte)  value;
    }

    /** Read big-endian uint64 from byte array as a signed Java long (bit-identical). */
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

    /** Read big-endian uint64 from byte array — interpreting as positive long (for mod arithmetic). */
    private static long readUint64AsLong(byte[] b, int off) {
        return readUint64(b, off);
    }

    /** Read big-endian uint32 as unsigned long. */
    private static long readUint32(byte[] b, int off) {
        return ((b[off]     & 0xFFL) << 24)
             | ((b[off + 1] & 0xFFL) << 16)
             | ((b[off + 2] & 0xFFL) << 8)
             |  (b[off + 3] & 0xFFL);
    }

    /** Convert big-endian bytes[off..off+len-1] to a positive BigInteger. */
    private static BigInteger toBigInteger(byte[] b, int off, int len) {
        byte[] tmp = Arrays.copyOfRange(b, off, off + len);
        return new BigInteger(1, tmp);
    }

    /** Convert BigInteger to big-endian bytes of exactly `len` bytes (zero-padded or truncated). */
    private static byte[] toBytesBigEndian(BigInteger v, int len) {
        byte[] raw = v.toByteArray();
        if (raw.length == len) return raw;
        byte[] out = new byte[len];
        if (raw.length > len) {
            // Drop leading sign byte(s)
            System.arraycopy(raw, raw.length - len, out, 0, len);
        } else {
            // Zero-pad on the left
            System.arraycopy(raw, 0, out, len - raw.length, raw.length);
        }
        return out;
    }
}
