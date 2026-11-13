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
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithContext;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Implements the SSH public key for the post-quantum composite algorithm
 * "ssh-mldsa44-ed25519" according to draft-ietf-sshm-composite-sigs-00.
 */
public class SshMldsa44Ed25519PublicKey implements SshPublicKey {

	public static final String ALGORITHM_NAME = "ssh-mldsa44-ed25519";
	public static final int MLDSA44_PUBLIC_KEY_SIZE = 1312;
	public static final int ED25519_PUBLIC_KEY_SIZE = 32;
	public static final int COMPOSITE_PUBLIC_KEY_SIZE = 1344;
	public static final int MLDSA44_SIGNATURE_SIZE = 2420;
	public static final int ED25519_SIGNATURE_SIZE = 64;
	public static final int COMPOSITE_SIGNATURE_SIZE = 2484;

	private byte[] mldsaPk;
	private byte[] ed25519Pk;
	private MLDSAPublicKeyParameters mldsaPubKeyParams;
	private Ed25519PublicKeyParameters ed25519PubKeyParams;

	public static class SshMldsa44Ed25519PublicKeyFactory implements SshPublicKeyFactory<SshMldsa44Ed25519PublicKey> {

		@Override
		public SshMldsa44Ed25519PublicKey create() throws NoSuchAlgorithmException, IOException {
			return new SshMldsa44Ed25519PublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM_NAME };
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}
	}

	public SshMldsa44Ed25519PublicKey() {
	}

	public SshMldsa44Ed25519PublicKey(byte[] mldsaPk, byte[] ed25519Pk) throws SshException {
		if (mldsaPk == null || mldsaPk.length != MLDSA44_PUBLIC_KEY_SIZE) {
			throw new SshException("Invalid ML-DSA-44 public key size: " + (mldsaPk == null ? 0 : mldsaPk.length) + ", expected " + MLDSA44_PUBLIC_KEY_SIZE, SshException.INTERNAL_ERROR);
		}
		if (ed25519Pk == null || ed25519Pk.length != ED25519_PUBLIC_KEY_SIZE) {
			throw new SshException("Invalid Ed25519 public key size: " + (ed25519Pk == null ? 0 : ed25519Pk.length) + ", expected " + ED25519_PUBLIC_KEY_SIZE, SshException.INTERNAL_ERROR);
		}
		this.mldsaPk = Arrays.copyOf(mldsaPk, mldsaPk.length);
		this.ed25519Pk = Arrays.copyOf(ed25519Pk, ed25519Pk.length);
		initCryptoParams();
	}

	public SshMldsa44Ed25519PublicKey(byte[] compositePublicKey) throws SshException {
		loadCompositePublicKey(compositePublicKey);
	}

	public SshMldsa44Ed25519PublicKey(MLDSAPublicKeyParameters mldsaPub, Ed25519PublicKeyParameters edPub) {
		if (mldsaPub == null) {
			throw new IllegalArgumentException("mldsaPub cannot be null");
		}
		if (edPub == null) {
			throw new IllegalArgumentException("edPub cannot be null");
		}
		this.mldsaPubKeyParams = mldsaPub;
		this.ed25519PubKeyParams = edPub;
		this.mldsaPk = mldsaPub.getEncoded();
		this.ed25519Pk = edPub.getEncoded();
	}

	private void loadCompositePublicKey(byte[] compositePublicKey) throws SshException {
		if (compositePublicKey == null) {
			throw new SshException("Composite public key cannot be null", SshException.INTERNAL_ERROR);
		}
		if (compositePublicKey.length < COMPOSITE_PUBLIC_KEY_SIZE) {
			throw new SshException("Truncated composite public key: expected " + COMPOSITE_PUBLIC_KEY_SIZE + " bytes, got " + compositePublicKey.length, SshException.INTERNAL_ERROR);
		}
		if (compositePublicKey.length > COMPOSITE_PUBLIC_KEY_SIZE) {
			throw new SshException("Oversized composite public key: expected " + COMPOSITE_PUBLIC_KEY_SIZE + " bytes, got " + compositePublicKey.length, SshException.INTERNAL_ERROR);
		}
		this.mldsaPk = new byte[MLDSA44_PUBLIC_KEY_SIZE];
		System.arraycopy(compositePublicKey, 0, this.mldsaPk, 0, MLDSA44_PUBLIC_KEY_SIZE);

		this.ed25519Pk = new byte[ED25519_PUBLIC_KEY_SIZE];
		System.arraycopy(compositePublicKey, MLDSA44_PUBLIC_KEY_SIZE, this.ed25519Pk, 0, ED25519_PUBLIC_KEY_SIZE);

		initCryptoParams();
	}

	private void initCryptoParams() throws SshException {
		try {
			this.mldsaPubKeyParams = new MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_44, this.mldsaPk);
			this.ed25519PubKeyParams = new Ed25519PublicKeyParameters(this.ed25519Pk, 0);
		} catch (Exception e) {
			throw new SshException("Failed to initialize cryptographic parameters: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}
	}

	@Override
	public SshPublicKey init(byte[] blob, int start, int len) throws SshException {
		ByteArrayReader bar = new ByteArrayReader(blob, start, len);
		try {
			String alg = bar.readString();
			if (!ALGORITHM_NAME.equals(alg)) {
				throw new SshException("The encoded key is not " + ALGORITHM_NAME + " (found " + alg + ")", SshException.INTERNAL_ERROR);
			}
			byte[] compositePublicKey = bar.readBinaryString();
			if (bar.available() > 0) {
				throw new SshException("Unexpected trailing data in public key encoding", SshException.INTERNAL_ERROR);
			}
			loadCompositePublicKey(compositePublicKey);
			return this;
		} catch (IOException e) {
			Log.error("Failed to decode " + ALGORITHM_NAME + " public key", e);
			throw new SshException("Failed to decode public key: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				bar.close();
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM_NAME;
	}

	@Override
	public String getSigningAlgorithm() {
		return ALGORITHM_NAME;
	}

	@Override
	public String getEncodingAlgorithm() {
		return ALGORITHM_NAME;
	}

	@Override
	public int getBitLength() {
		return 256;
	}

	@Override
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.PARANOID;
	}

	@Override
	public int getPriority() {
		return 10000;
	}

	public byte[] getMldsaPublicKey() {
		return mldsaPk != null ? Arrays.copyOf(mldsaPk, mldsaPk.length) : null;
	}

	public byte[] getEd25519PublicKey() {
		return ed25519Pk != null ? Arrays.copyOf(ed25519Pk, ed25519Pk.length) : null;
	}

	public byte[] getCompositePublicKey() {
		if (mldsaPk == null || ed25519Pk == null) {
			return null;
		}
		byte[] comp = new byte[COMPOSITE_PUBLIC_KEY_SIZE];
		System.arraycopy(mldsaPk, 0, comp, 0, MLDSA44_PUBLIC_KEY_SIZE);
		System.arraycopy(ed25519Pk, 0, comp, MLDSA44_PUBLIC_KEY_SIZE, ED25519_PUBLIC_KEY_SIZE);
		return comp;
	}

	public MLDSAPublicKeyParameters getMldsaPublicKeyParameters() {
		return mldsaPubKeyParams;
	}

	public Ed25519PublicKeyParameters getEd25519PublicKeyParameters() {
		return ed25519PubKeyParams;
	}

	@Override
	public byte[] getEncoded() throws SshException {
		ByteArrayWriter baw = new ByteArrayWriter();
		try {
			baw.writeString(getAlgorithm());
			baw.writeBinaryString(getCompositePublicKey());
			return baw.toByteArray();
		} catch (IOException e) {
			throw new SshException("Failed to encode " + ALGORITHM_NAME + " public key", SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				baw.close();
			} catch (IOException ignored) {
			}
		}
	}

	@Override
	public String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getEncoded());
	}

	@Override
	public boolean verifySignature(byte[] signature, byte[] data) throws SshException {
		if (signature == null || signature.length == 0 || data == null) {
			return false;
		}

		byte[] compSig;
		try {
			compSig = CompositeSignatureUtils.unwrapSignature(signature, getSigningAlgorithm());
		} catch (SshException e) {
			return false;
		}

		if (compSig == null || compSig.length != COMPOSITE_SIGNATURE_SIZE) {
			return false;
		}

		byte[] mldsaSig = new byte[MLDSA44_SIGNATURE_SIZE];
		System.arraycopy(compSig, 0, mldsaSig, 0, MLDSA44_SIGNATURE_SIZE);

		byte[] edSig = new byte[ED25519_SIGNATURE_SIZE];
		System.arraycopy(compSig, MLDSA44_SIGNATURE_SIZE, edSig, 0, ED25519_SIGNATURE_SIZE);

		byte[] mPrime = CompositeSignatureUtils.constructMPrime(
				CompositeSignatureUtils.PREFIX,
				CompositeSignatureUtils.LABEL_MLDSA44_ED25519,
				new byte[0],
				data);

		boolean mldsaVerified;
		try {
			MLDSASigner mldsaVerifier = new MLDSASigner();
			mldsaVerifier.init(false, new ParametersWithContext(mldsaPubKeyParams, CompositeSignatureUtils.LABEL_MLDSA44_ED25519));
			mldsaVerifier.update(mPrime, 0, mPrime.length);
			mldsaVerified = mldsaVerifier.verifySignature(mldsaSig);
		} catch (Exception e) {
			Log.warn("ML-DSA-44 verification failed with exception: " + e.getMessage());
			return false;
		}

		if (!mldsaVerified) {
			return false;
		}

		boolean edVerified;
		try {
			Ed25519Signer edVerifier = new Ed25519Signer();
			edVerifier.init(false, ed25519PubKeyParams);
			edVerifier.update(mPrime, 0, mPrime.length);
			edVerified = edVerifier.verifySignature(edSig);
		} catch (Exception e) {
			Log.warn("Ed25519 verification failed with exception: " + e.getMessage());
			return false;
		}

		return edVerified;
	}

	@Override
	public String test() {
		return "BC";
	}

	@Override
	public PublicKey getJCEPublicKey() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SshMldsa44Ed25519PublicKey) {
			SshMldsa44Ed25519PublicKey other = (SshMldsa44Ed25519PublicKey) obj;
			return Arrays.equals(mldsaPk, other.mldsaPk) && Arrays.equals(ed25519Pk, other.ed25519Pk);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(getCompositePublicKey());
	}
}
