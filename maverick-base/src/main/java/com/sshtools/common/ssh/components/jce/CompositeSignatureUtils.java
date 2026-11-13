package com.sshtools.common.ssh.components.jce;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Utility functions for post-quantum composite signatures according to
 * draft-ietf-sshm-composite-sigs-00 and draft-ietf-lamps-pq-composite-sigs-19.
 */
public final class CompositeSignatureUtils {

	public static final byte[] PREFIX = "CompositeAlgorithmSignatures2025".getBytes(StandardCharsets.US_ASCII);
	public static final byte[] LABEL_MLDSA44_ED25519 = "COMPSIG-MLDSA44-Ed25519-SHA512".getBytes(StandardCharsets.US_ASCII);
	public static final byte[] LABEL_MLDSA87_ECDSA_P384 = "COMPSIG-MLDSA87-ECDSA-P384-SHA512".getBytes(StandardCharsets.US_ASCII);

	private CompositeSignatureUtils() {
	}

	/**
	 * Construct the message representative M' as defined in Section 4.2.2 of
	 * draft-ietf-sshm-composite-sigs-00:
	 * <pre>
	 *     M' = Prefix || Label || len(ctx) || ctx || SHA512(M)
	 * </pre>
	 *
	 * @param prefix  the composite prefix (CompositeAlgorithmSignatures2025)
	 * @param label   the algorithm-specific label
	 * @param ctx     the signature context string (empty string for SSH protocol)
	 * @param message the message to sign/verify
	 * @return the message representative M'
	 * @throws SshException if hashing fails or context length exceeds 255 bytes
	 */
	public static byte[] constructMPrime(byte[] prefix, byte[] label, byte[] ctx, byte[] message) throws SshException {
		if (ctx == null) {
			ctx = new byte[0];
		}
		if (ctx.length > 255) {
			throw new SshException("Context length exceeds 255 bytes: " + ctx.length, SshException.BAD_API_USAGE);
		}

		byte[] mHash;
		try {
			MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
			mHash = sha512.digest(message != null ? message : new byte[0]);
		} catch (NoSuchAlgorithmException e) {
			throw new SshException("SHA-512 digest algorithm not available", SshException.JCE_ERROR, e);
		}

		byte[] mPrime = new byte[prefix.length + label.length + 1 + ctx.length + mHash.length];
		int offset = 0;
		System.arraycopy(prefix, 0, mPrime, offset, prefix.length);
		offset += prefix.length;

		System.arraycopy(label, 0, mPrime, offset, label.length);
		offset += label.length;

		mPrime[offset++] = (byte) ctx.length;

		if (ctx.length > 0) {
			System.arraycopy(ctx, 0, mPrime, offset, ctx.length);
			offset += ctx.length;
		}

		System.arraycopy(mHash, 0, mPrime, offset, mHash.length);
		return mPrime;
	}

	/**
	 * Wraps a raw composite signature into the standard SSH signature wire format
	 * (Section 4.2.2.3):
	 * <pre>
	 *     string    signature_type_name
	 *     string    composite_signature
	 * </pre>
	 *
	 * @param algorithmName      the algorithm name (e.g. "ssh-mldsa44-ed25519")
	 * @param compositeSignature the raw composite signature
	 * @return the SSH-formatted signature
	 * @throws IOException if encoding fails
	 */
	public static byte[] wrapSignature(String algorithmName, byte[] compositeSignature) throws IOException {
		ByteArrayWriter baw = new ByteArrayWriter();
		try {
			baw.writeString(algorithmName);
			baw.writeBinaryString(compositeSignature);
			return baw.toByteArray();
		} finally {
			try {
				baw.close();
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Unwraps an SSH signature wrapper if present, returning the underlying
	 * composite signature. If the input is not wrapped (e.g. raw signature),
	 * returns the input unchanged. Rejects signatures with trailing garbage when
	 * wrapped.
	 *
	 * @param signature          the signature bytes (wrapped or raw)
	 * @param expectedAlgorithm  the expected algorithm name
	 * @return the unwraped composite signature
	 * @throws SshException if wrapped signature header does not match expected algorithm
	 *                      or has unexpected trailing data
	 */
	public static byte[] unwrapSignature(byte[] signature, String expectedAlgorithm) throws SshException {
		if (signature == null || signature.length == 0) {
			return signature;
		}

		ByteArrayReader bar = new ByteArrayReader(signature);
		try {
			int headerLen = (int) bar.readInt();
			if (headerLen > 0 && headerLen == expectedAlgorithm.length() && bar.available() >= headerLen) {
				bar.reset();
				String alg = bar.readString();
				if (expectedAlgorithm.equals(alg)) {
					byte[] unwrapped = bar.readBinaryString();
					if (bar.available() > 0) {
						throw new SshException("Unexpected trailing data in SSH signature wrapper", SshException.INTERNAL_ERROR);
					}
					return unwrapped;
				} else {
					throw new SshException("Signature algorithm mismatch: expected " + expectedAlgorithm + " but was " + alg, SshException.INTERNAL_ERROR);
				}
			}
		} catch (SshException e) {
			throw e;
		} catch (Exception ignored) {
			// Not a valid SSH wrapper prefix; treat as raw composite signature
		} finally {
			try {
				bar.close();
			} catch (Exception ignored) {
			}
		}
		return signature;
	}
}
