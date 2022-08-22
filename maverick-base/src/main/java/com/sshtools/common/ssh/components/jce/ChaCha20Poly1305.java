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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.generators.Poly1305KeyGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.AbstractSshCipher;
import com.sshtools.common.ssh.components.SshCipherFactory;
import com.sshtools.common.ssh.components.jce.ChaCha20Poly1305.ChaCha20.WrongKeySizeException;
import com.sshtools.common.ssh.components.jce.ChaCha20Poly1305.ChaCha20.WrongNonceSizeException;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger64;

public class ChaCha20Poly1305 extends AbstractSshCipher {
	
	private static final String CIPHER = "chacha20-poly1305@openssh.com";

	public static class ChaCha20Poly1305Factory implements SshCipherFactory<ChaCha20Poly1305> {

		@Override
		public ChaCha20Poly1305 create() throws NoSuchAlgorithmException, IOException {
			return new ChaCha20Poly1305();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}
	}
	
	byte[] k1 = new byte[32];
	byte[] k2 = new byte[32];
	int mode;
	UnsignedInteger64 currentSequenceNo;
	
	public ChaCha20Poly1305()
			throws IOException {
		super(CIPHER, SecurityLevel.PARANOID, 4000);
	}

	public void init(int mode, byte[] iv, byte[] keydata) throws java.io.IOException {

		this.mode = mode;
        // Create the packet length key
        System.arraycopy(keydata, 0, k2, 0, k2.length);
        System.arraycopy(keydata, 32, k1, 0, k1.length);

	}
	
	@Override
	public int getBlockSize() {
		return 8;
	}

	@Override
	public int getKeyLength() {
		return 64;
	}
	
	@Override
	public int getMacLength() {
		return 16;
	}
	
	@Override
	public boolean isMAC() {
		return true;
	}

