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
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import org.bouncycastle.crypto.params.ParametersWithContext;
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
import com.sshtools.common.util.SimpleASNWriter;

/**
 * Implements the SSH public key for the post-quantum composite algorithm
 * "ssh-mldsa87-p384" according to draft-ietf-sshm-composite-sigs-00.
 */
public class SshMldsa87P384PublicKey implements SshPublicKey {

	public static final String ALGORITHM_NAME = "ssh-mldsa87-p384";
	public static final String CURVE_NAME = "secp384r1";
	public static final int MLDSA87_PUBLIC_KEY_SIZE = 2592;
	public static final int ECDSA_P384_POINT_SIZE = 97;
	public static final int COMPOSITE_PUBLIC_KEY_SIZE = MLDSA87_PUBLIC_KEY_SIZE + 4 + ECDSA_P384_POINT_SIZE; // 2693
	public static final int COMPOSITE_PUBLIC_KEY_SIZE_RAW = MLDSA87_PUBLIC_KEY_SIZE + ECDSA_P384_POINT_SIZE; // 2689

	public static final int MLDSA87_SIGNATURE_SIZE = 4627;
	public static final int MLDSA87_SIGNATURE_SIZE_ALT = 4896;

	private static ECParameterSpec p384Spec;

	private byte[] mldsaPk;
	private byte[] q;
	private MLDSAPublicKeyParameters mldsaPubKeyParams;
	private ECPublicKey ecPublicKey;

	public static class SshMldsa87P384PublicKeyFactory implements SshPublicKeyFactory<SshMldsa87P384PublicKey> {

