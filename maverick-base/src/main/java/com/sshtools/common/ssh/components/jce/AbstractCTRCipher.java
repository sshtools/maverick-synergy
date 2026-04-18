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

/*-
 * #%L
 * Base API
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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * <p>An abstract base class for CTR (Counter) mode ciphers that properly handles
 * counter state and JCE specification compliance by calling doFinal for each 
 * transform operation.</p>
 * 
 * <p>CTR mode is a stream cipher that maintains a counter as the IV. This class
 * tracks the number of blocks processed and increments the counter appropriately,
 * then reinitializes the cipher for each transform operation to handle JCE providers
 * that buffer data.</p>
 * 
 * <p>Set system property "maverick.forceNewCTRMode" to "true" to force cipher
 * reinitialization after every transform for testing purposes.</p>
 *
 * @author Lee David Painter
 */
public abstract class AbstractCTRCipher extends AbstractJCECipher {

	private static final boolean FORCE_REINIT = Boolean.getBoolean("maverick.forceNewCTRMode");
	
	private byte[] currentCounter;
	private long blocksProcessed;
	private int mode;
	private SecretKeySpec keySpec;
	
	/**
	 * Constructor for CTR cipher implementations.
	 * 
	 * @param spec the value passed into Cipher.getInstance() that specifies the
	 * specification of the cipher; for example "AES/CTR/NoPadding"
	 * @param keyspec the value passed into the constructor of SecretKeySpec.
	 * @param keylength the length in bytes of the key
	 * @param algorithm the SSH algorithm name
	 * @param securityLevel the security level of this cipher
	 * @param priority the priority for algorithm selection
	 * @throws IOException if cipher initialization fails
	 */
	public AbstractCTRCipher(String spec, String keyspec, int keylength, 
			String algorithm, SecurityLevel securityLevel, int priority) throws IOException {
		super(spec, keyspec, keylength, algorithm, securityLevel, priority);
	}

	@Override
	public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
		this.mode = mode;
		this.blocksProcessed = 0;
		
		// Create the key
		byte[] actualKey = new byte[keylength];
		System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);
		this.keySpec = new SecretKeySpec(actualKey, keyspec);
		
		// Initialize the counter (IV) for the first packet
		this.currentCounter = new byte[getBlockSize()];
		System.arraycopy(iv, 0, currentCounter, 0, currentCounter.length);
		
		// Initialize cipher with the counter
		initializeCipher();
	}

	/**
	 * Initialize or reinitialize the cipher with the current counter.
	 * 
	 * @throws IOException if cipher initialization fails
	 */
	private void initializeCipher() throws IOException {
		try {
			cipher.init(
				(mode == ENCRYPT_MODE) ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE,
				keySpec, 
				new IvParameterSpec(currentCounter, 0, getBlockSize())
			);
		} catch (InvalidKeyException ike) {
			throw new IOException("Invalid encryption key", ike);
		} catch (InvalidAlgorithmParameterException ape) {
			throw new IOException("Invalid algorithm parameter", ape);
		}
	}

	@Override
	public void transform(byte[] buf, int start, byte[] output, int off, int len) throws IOException {
		if (len > 0) {
			
			if (buf.length - start < len) {
				throw new IllegalStateException("Input buffer of " + buf.length 
						+ " bytes is too small for requested transform length " + len);
			}
			if (output.length - off < len) {
				throw new IllegalStateException("Output buffer of " + output.length 
						+ " bytes is too small for requested transform length " + len);
			}
			
			// Track how many blocks we'll process in this call
			int blockSize = getBlockSize();
			long blocksInThisCall = len / blockSize;
			
			try {
				// First try update() - this is the standard operation
				int stored = cipher.update(buf, start, len, output, off);
				
				// If the cipher buffered data (returned less than input), call doFinal
				// This handles JCE providers that cache data
				// Also call doFinal if FORCE_REINIT is enabled for testing
				if (stored < len || FORCE_REINIT) {
					if (stored < len) {
						stored += cipher.doFinal(output, off + stored);
						if (stored != len) {
							throw new IOException("Cipher did not return all bytes: " 
									+ stored + " != " + len);
						}
					} else if (FORCE_REINIT) {
						// Force mode: call doFinal even though update returned all bytes
						cipher.doFinal();
					}
					
					// Update counter for next packet and reinitialize cipher
					// This is needed after doFinal as it resets the cipher state
					blocksProcessed += blocksInThisCall;
					updateCounter();
					initializeCipher();
					blocksProcessed = 0; // Reset for next cycle
				} else {
					// If update() returned all data, just track blocks processed
					// Counter will be updated on next transform when doFinal is called
					blocksProcessed += blocksInThisCall;
				}
				
			} catch (IllegalBlockSizeException e) {
				throw new IOException("Illegal block size in CTR cipher transform", e);
			} catch (BadPaddingException e) {
				throw new IOException("Bad padding in CTR cipher transform", e);
			} catch (javax.crypto.ShortBufferException e) {
				throw new IOException("Short buffer in CTR cipher transform", e);
			}
		}
	}

	/**
	 * Update the counter by adding the number of blocks processed.
	 * The counter is treated as a big-endian unsigned integer stored in the last 8 bytes
	 * of the IV, with carry propagating to earlier bytes if needed.
	 */
	private void updateCounter() {
		int blockSize = getBlockSize();
		
		// Treat the last 8 bytes of the counter as a 64-bit big-endian unsigned integer
		// Extract current value
		long counterValue = 0;
		for (int i = blockSize - 8; i < blockSize; i++) {
			counterValue = (counterValue << 8) | (currentCounter[i] & 0xFF);
		}
		
		// Get the high bit before addition to detect overflow
		long highBitBefore = counterValue & 0x8000000000000000L;
		
		// Clear high bit, add blocks processed
		counterValue = (counterValue & 0x7FFFFFFFFFFFFFFFL) + blocksProcessed;
		
		// Get the high bit after addition
		long highBitAfter = counterValue & 0x8000000000000000L;
		
		// XOR to determine if we need to preserve the sign bit change
		counterValue = (counterValue & 0x7FFFFFFFFFFFFFFFL) | (highBitBefore ^ highBitAfter);
		
		// Detect carry (both bits were set)
		int carry = ((highBitBefore & highBitAfter) != 0) ? 1 : 0;
		
		// Write back the counter value (last 8 bytes)
		for (int i = blockSize - 1; i >= blockSize - 8; i--) {
			currentCounter[i] = (byte) (counterValue & 0xFF);
			counterValue >>= 8;
		}
		
		// Add carry to the remaining bytes (big-endian)
		if (carry > 0) {
			addCarry(blockSize - 8, carry);
		}
	}

	/**
	 * Add a carry value to the counter bytes from the specified position backwards.
	 * 
	 * @param position the position to start adding carry (working backwards)
	 * @param carry the carry value to add
	 */
	private void addCarry(int position, int carry) {
		int add = carry;
		for (int i = position - 1; i >= 0 && add > 0; i--) {
			int value = (currentCounter[i] & 0xFF) + add;
			currentCounter[i] = (byte) value;
			add = value >> 8; // Carry to next byte
		}
	}
}
