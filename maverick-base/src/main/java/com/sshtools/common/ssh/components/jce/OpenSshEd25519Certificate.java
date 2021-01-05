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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;

public class OpenSshEd25519Certificate extends OpenSshCertificate implements SshPublicKey {

	public static final String CERT_TYPE = "ssh-ed25519-cert-v01@openssh.com";
	
	byte[] nonce;

	public OpenSshEd25519Certificate() {
	}

	public OpenSshEd25519Certificate(PublicKey pub) {
		this.publicKey = new SshEd25519PublicKeyJCE(pub);
	}

	public OpenSshEd25519Certificate(byte[] pk) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchProviderException {
		this.publicKey = new SshEd25519PublicKeyJCE(pk);
	}
	
	public PublicKey getJCEPublicKey() {
		return (PublicKey) publicKey.getJCEPublicKey();
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.PARANOID;
	}
	
	public int getPriority() {
		return 0;
	}

	/**
	 * Get the algorithm name for the public key.
	 * 
	 * @return the algorithm name, for example "ssh-dss"
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public String getAlgorithm() {
		return CERT_TYPE;
	}
	
	/**
	 * 
	 * @return the bit length of the public key
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public int getBitLength() {
		return 256;
	}

	protected void decodePublicKey(ByteArrayReader reader) throws IOException, SshException {

		try {
			byte[] pk = reader.readBinaryString();
			this.publicKey = new SshEd25519PublicKeyJCE(pk);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SshException(
					"Failed to obtain Ed25519 public key instance",
					SshException.INTERNAL_ERROR, ex);

		}
	}

	/**
	 * Verify the signature.
	 * 
	 * @param signature
	 *            byte[]
	 * @param data
	 *            byte[]
	 * @return <code>true</code> if the signature was produced by the
	 *         corresponding private key that owns this public key, otherwise
	 *         <code>false</code>.
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public boolean verifySignature(byte[] signature, byte[] data)
			throws SshException {
		return publicKey.verifySignature(signature, data);
	}

	public boolean equals(Object obj) {
		if (obj instanceof SshEd25519PublicKeyJCE) {
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

	@Override
	public String test() {
		try {
			KeyFactory factory = KeyFactory.getInstance(JCEAlgorithms.ED25519, "BC");
			return factory.getProvider().getName();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public String getSigningAlgorithm() {
		return SshEd25519PublicKeyJCE.ALGORITHM_NAME;
	}
}