		@Override
		public SshMldsa87P384PublicKey create() throws NoSuchAlgorithmException, IOException {
			return new SshMldsa87P384PublicKey();
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

	public static synchronized ECParameterSpec getP384Spec() throws SshException {
		if (p384Spec == null) {
			try {
				AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
				ap.init(new ECGenParameterSpec(CURVE_NAME));
				p384Spec = ap.getParameterSpec(ECParameterSpec.class);
			} catch (Exception e) {
				try {
					org.bouncycastle.jce.spec.ECNamedCurveParameterSpec bcSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(CURVE_NAME);
					KeyFactory kf = KeyFactory.getInstance("ECDSA", JCEProvider.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
					org.bouncycastle.jce.spec.ECPublicKeySpec pubSpec = new org.bouncycastle.jce.spec.ECPublicKeySpec(bcSpec.getG(), bcSpec);
					p384Spec = ((ECPublicKey) kf.generatePublic(pubSpec)).getParams();
				} catch (Exception ex) {
					throw new SshException("Failed to obtain ECParameterSpec for " + CURVE_NAME + ": " + ex.getMessage(), SshException.JCE_ERROR, ex);
				}
			}
		}
		return p384Spec;
	}

	public SshMldsa87P384PublicKey() {
	}

	public SshMldsa87P384PublicKey(byte[] mldsaPk, byte[] q) throws SshException {
		if (mldsaPk == null || mldsaPk.length != MLDSA87_PUBLIC_KEY_SIZE) {
			throw new SshException("Invalid ML-DSA-87 public key size: " + (mldsaPk == null ? 0 : mldsaPk.length) + ", expected " + MLDSA87_PUBLIC_KEY_SIZE, SshException.INTERNAL_ERROR);
		}
		if (q == null || q.length != ECDSA_P384_POINT_SIZE) {
			throw new SshException("Invalid ECDSA P-384 point size: " + (q == null ? 0 : q.length) + ", expected " + ECDSA_P384_POINT_SIZE, SshException.INTERNAL_ERROR);
		}
		this.mldsaPk = Arrays.copyOf(mldsaPk, mldsaPk.length);
		this.q = Arrays.copyOf(q, q.length);
		initCryptoParams();
	}

	public SshMldsa87P384PublicKey(byte[] compositePublicKey) throws SshException {
		loadCompositePublicKey(compositePublicKey);
	}

	public SshMldsa87P384PublicKey(MLDSAPublicKeyParameters mldsaPub, ECPublicKey ecPub) throws SshException {
		if (mldsaPub == null) {
			throw new IllegalArgumentException("mldsaPub cannot be null");
		}
		if (ecPub == null) {
			throw new IllegalArgumentException("ecPub cannot be null");
		}
		this.mldsaPubKeyParams = mldsaPub;
		this.ecPublicKey = ecPub;
		this.mldsaPk = mldsaPub.getEncoded();
		byte[] pointBytes = ECUtils.toByteArray(ecPub.getW(), ecPub.getParams().getCurve());
		if (pointBytes == null || pointBytes.length != ECDSA_P384_POINT_SIZE) {
			throw new SshException("Invalid ECDSA P-384 public key point encoding", SshException.INTERNAL_ERROR);
		}
		this.q = pointBytes;
	}

	private void loadCompositePublicKey(byte[] compositePublicKey) throws SshException {
		if (compositePublicKey == null) {
			throw new SshException("Composite public key cannot be null", SshException.INTERNAL_ERROR);
		}
		if (compositePublicKey.length < COMPOSITE_PUBLIC_KEY_SIZE_RAW) {
			throw new SshException("Truncated composite public key: expected at least " + COMPOSITE_PUBLIC_KEY_SIZE_RAW + " bytes, got " + compositePublicKey.length, SshException.INTERNAL_ERROR);
		}

		this.mldsaPk = new byte[MLDSA87_PUBLIC_KEY_SIZE];
		System.arraycopy(compositePublicKey, 0, this.mldsaPk, 0, MLDSA87_PUBLIC_KEY_SIZE);

		int remaining = compositePublicKey.length - MLDSA87_PUBLIC_KEY_SIZE;
		if (remaining == 4 + ECDSA_P384_POINT_SIZE) {
			// Wire standard format: byte[2592] mldsa_pk || string Q
			ByteArrayReader r = new ByteArrayReader(compositePublicKey, MLDSA87_PUBLIC_KEY_SIZE, remaining);
			try {
				byte[] point = r.readBinaryString();
				if (point.length != ECDSA_P384_POINT_SIZE) {
					throw new SshException("Invalid ECDSA P-384 point length: expected " + ECDSA_P384_POINT_SIZE + ", got " + point.length, SshException.INTERNAL_ERROR);
				}
				if (r.available() > 0) {
					throw new SshException("Unexpected trailing data in composite public key", SshException.INTERNAL_ERROR);
				}
				this.q = point;
			} catch (IOException e) {
				throw new SshException("Failed to decode string Q: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
			} finally {
				try { r.close(); } catch (Exception ignored) {}
			}
		} else if (remaining == ECDSA_P384_POINT_SIZE) {
			// Alternative raw concatenation: byte[2592] mldsa_pk || byte[97] Q
			this.q = new byte[ECDSA_P384_POINT_SIZE];
			System.arraycopy(compositePublicKey, MLDSA87_PUBLIC_KEY_SIZE, this.q, 0, ECDSA_P384_POINT_SIZE);
		} else {
			throw new SshException("Invalid composite public key length: " + compositePublicKey.length, SshException.INTERNAL_ERROR);
		}

		initCryptoParams();
	}

	private void initCryptoParams() throws SshException {
		try {
			this.mldsaPubKeyParams = new MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_87, this.mldsaPk);
		} catch (Exception e) {
			throw new SshException("Failed to initialize ML-DSA-87 public key: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
		}

		if (this.q == null || this.q.length != ECDSA_P384_POINT_SIZE || this.q[0] != 0x04) {
			throw new SshException("Invalid uncompressed ECDSA P-384 point", SshException.INTERNAL_ERROR);
		}

		try {
			ECParameterSpec spec = getP384Spec();
			ECPoint point = ECUtils.fromByteArray(this.q, spec.getCurve());
			if (point == null) {
				throw new SshException("Failed to parse ECPoint from bytes", SshException.INTERNAL_ERROR);
			}

			// Validate coordinates via Bouncy Castle curve point validation
			org.bouncycastle.jce.spec.ECNamedCurveParameterSpec bcSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(CURVE_NAME);
			org.bouncycastle.math.ec.ECPoint bcPoint = bcSpec.getCurve().decodePoint(this.q);
			if (!bcPoint.isValid()) {
				throw new SshException("ECDSA P-384 point is not on curve", SshException.INTERNAL_ERROR);
			}

			KeyFactory kf = KeyFactory.getInstance("EC");
			this.ecPublicKey = (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(point, spec));
		} catch (SshException e) {
			throw e;
		} catch (Exception e) {
			throw new SshException("Invalid ECDSA P-384 public key: " + e.getMessage(), SshException.INTERNAL_ERROR, e);
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
		return 384;
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

	public byte[] getQ() {
		return q != null ? Arrays.copyOf(q, q.length) : null;
	}

	public byte[] getCompositePublicKey() {
		if (mldsaPk == null || q == null) {
			return null;
		}
		ByteArrayWriter baw = new ByteArrayWriter();
		try {
			baw.write(mldsaPk);
			baw.writeBinaryString(q);
			return baw.toByteArray();
		} catch (IOException e) {
			return null;
		} finally {
			try { baw.close(); } catch (IOException ignored) {}
		}
	}

	public MLDSAPublicKeyParameters getMldsaPublicKeyParameters() {
		return mldsaPubKeyParams;
	}

	public ECPublicKey getEcdsaPublicKey() {
		return ecPublicKey;
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

		if (compSig == null || compSig.length < MLDSA87_SIGNATURE_SIZE + 8) {
			return false;
		}

		// Detect whether ML-DSA-87 signature is standard 4627 bytes or draft-typo 4896 bytes
		byte[] mldsaSig;
		BigInteger r;
		BigInteger s;

		if (canParseMpintsAtOffset(compSig, MLDSA87_SIGNATURE_SIZE)) {
			mldsaSig = new byte[MLDSA87_SIGNATURE_SIZE];
			System.arraycopy(compSig, 0, mldsaSig, 0, MLDSA87_SIGNATURE_SIZE);
			ByteArrayReader bar = new ByteArrayReader(compSig, MLDSA87_SIGNATURE_SIZE, compSig.length - MLDSA87_SIGNATURE_SIZE);
			try {
				r = bar.readBigInteger();
				s = bar.readBigInteger();
			} catch (IOException e) {
				return false;
			} finally {
				try { bar.close(); } catch (Exception ignored) {}
			}
		} else if (compSig.length >= MLDSA87_SIGNATURE_SIZE_ALT + 8 && canParseMpintsAtOffset(compSig, MLDSA87_SIGNATURE_SIZE_ALT)) {
			// Alternate 4896 bytes ML-DSA component
			mldsaSig = new byte[MLDSA87_SIGNATURE_SIZE];
			System.arraycopy(compSig, 0, mldsaSig, 0, MLDSA87_SIGNATURE_SIZE);
			ByteArrayReader bar = new ByteArrayReader(compSig, MLDSA87_SIGNATURE_SIZE_ALT, compSig.length - MLDSA87_SIGNATURE_SIZE_ALT);
			try {
				r = bar.readBigInteger();
				s = bar.readBigInteger();
			} catch (IOException e) {
				return false;
			} finally {
				try { bar.close(); } catch (Exception ignored) {}
			}
		} else {
			return false;
		}

		if (r == null || s == null || r.signum() <= 0 || s.signum() <= 0) {
			return false;
		}

		byte[] mPrime = CompositeSignatureUtils.constructMPrime(
				CompositeSignatureUtils.PREFIX,
				CompositeSignatureUtils.LABEL_MLDSA87_ECDSA_P384,
				new byte[0],
				data);

		boolean mldsaVerified;
		try {
			MLDSASigner mldsaVerifier = new MLDSASigner();
			mldsaVerifier.init(false, new ParametersWithContext(mldsaPubKeyParams, CompositeSignatureUtils.LABEL_MLDSA87_ECDSA_P384));
			mldsaVerifier.update(mPrime, 0, mPrime.length);
			mldsaVerified = mldsaVerifier.verifySignature(mldsaSig);
		} catch (Exception e) {
			Log.warn("ML-DSA-87 verification failed with exception: " + e.getMessage());
			return false;
		}

		if (!mldsaVerified) {
			return false;
		}

		boolean ecdsaVerified;
		try {
			SimpleASNWriter asn = new SimpleASNWriter();
			asn.writeByte(0x02);
			asn.writeData(r.toByteArray());
			asn.writeByte(0x02);
			asn.writeData(s.toByteArray());

			SimpleASNWriter asnEncoded = new SimpleASNWriter();
			asnEncoded.writeByte(0x30);
			asnEncoded.writeData(asn.toByteArray());

			byte[] derSig = asnEncoded.toByteArray();

			Signature ecVerifier = Signature.getInstance("SHA384withECDSA");
			ecVerifier.initVerify(ecPublicKey);
			ecVerifier.update(mPrime);
			ecdsaVerified = ecVerifier.verify(derSig);
		} catch (Exception e) {
			Log.warn("ECDSA P-384 verification failed with exception: " + e.getMessage());
			return false;
		}

		return ecdsaVerified;
	}

	private boolean canParseMpintsAtOffset(byte[] buf, int offset) {
		if (offset + 8 > buf.length) {
			return false;
		}
		try {
			ByteArrayReader r = new ByteArrayReader(buf, offset, buf.length - offset);
			try {
				BigInteger rVal = r.readBigInteger();
				BigInteger sVal = r.readBigInteger();
				return r.available() == 0 && rVal != null && sVal != null;
			} finally {
				r.close();
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String test() {
		return "BC";
	}

	@Override
	public PublicKey getJCEPublicKey() {
		return ecPublicKey;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SshMldsa87P384PublicKey) {
			SshMldsa87P384PublicKey other = (SshMldsa87P384PublicKey) obj;
			return Arrays.equals(mldsaPk, other.mldsaPk) && Arrays.equals(q, other.q);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(getCompositePublicKey());
	}
}