	@Override
	public void transform(byte[] src, int start, byte[] dest, int offset, int len) throws IOException {
		
		try {
			if(mode==DECRYPT_MODE) {
				doDecrypt(src, start, dest, offset, len);
			} else {
				doEncrypt(src, start, dest, offset, len);
			}
		} catch (WrongKeySizeException | WrongNonceSizeException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private void doEncrypt(byte[] src, int start, byte[] dest, int offset, int len) throws WrongKeySizeException, WrongNonceSizeException, IllegalStateException, IOException {
		
		
		int payloadLength = 4 + len - 16;
		
		transformPayload(src, start, dest, offset, payloadLength);
		
		byte[] polykey = generatePoly1305Key();
		Poly1305 mac = new Poly1305();
		mac.init(polykey);
		mac.update(src, 0, payloadLength);
	
		byte[] expectedTag = generatePoly1305Tag(polykey, src, 0, payloadLength);
		
		System.arraycopy(expectedTag, 0, dest, payloadLength, expectedTag.length);

		
	}

	private void doDecrypt(byte[] src, int start, byte[] dest, int offset, int len) throws WrongKeySizeException, WrongNonceSizeException, IllegalStateException, IOException {
		
		byte[] tag = new byte[16];
		int payloadLength = 4 + len - 16;
		System.arraycopy(src, payloadLength, tag, 0 ,16);
			
		byte[] polykey = generatePoly1305Key();
		
		byte[] expectedTag = generatePoly1305Tag(polykey, src, 0, payloadLength);

		if(!Arrays.areEqual(tag, expectedTag)) {
			throw new IOException("Corrupt authentication tag");
		}
		
		transformPayload(src, start, dest, offset, len);
		
	}

	private void transformPayload(byte[] src, int start, byte[] dst, int off, int len) throws WrongKeySizeException, WrongNonceSizeException {
		
		ChaCha20 cha = new ChaCha20(k2, currentSequenceNo.toByteArray(), 1);
		cha.encrypt(dst, off, src, start, len);
		
	}

	private byte[] generatePoly1305Key() throws WrongKeySizeException, WrongNonceSizeException {
		
		byte[] polykey = new byte[k2.length];
		
		ChaCha20 cha = new ChaCha20(k2, currentSequenceNo.toByteArray(), 0);
		cha.encrypt(polykey, 0, polykey, 0, polykey.length);
		
		return polykey;
	}
	
	private byte[] generatePoly1305Tag(byte[] polykey, byte[] src, int off, int len) throws IllegalStateException, IOException {
		Poly1305 mac = new Poly1305();
		mac.init(polykey);
		mac.update(src, off, len);
		
		byte[] expectedTag = new byte[16];
		mac.doFinal(expectedTag, 0);
		
		return expectedTag;
	}

	@Override
	public String getProviderName() {
		return "JADAPTIVE";
	}

	public long readPacketLength(byte[] encoded, UnsignedInteger64 sequenceNo) throws IOException {
		
		try {
			
			this.currentSequenceNo = sequenceNo;
			
			ChaCha20 cha = new ChaCha20(k1, sequenceNo.toByteArray(), 0);
			byte[] tmp = new byte[4];
			
			cha.encrypt(tmp, 0, encoded, 0, 4);
			return ByteArrayReader.readInt(tmp, 0);
			
		} catch (WrongKeySizeException | WrongNonceSizeException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
	}
	
	public byte[] writePacketLength(int length, UnsignedInteger64 sequenceNo) throws IOException {
		
		try {
			
			this.currentSequenceNo = sequenceNo;
			
			ChaCha20 cha = new ChaCha20(k1, sequenceNo.toByteArray(), 0);
			byte[] tmp = new byte[4];
			
			cha.encrypt(tmp, 0, ByteArrayWriter.encodeInt(length), 0, 4);

			return tmp;
			
		} catch(WrongKeySizeException | WrongNonceSizeException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
	}
	
    protected static int littleEndianToInt(byte[] bs, int i) {
        return (bs[i] & 0xff) | ((bs[i + 1] & 0xff) << 8) | ((bs[i + 2] & 0xff) << 16) | ((bs[i + 3] & 0xff) << 24);
    }

    protected static void intToLittleEndian(int n, byte[] bs, int off) {
        bs[  off] = (byte)(n       );
        bs[++off] = (byte)(n >>>  8);
        bs[++off] = (byte)(n >>> 16);
        bs[++off] = (byte)(n >>> 24);
    }
    
	/*
	 * Quick-n-dirty standalone implementation of ChaCha 256-bit
	 * <p/>
	 * Created by Clarence Ho on 20150729
	 * <p/>
	 * References:
	 * ~ http://cr.yp.to/chacha/chacha-20080128.pdf
	 * ~ https://tools.ietf.org/html/draft-irtf-cfrg-chacha20-poly1305-01
	 * ~ https://github.com/quartzjer/chacha20
	 * ~ https://github.com/jotcmd/chacha20
	 */
	public static class ChaCha20 {
	    
	    /*
	     * Key size in byte
	     */
	    public static final int KEY_SIZE = 32;
	    
	    /*
	     * Nonce size in byte (reference implementation)
	     */
	    public static final int NONCE_SIZE_REF = 8;

	    /*
	     * Nonce size in byte (IETF draft)
	     */
	    public static final int NONCE_SIZE_IETF = 12;

	    private int[] matrix = new int[16];

	    
	    protected int ROTATE(int v, int c) {
	        return (v << c) | (v >>> (32 - c));
	    }
	    
	    protected void quarterRound(int[] x, int a, int b, int c, int d) {
	        x[a] += x[b];
	        x[d] = ROTATE(x[d] ^ x[a], 16);
	        x[c] += x[d];
	        x[b] = ROTATE(x[b] ^ x[c], 12);
	        x[a] += x[b];
	        x[d] = ROTATE(x[d] ^ x[a], 8);
	        x[c] += x[d];
	        x[b] = ROTATE(x[b] ^ x[c], 7);
	    }
	    
	    public class WrongNonceSizeException extends Exception {
	        private static final long serialVersionUID = 2687731889587117531L;
	    }
	    
	    public class WrongKeySizeException extends Exception {
	        private static final long serialVersionUID = -290509589749955895L;
	    }

	    
	    public ChaCha20(byte[] key, byte[] nonce, int counter)
	            throws WrongKeySizeException, WrongNonceSizeException {

	        if (key.length != KEY_SIZE) {
	            throw new WrongKeySizeException();
	        }
	        
	        this.matrix[ 0] = 0x61707865;
	        this.matrix[ 1] = 0x3320646e;
	        this.matrix[ 2] = 0x79622d32;
	        this.matrix[ 3] = 0x6b206574;
	        this.matrix[ 4] = littleEndianToInt(key, 0);
	        this.matrix[ 5] = littleEndianToInt(key, 4);
	        this.matrix[ 6] = littleEndianToInt(key, 8);
	        this.matrix[ 7] = littleEndianToInt(key, 12);
	        this.matrix[ 8] = littleEndianToInt(key, 16);
	        this.matrix[ 9] = littleEndianToInt(key, 20);
	        this.matrix[10] = littleEndianToInt(key, 24);
	        this.matrix[11] = littleEndianToInt(key, 28);
	        
	        if (nonce.length == NONCE_SIZE_REF) {        // reference implementation
	            this.matrix[12] = counter;
	            this.matrix[13] = 0;
	            this.matrix[14] = littleEndianToInt(nonce, 0);
	            this.matrix[15] = littleEndianToInt(nonce, 4);

	        } else if (nonce.length == NONCE_SIZE_IETF) {
	            this.matrix[12] = counter;
	            this.matrix[13] = littleEndianToInt(nonce, 0);
	            this.matrix[14] = littleEndianToInt(nonce, 4);
	            this.matrix[15] = littleEndianToInt(nonce, 8);
	        } else {
	            throw new WrongNonceSizeException();
	        }
	    }
	    
	    public void encrypt(byte[] dst, int doff, byte[] src, int soff, int len) {
	        int[] x = new int[16];
	        byte[] output = new byte[64];
	        int i, dpos = 0, spos = 0;

	        while (len > 0) {
	            for (i = 16; i-- > 0; ) x[i] = this.matrix[i];
	            for (i = 20; i > 0; i -= 2) {
	                quarterRound(x, 0, 4,  8, 12);
	                quarterRound(x, 1, 5,  9, 13);
	                quarterRound(x, 2, 6, 10, 14);
	                quarterRound(x, 3, 7, 11, 15);
	                quarterRound(x, 0, 5, 10, 15);
	                quarterRound(x, 1, 6, 11, 12);
	                quarterRound(x, 2, 7,  8, 13);
	                quarterRound(x, 3, 4,  9, 14);
	            }
	            for (i = 16; i-- > 0; ) x[i] += this.matrix[i];
	            for (i = 16; i-- > 0; ) intToLittleEndian(x[i], output, 4 * i);

	            // TODO: (1) check block count is 32-bit vs 64-bit; (2) java int is signed!
	            this.matrix[12] += 1;
	            if (this.matrix[12] <= 0) {
	                this.matrix[13] += 1;
	            }
	            if (len <= 64) {
	                for (i = len; i-- > 0; ) {
	                    dst[doff + i + dpos] = (byte) (src[soff + i + spos] ^ output[i]);
	                }
	                break;
	            }
	            for (i = 64; i-- > 0; ) {
	                dst[doff + i + dpos] = (byte) (src[soff + i + spos] ^ output[i]);
	            }
	            len -= 64;
	            spos += 64;
	            dpos += 64;
	        }
	
	    }
	 }
	

	
	/**
	 * Poly1305 message authentication code, designed by D. J. Bernstein.
	 * <p>
	 * Poly1305 computes a 128-bit (16 bytes) authenticator, using a 128 bit nonce and a 256 bit key
	 * consisting of a 128 bit key applied to an underlying cipher, and a 128 bit key (with 106
	 * effective key bits) used in the authenticator.
	 * <p>
	 * The polynomial calculation in this implementation is adapted from the public domain <a
	 * href="https://github.com/floodyberry/poly1305-donna">poly1305-donna-unrolled</a> C implementation
	 * by Andrew M (@floodyberry).
	 * @see Poly1305KeyGenerator
	 */
	public static class Poly1305
	{
	    private static final int BLOCK_SIZE = 16;

	    private final byte[] singleByte = new byte[1];

	    // Initialised state

	    /** Polynomial key */
	    private int r0, r1, r2, r3, r4;

	    /** Precomputed 5 * r[1..4] */
	    private int s1, s2, s3, s4;

	    /** Encrypted nonce */
	    private int k0, k1, k2, k3;

	    // Accumulating state

	    /** Current block of buffered input */
	    private final byte[] currentBlock = new byte[BLOCK_SIZE];

	    /** Current offset in input buffer */
	    private int currentBlockOffset = 0;

	    /** Polynomial accumulator */
	    private int h0, h1, h2, h3, h4;

	    /**
	     * Constructs a Poly1305 MAC, where the key passed to init() will be used directly.
	     */
	    public Poly1305()
	    {
	    }


	    /**
	     * Initialises the Poly1305 MAC.
	     * 
	     * @param params if used with a block cipher, then a {@link ParametersWithIV} containing a 128 bit
	     *        nonce and a {@link KeyParameter} with a 256 bit key complying to the
	     *        {@link Poly1305KeyGenerator Poly1305 key format}, otherwise just the
	     *        {@link KeyParameter}.
	     */
	    public void init(byte[] key)
	        throws IllegalArgumentException
	    {
	        setKey(key);
	        reset();
	    }

	    private void setKey(final byte[] key)
	    {
	        if (key.length != 32)
	        {
	            throw new IllegalArgumentException("Poly1305 key must be 256 bits.");
	        }

	        // Extract r portion of key (and "clamp" the values)
	        int t0 = littleEndianToInt(key, 0);
	        int t1 = littleEndianToInt(key, 4);
	        int t2 = littleEndianToInt(key, 8);
	        int t3 = littleEndianToInt(key, 12);

	        // NOTE: The masks perform the key "clamping" implicitly
	        r0 =   t0                       & 0x03FFFFFF;
	        r1 = ((t0 >>> 26) | (t1 <<  6)) & 0x03FFFF03;
	        r2 = ((t1 >>> 20) | (t2 << 12)) & 0x03FFC0FF;
	        r3 = ((t2 >>> 14) | (t3 << 18)) & 0x03F03FFF;
	        r4 =  (t3 >>>  8)               & 0x000FFFFF;

	        // Precompute multipliers
	        s1 = r1 * 5;
	        s2 = r2 * 5;
	        s3 = r3 * 5;
	        s4 = r4 * 5;

	        final byte[] kBytes;
	        final int kOff;

            kBytes = key;
            kOff = BLOCK_SIZE;

	        k0 = littleEndianToInt(kBytes, kOff + 0);
	        k1 = littleEndianToInt(kBytes, kOff + 4);
	        k2 = littleEndianToInt(kBytes, kOff + 8);
	        k3 = littleEndianToInt(kBytes, kOff + 12);
	    }

	    public String getAlgorithmName()
	    {
	        return "Poly1305";
	    }

	    public int getMacSize()
	    {
	        return BLOCK_SIZE;
	    }

	    public void update(final byte in)
	        throws IOException, IllegalStateException
	    {
	        singleByte[0] = in;
	        update(singleByte, 0, 1);
	    }

	    public void update(final byte[] in, final int inOff, final int len)
	        throws IOException,
	        IllegalStateException
	    {
	        int copied = 0;
	        while (len > copied)
	        {
	            if (currentBlockOffset == BLOCK_SIZE)
	            {
	                processBlock();
	                currentBlockOffset = 0;
	            }

	            int toCopy = Math.min((len - copied), BLOCK_SIZE - currentBlockOffset);
	            System.arraycopy(in, copied + inOff, currentBlock, currentBlockOffset, toCopy);
	            copied += toCopy;
	            currentBlockOffset += toCopy;
	        }

	    }

	    private void processBlock()
	    {
	        if (currentBlockOffset < BLOCK_SIZE)
	        {
	            currentBlock[currentBlockOffset] = 1;
	            for (int i = currentBlockOffset + 1; i < BLOCK_SIZE; i++)
	            {
	                currentBlock[i] = 0;
	            }
	        }

	        final long t0 = 0xffffffffL & littleEndianToInt(currentBlock, 0);
	        final long t1 = 0xffffffffL & littleEndianToInt(currentBlock, 4);
	        final long t2 = 0xffffffffL & littleEndianToInt(currentBlock, 8);
	        final long t3 = 0xffffffffL & littleEndianToInt(currentBlock, 12);

	        h0 += t0 & 0x3ffffff;
	        h1 += (((t1 << 32) | t0) >>> 26) & 0x3ffffff;
	        h2 += (((t2 << 32) | t1) >>> 20) & 0x3ffffff;
	        h3 += (((t3 << 32) | t2) >>> 14) & 0x3ffffff;
	        h4 += (t3 >>> 8);

	        if (currentBlockOffset == BLOCK_SIZE)
	        {
	            h4 += (1 << 24);
	        }

	        long tp0 = mul32x32_64(h0,r0) + mul32x32_64(h1,s4) + mul32x32_64(h2,s3) + mul32x32_64(h3,s2) + mul32x32_64(h4,s1);
	        long tp1 = mul32x32_64(h0,r1) + mul32x32_64(h1,r0) + mul32x32_64(h2,s4) + mul32x32_64(h3,s3) + mul32x32_64(h4,s2);
	        long tp2 = mul32x32_64(h0,r2) + mul32x32_64(h1,r1) + mul32x32_64(h2,r0) + mul32x32_64(h3,s4) + mul32x32_64(h4,s3);
	        long tp3 = mul32x32_64(h0,r3) + mul32x32_64(h1,r2) + mul32x32_64(h2,r1) + mul32x32_64(h3,r0) + mul32x32_64(h4,s4);
	        long tp4 = mul32x32_64(h0,r4) + mul32x32_64(h1,r3) + mul32x32_64(h2,r2) + mul32x32_64(h3,r1) + mul32x32_64(h4,r0);

	        h0 = (int)tp0 & 0x3ffffff; tp1 += (tp0 >>> 26);
	        h1 = (int)tp1 & 0x3ffffff; tp2 += (tp1 >>> 26);
	        h2 = (int)tp2 & 0x3ffffff; tp3 += (tp2 >>> 26);
	        h3 = (int)tp3 & 0x3ffffff; tp4 += (tp3 >>> 26);
	        h4 = (int)tp4 & 0x3ffffff;
	        h0 += (int)(tp4 >>> 26) * 5;
	        h1 += (h0 >>> 26); h0 &= 0x3ffffff;
	    }

	    public int doFinal(final byte[] out, final int outOff)
	        throws IOException,
	        IllegalStateException
	    {
	        if (outOff + BLOCK_SIZE > out.length)
	        {
	            throw new IOException("Output buffer is too short.");
	        }

	        if (currentBlockOffset > 0)
	        {
	            // Process padded final block
	            processBlock();
	        }

	        h1 += (h0 >>> 26); h0 &= 0x3ffffff;
	        h2 += (h1 >>> 26); h1 &= 0x3ffffff;
	        h3 += (h2 >>> 26); h2 &= 0x3ffffff;
	        h4 += (h3 >>> 26); h3 &= 0x3ffffff;
	        h0 += (h4 >>> 26) * 5; h4 &= 0x3ffffff;
	        h1 += (h0 >>> 26); h0 &= 0x3ffffff;

	        int g0, g1, g2, g3, g4, b;
	        g0 = h0 + 5; b = g0 >>> 26; g0 &= 0x3ffffff;
	        g1 = h1 + b; b = g1 >>> 26; g1 &= 0x3ffffff;
	        g2 = h2 + b; b = g2 >>> 26; g2 &= 0x3ffffff;
	        g3 = h3 + b; b = g3 >>> 26; g3 &= 0x3ffffff;
	        g4 = h4 + b - (1 << 26);

	        b = (g4 >>> 31) - 1;
	        int nb = ~b;
	        h0 = (h0 & nb) | (g0 & b);
	        h1 = (h1 & nb) | (g1 & b);
	        h2 = (h2 & nb) | (g2 & b);
	        h3 = (h3 & nb) | (g3 & b);
	        h4 = (h4 & nb) | (g4 & b);

	        long f0, f1, f2, f3;
	        f0 = (((h0       ) | (h1 << 26)) & 0xffffffffl) + (0xffffffffL & k0);
	        f1 = (((h1 >>> 6 ) | (h2 << 20)) & 0xffffffffl) + (0xffffffffL & k1);
	        f2 = (((h2 >>> 12) | (h3 << 14)) & 0xffffffffl) + (0xffffffffL & k2);
	        f3 = (((h3 >>> 18) | (h4 << 8 )) & 0xffffffffl) + (0xffffffffL & k3);

	        intToLittleEndian((int)f0, out, outOff);
	        f1 += (f0 >>> 32);
	        intToLittleEndian((int)f1, out, outOff + 4);
	        f2 += (f1 >>> 32);
	        intToLittleEndian((int)f2, out, outOff + 8);
	        f3 += (f2 >>> 32);
	        intToLittleEndian((int)f3, out, outOff + 12);

	        reset();
	        return BLOCK_SIZE;
	    }

	    public void reset()
	    {
	        currentBlockOffset = 0;

	        h0 = h1 = h2 = h3 = h4 = 0;
	    }
	    private static final long mul32x32_64(int i1, int i2)
	    {
	        return (i1 & 0xFFFFFFFFL) * i2;
	    }
	}

}
