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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * <p>An abstract base class for CBC mode ciphers that properly handles IV chaining
 * and JCE specification compliance by calling doFinal for each transform operation.</p>
 * 
 * <p>According to SSH specification, initialization vectors should be passed from 
 * the end of one packet to the beginning of the next packet. This class maintains
 * the IV state and reinitializes the cipher for each transform operation.</p>
 * 
 * <p>Set system property "maverick.forceNewCBCMode" to "true" to force cipher
 * reinitialization after every transform for testing purposes.</p>
 *
 * @author Lee David Painter
 */
public abstract class AbstractCBCCipher extends AbstractJCECipher {

	private static final boolean FORCE_REINIT = Boolean.getBoolean("maverick.forceNewCBCMode");
	
	private byte[] currentIV;
	private int mode;
	private SecretKeySpec keySpec;
	
	/**
	 * Constructor for CBC cipher implementations.
	 * 
	 * @param spec the value passed into Cipher.getInstance() that specifies the
	 * specification of the cipher; for example "AES/CBC/NoPadding"
	 * @param keyspec the value passed into the constructor of SecretKeySpec.
	 * @param keylength the length in bytes of the key
	 * @param algorithm the SSH algorithm name
	 * @param securityLevel the security level of this cipher
	 * @param priority the priority for algorithm selection
	 * @throws IOException if cipher initialization fails
	 */
	public AbstractCBCCipher(String spec, String keyspec, int keylength, 
			String algorithm, SecurityLevel securityLevel, int priority) throws IOException {
		super(spec, keyspec, keylength, algorithm, securityLevel, priority);
	}

	@Override
	public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
		this.mode = mode;
		
		// Create the key
		byte[] actualKey = new byte[keylength];
		System.arraycopy(keydata, 0, actualKey, 0, actualKey.length);
		this.keySpec = new SecretKeySpec(actualKey, keyspec);
		
		// Initialize the IV for the first packet
		this.currentIV = new byte[getBlockSize()];
		System.arraycopy(iv, 0, currentIV, 0, currentIV.length);
		
		// Initialize cipher with the IV
		initializeCipher();
	}

	/**
	 * Initialize or reinitialize the cipher with the current IV.
	 * 
	 * @throws IOException if cipher initialization fails
	 */
	private void initializeCipher() throws IOException {
		try {
			cipher.init(
				(mode == ENCRYPT_MODE) ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE,
				keySpec, 
				new IvParameterSpec(currentIV, 0, getBlockSize())
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
			
			try {
				// Save the last block of input (ciphertext) for IV before transformation
				// This is needed for decryption mode
				byte[] lastInputBlock = null;
				if (mode == DECRYPT_MODE) {
					int blockSize = getBlockSize();
					lastInputBlock = new byte[blockSize];
					System.arraycopy(buf, start + len - blockSize, lastInputBlock, 0, blockSize);
				}
				
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
					
					// Update IV for next packet and reinitialize cipher
					// This is needed after doFinal as it resets the cipher state
					updateIV(lastInputBlock, output, off, len);
					initializeCipher();
				} else {
					// If update() returned all data, we still need to update the IV
					// but don't reinitialize (cipher state is still valid)
					updateIV(lastInputBlock, output, off, len);
				}
				
			} catch (IllegalBlockSizeException e) {
				throw new IOException("Illegal block size in CBC cipher transform", e);
			} catch (BadPaddingException e) {
				throw new IOException("Bad padding in CBC cipher transform", e);
			} catch (javax.crypto.ShortBufferException e) {
				throw new IOException("Short buffer in CBC cipher transform", e);
			}
		}
	}

	/**
	 * Update the IV for the next packet. According to SSH specification,
	 * the IV should be the last block of ciphertext.
	 * 
	 * @param lastInputBlock the saved last block from input (for decrypt mode), or null
	 * @param outputBuf the output buffer
	 * @param outputStart the start position in output buffer
	 * @param len the length of data processed
	 */
	private void updateIV(byte[] lastInputBlock, byte[] outputBuf, int outputStart, int len) {
		int blockSize = getBlockSize();
		
		if (mode == ENCRYPT_MODE) {
			// For encryption, use the last block of the output (ciphertext)
			int lastBlockOffset = outputStart + len - blockSize;
			System.arraycopy(outputBuf, lastBlockOffset, currentIV, 0, blockSize);
		} else {
			// For decryption, use the saved last block of the input (ciphertext before decryption)
			System.arraycopy(lastInputBlock, 0, currentIV, 0, blockSize);
		}
	}
}
