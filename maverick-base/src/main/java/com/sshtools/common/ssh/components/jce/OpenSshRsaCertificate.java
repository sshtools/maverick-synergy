/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import com.sshtools.common.publickey.OpenSshCertificate;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.util.ByteArrayReader;

/**
 * A RSA public key implementation which uses a JCE provider.
 * 
 * @author Lee David Painter
 */
public class OpenSshRsaCertificate extends OpenSshCertificate implements SshRsaPublicKey {

	public static final String SSH_RSA_CERT_V01 = "ssh-rsa-cert-v01@openssh.com";
	
//	RSAPublicKey pubKey;
	byte[] nonce;
	
	/**
	 * Default constructor for initializing the key from a byte array using the
	 * init method.
	 * 
	 */
	public OpenSshRsaCertificate() {
	}

	public OpenSshRsaCertificate(RSAPublicKey pubKey) {
		this.publicKey = new Ssh2RsaPublicKey(pubKey);
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.WEAK;
	}
	
	public int getPriority() {
		return (SecurityLevel.WEAK.ordinal() * 1000) + 2;
	}

	public OpenSshRsaCertificate(BigInteger modulus, BigInteger publicExponent)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		this.publicKey = new Ssh2RsaPublicKey(modulus, publicExponent);

	}
	
	public int getBitLength() {
		return publicKey.getBitLength();
	}

	protected void decodePublicKey(ByteArrayReader reader) throws IOException, SshException {

		try {

			BigInteger e = reader.readBigInteger();
			BigInteger n = reader.readBigInteger();
			
			this.publicKey = new Ssh2RsaPublicKey(n, e);

		
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SshException(
					"Failed to obtain RSA public key instance",
					SshException.INTERNAL_ERROR, ex);

		}
	}

	public String getAlgorithm() {
		return SSH_RSA_CERT_V01;
	}

	public boolean verifySignature(byte[] signature, byte[] data)
			throws SshException {
		return publicKey.verifySignature(signature, data);
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

	public int getVersion() {
		return 2;
	}

	public PublicKey getJCEPublicKey() {
		return publicKey.getJCEPublicKey();
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
					.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA_CIPHER) == null ? Cipher
					.getInstance(JCEAlgorithms.JCE_RSA)
					: Cipher.getInstance(
							JCEAlgorithms.JCE_RSA,
							JCEProvider
									.getProviderForAlgorithm(JCEAlgorithms.JCE_RSA_CIPHER));
					
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

	@Override
	public BigInteger getModulus() {
		return ((Ssh2RsaPublicKey)publicKey).getModulus();
	}

	@Override
	public BigInteger getPublicExponent() {
		return ((Ssh2RsaPublicKey)publicKey).getPublicExponent();
	}

	@Override
	public BigInteger doPublic(BigInteger input) throws SshException {
		return ((Ssh2RsaPublicKey)publicKey).doPublic(input);
	}
}
