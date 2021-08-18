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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * A RSA public key implementation which uses a JCE provider.
 * 
 * @author Lee David Painter
 */
public class Ssh2RsaPublicKey implements SshRsaPublicKey {

	protected RSAPublicKey pubKey;

	/**
	 * Default constructor for initializing the key from a byte array using the
	 * init method.
	 * 
	 */
	public Ssh2RsaPublicKey() {
	}

	public Ssh2RsaPublicKey(RSAPublicKey pubKey) {
		this.pubKey = pubKey;
	}

	public Ssh2RsaPublicKey(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_RSA) : KeyFactory.getInstance(
				JCEAlgorithms.JCE_RSA,
				JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA));
		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
		pubKey = (RSAPublicKey) keyFactory.generatePublic(spec);
	}

	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.WEAK;
	}

	@Override
	public int getPriority() {
		return 1000;
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
			baw.writeBigInteger(pubKey.getPublicExponent());
			baw.writeBigInteger(pubKey.getModulus());

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

	public String getEncodingAlgorithm() {
		return getAlgorithm();
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

			if (!header.equals(getEncodingAlgorithm())) {
				throw new SshException("The encoded key is not " + getEncodingAlgorithm(),
						SshException.INTERNAL_ERROR);
			}

			BigInteger e = bar.readBigInteger();
			BigInteger n = bar.readBigInteger();
			rsaKey = new RSAPublicKeySpec(n, e);

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
		return "ssh-rsa";
	}

	public boolean verifySignature(byte[] signature, byte[] data)
			throws SshException {
		try {

			ByteArrayReader bar = new ByteArrayReader(signature);
			String signatureAlgorithm = "ssh-rsa";
			try {

				long count = bar.readInt();
				if (count > 0 && count < 100) {
					bar.reset();
					byte[] sig = bar.readBinaryString();
					signatureAlgorithm = new String(sig);
					signature = bar.readBinaryString();
				}
			} finally {
				bar.close();
			}

			return verifyJCESignature(signature, signatureAlgorithm, data, true);

		} catch (Exception ex) {
			throw new SshException(SshException.JCE_ERROR, ex);
		}

	}
	
	public int getSignatureLength() {
		int length = getModulus().bitLength() / 8;
		int mod = getModulus().bitLength() % 8;
		if(mod != 0) {
			length++;
		}
		return length;
	}

	private boolean verifyJCESignature(byte[] signature, String signatureAlgorithm, byte[] data, boolean allowCorrect)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException {

		Signature s;

		switch(signatureAlgorithm) {
		case "rsa-sha2-256":
			s = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA256WithRSA) == null ? 
					  Signature.getInstance(JCEAlgorithms.JCE_SHA256WithRSA)
					: Signature.getInstance(JCEAlgorithms.JCE_SHA256WithRSA,
								JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA256WithRSA));
			break;
		case "rsa-sha2-512":
			s = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA512WithRSA) == null ? 
					  Signature.getInstance(JCEAlgorithms.JCE_SHA512WithRSA)
					: Signature.getInstance(JCEAlgorithms.JCE_SHA512WithRSA,
								JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA512WithRSA));
			break;
		default:
			s = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA) == null ? 
					  Signature.getInstance(JCEAlgorithms.JCE_SHA1WithRSA)
					: Signature.getInstance(JCEAlgorithms.JCE_SHA1WithRSA,
								JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithRSA));
			break;
		}
		
		s.initVerify(pubKey);
		s.update(data);
		
		int expectedLength = getSignatureLength();
		int signatureLength = signature.length;
		boolean corrected = false;
		byte[] original = signature;
		if(allowCorrect) {
			if(signature.length < expectedLength) {
				if(Log.isDebugEnabled()) {
					Log.debug("No Padding Detected: Expected signature length of " + expectedLength + " (modulus=" + getModulus().bitLength() + ") but got " + signature.length);
				}
				byte[] tmp = new byte[expectedLength];
				System.arraycopy(signature, 0, tmp, expectedLength - signature.length, signature.length);
				signature = tmp;
				corrected = true;
			}
		}

		boolean result = false;
		
		try {
			result = s.verify(signature);
		} catch(SignatureException e) {
			if(!allowCorrect) {
				throw e;
			}
			if(Log.isDebugEnabled()) {
				Log.debug("Signature failed. Falling back to raw signature data.");
			}
		}
		
		if(!result) {
			if(corrected) {
				result = verifyJCESignature(original, signatureAlgorithm, data, false);
			}
			if(!result) {
				if(Log.isDebugEnabled() && Boolean.getBoolean("maverick.verbose")) {
					Log.debug("JCE Reports Invalid Signature: Expected signature length of " + expectedLength + " (modulus=" + getModulus().bitLength() + ") but got " + signatureLength);
				}
			}
		}
		return result;
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
		return "ssh-rsa";
	}
}
