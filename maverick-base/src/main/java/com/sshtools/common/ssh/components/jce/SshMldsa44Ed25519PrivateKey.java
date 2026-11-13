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
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithContext;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;

/**
 * Implements the SSH private key for the post-quantum composite algorithm
 * "ssh-mldsa44-ed25519" according to draft-ietf-sshm-composite-sigs-00.
 */
public class SshMldsa44Ed25519PrivateKey implements SshPrivateKey {

	public static final String ALGORITHM_NAME = "ssh-mldsa44-ed25519";
	public static final int MLDSA44_SEED_SIZE = 32;
	public static final int ED25519_SEED_SIZE = 32;
	public static final int COMPOSITE_PRIVATE_KEY_SIZE = 64;

	private byte[] mldsaSeed;
	private byte[] ed25519Seed;
	private MLDSAPrivateKeyParameters mldsaPrivKeyParams;
	private Ed25519PrivateKeyParameters ed25519PrivKeyParams;

	public SshMldsa44Ed25519PrivateKey(byte[] mldsaSeed, byte[] ed25519Seed) throws SshException {
		if (mldsaSeed == null || mldsaSeed.length != MLDSA44_SEED_SIZE) {
			throw new SshException("Invalid ML-DSA-44 seed size: " + (mldsaSeed == null ? 0 : mldsaSeed.length) + ", expected " + MLDSA44_SEED_SIZE, SshException.INTERNAL_ERROR);
		}
		if (ed25519Seed == null || ed25519Seed.length != ED25519_SEED_SIZE) {
			throw new SshException("Invalid Ed25519 seed size: " + (ed25519Seed == null ? 0 : ed25519Seed.length) + ", expected " + ED25519_SEED_SIZE, SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = Arrays.copyOf(mldsaSeed, mldsaSeed.length);
		this.ed25519Seed = Arrays.copyOf(ed25519Seed, ed25519Seed.length);
		initCryptoParams();
	}

	public SshMldsa44Ed25519PrivateKey(byte[] compositePrivateKey) throws SshException {
		if (compositePrivateKey == null) {
			throw new SshException("Composite private key cannot be null", SshException.INTERNAL_ERROR);
		}
		if (compositePrivateKey.length < COMPOSITE_PRIVATE_KEY_SIZE) {
			throw new SshException("Truncated composite private key: expected " + COMPOSITE_PRIVATE_KEY_SIZE + " bytes, got " + compositePrivateKey.length, SshException.INTERNAL_ERROR);
		}
		if (compositePrivateKey.length > COMPOSITE_PRIVATE_KEY_SIZE) {
			throw new SshException("Oversized composite private key: expected " + COMPOSITE_PRIVATE_KEY_SIZE + " bytes, got " + compositePrivateKey.length, SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = new byte[MLDSA44_SEED_SIZE];
		System.arraycopy(compositePrivateKey, 0, this.mldsaSeed, 0, MLDSA44_SEED_SIZE);

		this.ed25519Seed = new byte[ED25519_SEED_SIZE];
		System.arraycopy(compositePrivateKey, MLDSA44_SEED_SIZE, this.ed25519Seed, 0, ED25519_SEED_SIZE);

		initCryptoParams();
	}

	public SshMldsa44Ed25519PrivateKey(MLDSAPrivateKeyParameters mldsaPriv, Ed25519PrivateKeyParameters edPriv) throws SshException {
		if (mldsaPriv == null) {
			throw new IllegalArgumentException("mldsaPriv cannot be null");
		}
		if (edPriv == null) {
			throw new IllegalArgumentException("edPriv cannot be null");
		}
		this.mldsaPrivKeyParams = mldsaPriv;
		this.ed25519PrivKeyParams = edPriv;
		byte[] seed = mldsaPriv.getSeed();
		if (seed == null || seed.length != MLDSA44_SEED_SIZE) {
			throw new SshException("ML-DSA-44 private key does not contain a valid 32-byte seed", SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = seed;
		this.ed25519Seed = edPriv.getEncoded();
	}

	private void initCryptoParams() throws SshException {
		try {
			this.mldsaPrivKeyParams = new MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_44, this.mldsaSeed);
			this.ed25519PrivKeyParams = new Ed25519PrivateKeyParameters(this.ed25519Seed, 0);
		} catch (Exception e) {
			throw new SshException("Failed to initialize cryptographic parameters from seeds: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}
	}

	/**
	 * Generates a new composite key pair with fresh component key material.
	 *
	 * @return a fresh SshKeyPair
	 * @throws SshException if key generation fails
	 */
	public static SshKeyPair generateKeyPair() throws SshException {
		SecureRandom rnd = JCEProvider.getSecureRandom();
		byte[] mldsaSeed = new byte[MLDSA44_SEED_SIZE];
		byte[] edSeed = new byte[ED25519_SEED_SIZE];
		rnd.nextBytes(mldsaSeed);
		rnd.nextBytes(edSeed);

		SshMldsa44Ed25519PrivateKey prv = new SshMldsa44Ed25519PrivateKey(mldsaSeed, edSeed);
		SshMldsa44Ed25519PublicKey pub = prv.getPublicKey();
		return SshKeyPair.getKeyPair(prv, pub);
	}

	public SshMldsa44Ed25519PublicKey getPublicKey() {
		return new SshMldsa44Ed25519PublicKey(
				mldsaPrivKeyParams.getPublicKeyParameters(),
				ed25519PrivKeyParams.generatePublicKey());
	}

	public byte[] getMldsaSeed() {
		return mldsaSeed != null ? Arrays.copyOf(mldsaSeed, mldsaSeed.length) : null;
	}

	public byte[] getEd25519Seed() {
		return ed25519Seed != null ? Arrays.copyOf(ed25519Seed, ed25519Seed.length) : null;
	}

	public byte[] getCompositePrivateKey() {
		if (mldsaSeed == null || ed25519Seed == null) {
			return null;
		}
		byte[] comp = new byte[COMPOSITE_PRIVATE_KEY_SIZE];
		System.arraycopy(mldsaSeed, 0, comp, 0, MLDSA44_SEED_SIZE);
		System.arraycopy(ed25519Seed, 0, comp, MLDSA44_SEED_SIZE, ED25519_SEED_SIZE);
		return comp;
	}

	public MLDSAPrivateKeyParameters getMldsaPrivateKeyParameters() {
		return mldsaPrivKeyParams;
	}

	public Ed25519PrivateKeyParameters getEd25519PrivateKeyParameters() {
		return ed25519PrivKeyParams;
	}

	@Override
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}

	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			byte[] mPrime = CompositeSignatureUtils.constructMPrime(
					CompositeSignatureUtils.PREFIX,
					CompositeSignatureUtils.LABEL_MLDSA44_ED25519,
					new byte[0],
					data);

			// ML-DSA-44 Sign with ctx=Label
			MLDSASigner mldsaSigner = new MLDSASigner();
			SecureRandom rnd = JCEProvider.getSecureRandom();
			mldsaSigner.init(true, new ParametersWithContext(
					new ParametersWithRandom(mldsaPrivKeyParams, rnd),
					CompositeSignatureUtils.LABEL_MLDSA44_ED25519));
			mldsaSigner.update(mPrime, 0, mPrime.length);
			byte[] mldsaSig = mldsaSigner.generateSignature();

			// Ed25519 Sign over M'
			Ed25519Signer edSigner = new Ed25519Signer();
			edSigner.init(true, ed25519PrivKeyParams);
			edSigner.update(mPrime, 0, mPrime.length);
			byte[] edSig = edSigner.generateSignature();

			byte[] compositeSig = new byte[SshMldsa44Ed25519PublicKey.COMPOSITE_SIGNATURE_SIZE];
			System.arraycopy(mldsaSig, 0, compositeSig, 0, SshMldsa44Ed25519PublicKey.MLDSA44_SIGNATURE_SIZE);
			System.arraycopy(edSig, 0, compositeSig, SshMldsa44Ed25519PublicKey.MLDSA44_SIGNATURE_SIZE, SshMldsa44Ed25519PublicKey.ED25519_SIGNATURE_SIZE);

			return compositeSig;
		} catch (Exception e) {
			throw new IOException("Failed to sign with " + ALGORITHM_NAME + ": " + e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SshMldsa44Ed25519PrivateKey) {
			SshMldsa44Ed25519PrivateKey other = (SshMldsa44Ed25519PrivateKey) obj;
			return Arrays.equals(mldsaSeed, other.mldsaSeed) && Arrays.equals(ed25519Seed, other.ed25519Seed);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(getCompositePrivateKey());
	}
}
