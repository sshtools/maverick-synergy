/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.ssh.components.jce.OpenSshCertificate;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * A RSA public key implementation which uses a JCE provider.
 * 
 * @author Lee David Painter
 */
public class OpenSshRsaCertificate extends OpenSshCertificate implements SshRsaPublicKey {

	public static final String SSH_RSA_CERT_V01 = "ssh-rsa-cert-v01@openssh.com";
	
	RSAPublicKey pubKey;
	byte[] nonce;
	
	/**
	 * Default constructor for initializing the key from a byte array using the
	 * init method.
	 * 
	 */
	public OpenSshRsaCertificate() {
	}

	public OpenSshRsaCertificate(RSAPublicKey pubKey) {
		this.pubKey = pubKey;
	}

	public OpenSshRsaCertificate(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_RSA) : KeyFactory.getInstance(
				JCEAlgorithms.JCE_RSA,
				JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
		pubKey = (RSAPublicKey) keyFactory.generatePublic(spec);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.ssh.publickey.RsaPublicKey#getEncoded()
	 */
	public byte[] getEncoded() throws SshException {

		ByteArrayWriter baw = new ByteArrayWriter();
		try {

			baw.writeString(getEncodingAlgorithm());
			baw.writeBinaryString(nonce);
			baw.writeBigInteger(pubKey.getPublicExponent());
			baw.writeBigInteger(pubKey.getModulus());

			encode(baw);
			
			return baw.toByteArray();
		} catch (IOException ex) {
			throw new SshException("Failed to encoded key data",
					SshException.INTERNAL_ERROR, ex);
		} finally {
			try {
				baw.close();
			} catch (IOException e) {
			}
		}
	}

	public String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getEncoded());
	}

	public int getBitLength() {
		return pubKey.getModulus().bitLength();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.maverick.ssh.SshPublicKey#init(byte[], int, int)
	 */
	public void init(byte[] blob, int start, int len) throws SshException {

		ByteArrayReader bar = new ByteArrayReader(blob, start, len);

		try {
			// this.hostKey = hostKey;
			RSAPublicKeySpec rsaKey;

			// Extract the key information

			String header = bar.readString();

			if (!header.equals(getAlgorithm())) {
				throw new SshException("The encoded key is not RSA",
						SshException.INTERNAL_ERROR);
			}

			nonce = bar.readBinaryString();
			BigInteger e = bar.readBigInteger();
			BigInteger n = bar.readBigInteger();
			rsaKey = new RSAPublicKeySpec(n, e);

			decode(bar);
			
			try {
				KeyFactory kf = JCEProvider
						.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
						.getInstance(JCEAlgorithms.JCE_RSA)
						: KeyFactory
								.getInstance(
										JCEAlgorithms.JCE_RSA,
										JCEProvider
												.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
				pubKey = (RSAPublicKey) kf.generatePublic(rsaKey);

			} catch (Exception ex) {
				throw new SshException(
						"Failed to obtain RSA key instance from JCE",
						SshException.INTERNAL_ERROR, ex);
			}
		} catch (IOException ioe) {
			throw new SshException("Failed to read encoded key data",
					SshException.INTERNAL_ERROR);
		} finally {
			bar.close();
		}

	}

	public String getAlgorithm() {
		return SSH_RSA_CERT_V01;
	}

	public boolean verifySignature(byte[] signature, byte[] data)
			throws SshException {
		try {

			ByteArrayReader bar = new ByteArrayReader(signature);
			try {

				long count = bar.readInt();
				if (count > 0 && count < 100) {
					bar.reset();
					byte[] sig = bar.readBinaryString();
					@SuppressWarnings("unused")
					String header = new String(sig);
					signature = bar.readBinaryString();
				}
			} finally {
				bar.close();
			}

			return verifyJCESignature(signature, data);

		} catch (Exception ex) {
			throw new SshException(SshException.JCE_ERROR, ex);
		}

	}

	private boolean verifyJCESignature(byte[] signature, byte[] data)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException {

		Signature s;

		s = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA) == null ? Signature
				.getInstance(JCEAlgorithms.JCE_SHA1WithRSA)
				: Signature
						.getInstance(
								JCEAlgorithms.JCE_SHA1WithRSA,
								JCEProvider
										.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA));
		s.initVerify(pubKey);

		s.update(data);

		return s.verify(signature);
	}

	public boolean equals(Object obj) {
		if (obj instanceof SshRsaPublicKey) {
			try {
				return (((SshPublicKey) obj).getFingerprint()
						.equals(getFingerprint()));
			} catch (SshException ex) {
			}
		}

		return false;
	}

	public int hashCode() {
		try {
			return getFingerprint().hashCode();
		} catch (SshException ex) {
			return 0;
		}
	}

	public BigInteger doPublic(BigInteger input) throws SshException {
		try {

			Cipher cipher = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING) == null ? Cipher
					.getInstance(JCEAlgorithms.JCE_RSANONEPKCS1PADDING)
					: Cipher.getInstance(
							JCEAlgorithms.JCE_RSANONEPKCS1PADDING,
							JCEProvider
									.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING));
			cipher.init(Cipher.ENCRYPT_MODE, pubKey,
					JCEProvider.getSecureRandom());
			byte[] tmp = input.toByteArray();
			return new BigInteger(cipher.doFinal(tmp, tmp[0] == 0 ? 1 : 0,
					tmp[0] == 0 ? tmp.length - 1 : tmp.length));

		} catch (Throwable e) {
			if (e.getMessage().indexOf(JCEAlgorithms.JCE_RSANONEPKCS1PADDING) > -1)
				throw new SshException(
						"JCE provider requires BouncyCastle provider for RSA/NONE/PKCS1Padding component. Add bcprov.jar to your classpath or configure an alternative provider for this algorithm",
						SshException.INTERNAL_ERROR);
			throw new SshException(e);
		}
	}

	public BigInteger getModulus() {
		return pubKey.getModulus();
	}

	public BigInteger getPublicExponent() {
		return pubKey.getPublicExponent();
	}

	public int getVersion() {
		return 2;
	}

	public PublicKey getJCEPublicKey() {
		return pubKey;
	}

	@Override
	public String test() {
		try {
			KeyFactory keyFactory = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
					.getInstance(JCEAlgorithms.JCE_RSA) : KeyFactory.getInstance(
					JCEAlgorithms.JCE_RSA,
					JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
				
			@SuppressWarnings("unused")
			Cipher cipher = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING) == null ? Cipher
					.getInstance(JCEAlgorithms.JCE_RSANONEPKCS1PADDING)
					: Cipher.getInstance(
							JCEAlgorithms.JCE_RSANONEPKCS1PADDING,
							JCEProvider
									.getProviderForAlgorithm(JCEAlgorithms.JCE_RSANONEPKCS1PADDING));
					
			@SuppressWarnings("unused")
			Signature s = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA) == null ? Signature
					.getInstance(JCEAlgorithms.JCE_SHA1WithRSA)
					: Signature
							.getInstance(
									JCEAlgorithms.JCE_SHA1WithRSA,
									JCEProvider
											.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA));
			
			return keyFactory.getProvider().getName();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public String getSigningAlgorithm() {
		return getAlgorithm();
	}
}
