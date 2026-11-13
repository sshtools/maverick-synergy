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
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import org.bouncycastle.crypto.params.ParametersWithContext;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.SimpleASNReader;

/**
 * Implements the SSH private key for the post-quantum composite algorithm
 * "ssh-mldsa87-p384" according to draft-ietf-sshm-composite-sigs-00.
 */
public class SshMldsa87P384PrivateKey implements SshPrivateKey {

	public static final String ALGORITHM_NAME = "ssh-mldsa87-p384";
	public static final int MLDSA87_SEED_SIZE = 32;

	private byte[] mldsaSeed;
	private BigInteger d;
	private MLDSAPrivateKeyParameters mldsaPrivKeyParams;
	private ECPrivateKey ecPrivateKey;

	public SshMldsa87P384PrivateKey(byte[] mldsaSeed, BigInteger d) throws SshException {
		if (mldsaSeed == null || mldsaSeed.length != MLDSA87_SEED_SIZE) {
			throw new SshException("Invalid ML-DSA-87 seed size: " + (mldsaSeed == null ? 0 : mldsaSeed.length) + ", expected " + MLDSA87_SEED_SIZE, SshException.INTERNAL_ERROR);
		}
		if (d == null || d.signum() <= 0) {
			throw new SshException("Invalid ECDSA P-384 private key integer d", SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = Arrays.copyOf(mldsaSeed, mldsaSeed.length);
		this.d = d;
		initCryptoParams();
	}

	public SshMldsa87P384PrivateKey(byte[] compositePrivateKey) throws SshException {
		if (compositePrivateKey == null) {
			throw new SshException("Composite private key cannot be null", SshException.INTERNAL_ERROR);
		}
		if (compositePrivateKey.length < MLDSA87_SEED_SIZE + 5) {
			throw new SshException("Truncated composite private key: expected at least " + (MLDSA87_SEED_SIZE + 5) + " bytes, got " + compositePrivateKey.length, SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = new byte[MLDSA87_SEED_SIZE];
		System.arraycopy(compositePrivateKey, 0, this.mldsaSeed, 0, MLDSA87_SEED_SIZE);

		ByteArrayReader bar = new ByteArrayReader(compositePrivateKey, MLDSA87_SEED_SIZE, compositePrivateKey.length - MLDSA87_SEED_SIZE);
		try {
			this.d = bar.readBigInteger();
			if (bar.available() > 0) {
				throw new SshException("Unexpected trailing data in composite private key", SshException.INTERNAL_ERROR);
			}
		} catch (IOException e) {
			throw new SshException("Failed to decode ECDSA private key integer d: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		} finally {
			try { bar.close(); } catch (Exception ignored) {}
		}

		if (this.d == null || this.d.signum() <= 0) {
			throw new SshException("Invalid ECDSA P-384 private key integer d", SshException.INTERNAL_ERROR);
		}

		initCryptoParams();
	}

	public SshMldsa87P384PrivateKey(MLDSAPrivateKeyParameters mldsaPriv, ECPrivateKey ecPriv) throws SshException {
		if (mldsaPriv == null) {
			throw new IllegalArgumentException("mldsaPriv cannot be null");
		}
		if (ecPriv == null) {
			throw new IllegalArgumentException("ecPriv cannot be null");
		}
		this.mldsaPrivKeyParams = mldsaPriv;
		this.ecPrivateKey = ecPriv;
		byte[] seed = mldsaPriv.getSeed();
		if (seed == null || seed.length != MLDSA87_SEED_SIZE) {
			throw new SshException("ML-DSA-87 private key does not contain a valid 32-byte seed", SshException.INTERNAL_ERROR);
		}
		this.mldsaSeed = seed;
		this.d = ecPriv.getS();
	}

	private void initCryptoParams() throws SshException {
		try {
			this.mldsaPrivKeyParams = new MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_87, this.mldsaSeed);
		} catch (Exception e) {
			throw new SshException("Failed to initialize ML-DSA-87 private key from seed: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}

		try {
			ECParameterSpec spec = SshMldsa87P384PublicKey.getP384Spec();
			KeyFactory kf = KeyFactory.getInstance("EC");
			this.ecPrivateKey = (ECPrivateKey) kf.generatePrivate(new ECPrivateKeySpec(this.d, spec));
		} catch (Exception e) {
			throw new SshException("Failed to initialize ECDSA P-384 private key: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}
	}

	/**
	 * Generates a new composite key pair with fresh component key material.
	 *
	 * @return a fresh SshKeyPair
	 * @throws SshException if key generation fails
	 */
	public static SshKeyPair generateKeyPair() throws SshException {
		try {
			SecureRandom rnd = JCEProvider.getSecureRandom();
			byte[] mldsaSeed = new byte[MLDSA87_SEED_SIZE];
			rnd.nextBytes(mldsaSeed);

			KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
			kpg.initialize(new ECGenParameterSpec(SshMldsa87P384PublicKey.CURVE_NAME), rnd);
			KeyPair kp = kpg.generateKeyPair();

			ECPrivateKey ecPriv = (ECPrivateKey) kp.getPrivate();
			ECPublicKey ecPub = (ECPublicKey) kp.getPublic();

			MLDSAPrivateKeyParameters mldsaPriv = new MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_87, mldsaSeed);
			MLDSAPublicKeyParameters mldsaPub = mldsaPriv.getPublicKeyParameters();

			SshMldsa87P384PrivateKey prv = new SshMldsa87P384PrivateKey(mldsaPriv, ecPriv);
			SshMldsa87P384PublicKey pub = new SshMldsa87P384PublicKey(mldsaPub, ecPub);

			return SshKeyPair.getKeyPair(prv, pub);
		} catch (SshException e) {
			throw e;
		} catch (Exception e) {
			throw new SshException("Failed to generate " + ALGORITHM_NAME + " key pair: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}
	}

	public SshMldsa87P384PublicKey getPublicKey() throws SshException {
		try {
			// Compute public ECPoint Q = d * G
			org.bouncycastle.jce.spec.ECNamedCurveParameterSpec bcSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(SshMldsa87P384PublicKey.CURVE_NAME);
			org.bouncycastle.math.ec.ECPoint qPoint = bcSpec.getG().multiply(d).normalize();
			ECPoint w = new ECPoint(qPoint.getAffineXCoord().toBigInteger(), qPoint.getAffineYCoord().toBigInteger());

			ECParameterSpec spec = SshMldsa87P384PublicKey.getP384Spec();
			KeyFactory kf = KeyFactory.getInstance("EC");
			ECPublicKey ecPub = (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(w, spec));

			return new SshMldsa87P384PublicKey(mldsaPrivKeyParams.getPublicKeyParameters(), ecPub);
		} catch (SshException e) {
			throw e;
		} catch (Exception e) {
			throw new SshException("Failed to derive public key: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}
	}

	public byte[] getMldsaSeed() {
		return mldsaSeed != null ? Arrays.copyOf(mldsaSeed, mldsaSeed.length) : null;
	}

	public BigInteger getD() {
		return d;
	}

	public byte[] getCompositePrivateKey() throws SshException {
		if (mldsaSeed == null || d == null) {
			return null;
		}
		ByteArrayWriter baw = new ByteArrayWriter();
		try {
			baw.write(mldsaSeed);
			baw.writeBigInteger(d);
			return baw.toByteArray();
		} catch (IOException e) {
			throw new SshException("Failed to encode composite private key", SshException.INTERNAL_ERROR, e);
		} finally {
			try { baw.close(); } catch (IOException ignored) {}
		}
	}

	public MLDSAPrivateKeyParameters getMldsaPrivateKeyParameters() {
		return mldsaPrivKeyParams;
	}

	public ECPrivateKey getEcdsaPrivateKey() {
		return ecPrivateKey;
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
					CompositeSignatureUtils.LABEL_MLDSA87_ECDSA_P384,
					new byte[0],
					data);

			// ML-DSA-87 Sign with ctx=Label
			MLDSASigner mldsaSigner = new MLDSASigner();
			SecureRandom rnd = JCEProvider.getSecureRandom();
			mldsaSigner.init(true, new ParametersWithContext(
					new ParametersWithRandom(mldsaPrivKeyParams, rnd),
					CompositeSignatureUtils.LABEL_MLDSA87_ECDSA_P384));
			mldsaSigner.update(mPrime, 0, mPrime.length);
			byte[] mldsaSig = mldsaSigner.generateSignature();

			// ECDSA P-384 Sign over M' with SHA-384
			Signature ecSigner = Signature.getInstance("SHA384withECDSA");
			ecSigner.initSign(ecPrivateKey, rnd);
			ecSigner.update(mPrime);
			byte[] derSig = ecSigner.sign();

			// Decode DER to extract BigInteger r and s
			SimpleASNReader asn = new SimpleASNReader(derSig);
			asn.getByte(); // 0x30
			asn.getLength();
			asn.getByte(); // 0x02
			byte[] rBytes = asn.getData();
			asn.getByte(); // 0x02
			byte[] sBytes = asn.getData();

			BigInteger r = new BigInteger(1, rBytes);
			BigInteger s = new BigInteger(1, sBytes);

			// Composite signature wire encoding (Section 4.2.2.2):
			// byte[4627] mldsa_sig
			// mpint r
			// mpint s
			ByteArrayWriter baw = new ByteArrayWriter();
			try {
				baw.write(mldsaSig);
				baw.writeBigInteger(r);
				baw.writeBigInteger(s);
				return baw.toByteArray();
			} finally {
				baw.close();
			}
		} catch (SshException e) {
			throw new IOException("Failed to sign with " + ALGORITHM_NAME + ": " + e.getMessage(), e);
		} catch (Exception e) {
			throw new IOException("Error signing with " + ALGORITHM_NAME + ": " + e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return ecPrivateKey;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SshMldsa87P384PrivateKey) {
			SshMldsa87P384PrivateKey other = (SshMldsa87P384PrivateKey) obj;
			return Arrays.equals(mldsaSeed, other.mldsaSeed) && (d != null && d.equals(other.d));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(mldsaSeed);
		result = 31 * result + (d != null ? d.hashCode() : 0);
		return result;
	}
}
