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
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;

public class OpenSshEcdsaCertificate extends OpenSshCertificate implements SshPublicKey {

	byte[] nonce;
	String name;
	String spec;
	String curve;
	
	OpenSshEcdsaCertificate(String name, String spec, String curve) {
		this.name = name;
		this.spec = spec;
		this.curve = curve;
	}
	
	public OpenSshEcdsaCertificate(String name, ECPublicKey pub, String curve) throws IOException {
		this.name = name;
		this.publicKey = new Ssh2EcdsaSha2NistPublicKey(pub, curve);
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.STRONG;
	}
	
	public int getPriority() {
		return (SecurityLevel.STRONG.ordinal() * 1000) + 10;
	}

	protected void decodePublicKey(ByteArrayReader reader) throws IOException, SshException {

		try {
			
			@SuppressWarnings("unused")
			String ignored = reader.readString();
			byte[] Q = reader.readBinaryString();
			
			ECParameterSpec ecspec = getCurveParams(curve);

			ECPoint p = ECUtils.fromByteArray(Q, ecspec.getCurve());
			KeyFactory keyFactory = JCEProvider
					.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));

			this.publicKey = new Ssh2EcdsaSha2NistPublicKey((ECPublicKey) 
					keyFactory.generatePublic(new ECPublicKeySpec(p, ecspec)), curve);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SshException(
					"Failed to obtain ECDSA public key instance",
					SshException.INTERNAL_ERROR, ex);

		}
	}
	
	public String getAlgorithm() {
		return name;
	}

	public int getBitLength() {
		return publicKey.getBitLength();
	}

	public byte[] getPublicOctet() {
		return ((Ssh2EcdsaSha2NistPublicKey)publicKey).getPublicOctet();
	}

	public boolean verifySignature(byte[] signature, byte[] data)
			throws SshException {
		return publicKey.verifySignature(signature, data);
	}

	public ECParameterSpec getCurveParams(String curve) {
		try {
			KeyPairGenerator gen = JCEProvider
					.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyPairGenerator
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyPairGenerator
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));

			gen.initialize(new ECGenParameterSpec(curve),
					JCEProvider.getSecureRandom());
			KeyPair tmp = gen.generateKeyPair();
			return ((ECPublicKey) tmp.getPublic()).getParams();
		} catch (Throwable t) {
		}
		return null;
	}

	public PublicKey getJCEPublicKey() {
		return publicKey.getJCEPublicKey();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((publicKey == null) ? 0 : publicKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenSshEcdsaCertificate other = (OpenSshEcdsaCertificate) obj;
		if (publicKey == null) {
			if (other.publicKey != null)
				return false;
		} else if (!publicKey.equals(other.publicKey))
			return false;
		return true;
	}

	@Override
	public String getSigningAlgorithm() {
		return getAlgorithm();
	}
	
	@Override
	public String test() {
		try {
			KeyFactory keyFactory = JCEProvider
					.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()) == null ? KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName()) : KeyFactory
					.getInstance(JCEProvider.getECDSAAlgorithmName(), JCEProvider
							.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
					
			@SuppressWarnings("unused")
			Signature sig = JCEProvider.getProviderForAlgorithm(spec) == null ? Signature
					.getInstance(spec) : Signature.getInstance(spec,
					JCEProvider.getProviderForAlgorithm(spec));
					
			return keyFactory.getProvider().getName();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
