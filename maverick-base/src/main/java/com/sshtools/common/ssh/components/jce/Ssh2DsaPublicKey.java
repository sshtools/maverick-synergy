/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
import java.security.Signature;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshDsaPublicKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.SimpleASNWriter;
import com.sshtools.common.util.Utils;

/**
 * A DSA public key implementation which uses a JCE provider.
 * 
 * @author Lee David Painter
 */
public class Ssh2DsaPublicKey implements SshDsaPublicKey {

	private static final String ALGORITHM = "ssh-dss";
	
	public static class Ssh2DsaPublicKeyFactory implements SshPublicKeyFactory<Ssh2DsaPublicKey> {

		@Override
		public Ssh2DsaPublicKey create() throws NoSuchAlgorithmException, IOException {
			return new Ssh2DsaPublicKey();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  ALGORITHM };
		}
	}

	protected DSAPublicKey pubkey;

	public Ssh2DsaPublicKey() {
	}

	public Ssh2DsaPublicKey(DSAPublicKey pub) {
		this.pubkey = pub;
	}

	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.WEAK;
	}
	
	@Override
	public int getPriority() {
		return 0;
	}
	
	public Ssh2DsaPublicKey(BigInteger p, BigInteger q, BigInteger g,
			BigInteger y) throws NoSuchAlgorithmException,
			InvalidKeySpecException {

		KeyFactory keyFactory = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_DSA) : KeyFactory.getInstance(
				JCEAlgorithms.JCE_DSA,
				JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA));
		KeySpec publicKeySpec = new DSAPublicKeySpec(y, p, q, g);
		pubkey = (DSAPublicKey) keyFactory.generatePublic(publicKeySpec);
	}

	public DSAPublicKey getJCEPublicKey() {
		return pubkey;
	}

	/**
	 * Get the algorithm name for the public key.
	 * 
	 * @return the algorithm name, for example "ssh-dss"
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public String getAlgorithm() {
		return ALGORITHM;
	}

	public String getEncodingAlgorithm() {
		return getAlgorithm();
	}
	
	/**
	 * 
	 * @return the bit length of the public key
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public int getBitLength() {
		return pubkey.getParams().getP().bitLength();
	}

	/**
	 * Encode the public key into a blob of binary data, the encoded result will
	 * be passed into init to recreate the key.
	 * 
	 * @return an encoded byte array
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public byte[] getEncoded() throws SshException {

		ByteArrayWriter baw = new ByteArrayWriter();
		try {

			baw.writeString(getEncodingAlgorithm());
			baw.writeBigInteger(pubkey.getParams().getP());
			baw.writeBigInteger(pubkey.getParams().getQ());
			baw.writeBigInteger(pubkey.getParams().getG());
			baw.writeBigInteger(pubkey.getY());

			return baw.toByteArray();
		} catch (IOException ioe) {
			throw new SshException("Failed to encoded DSA key",
					SshException.INTERNAL_ERROR, ioe);
		} finally {
			try {
				baw.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 
	 * @return java.lang.String
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getEncoded());
	}

	/**
	 * Initialize the public key from a blob of binary data.
	 * 
	 * @param blob
	 *            byte[]
	 * @param start
	 *            int
	 * @param len
	 *            int
	 * @throws SshException
	 * @todo Implement this com.maverick.ssh.SshPublicKey method
	 */
	public void init(byte[] blob, int start, int len) throws SshException {

		ByteArrayReader bar = new ByteArrayReader(blob, start, len);

		try {
			DSAPublicKeySpec dsaKey;

			// Extract the key information
			String header = bar.readString();

			if (!header.equals(getAlgorithm())) {
				throw new SshException("The encoded key is not DSA",
						SshException.INTERNAL_ERROR);
			}

			BigInteger p = bar.readBigInteger();
			BigInteger q = bar.readBigInteger();
			BigInteger g = bar.readBigInteger();
			BigInteger y = bar.readBigInteger();
			dsaKey = new DSAPublicKeySpec(y, p, q, g);

			KeyFactory kf = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA) == null ? KeyFactory
					.getInstance(JCEAlgorithms.JCE_DSA) : KeyFactory
					.getInstance(JCEAlgorithms.JCE_DSA, JCEProvider
							.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA));
			pubkey = (DSAPublicKey) kf.generatePublic(dsaKey);

		} catch (Exception ex) {
			throw new SshException(
					"Failed to obtain DSA key instance from JCE",
					SshException.INTERNAL_ERROR, ex);

		} finally {
			bar.close();
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
		try {
			if (signature.length != 40 // 160 bits
					&& signature.length != 56 // 224 bits
					&& signature.length != 64) { // 256 bits

				ByteArrayReader bar = new ByteArrayReader(signature);
				try {
					byte[] sig = bar.readBinaryString();

					// Log.debug("Signature blob is " + new String(sig));
					String header = new String(sig);

					if (!header.equals("ssh-dss")) {
						throw new SshException(
								"The encoded signature is not DSA",
								SshException.INTERNAL_ERROR);
					}

					signature = bar.readBinaryString();
				} finally {
					bar.close();
				}
			}

			int numSize = signature.length / 2;

			byte[] r = new BigInteger(1, java.util.Arrays.copyOfRange(signature, 0, numSize)).toByteArray();
	        byte[] s = new BigInteger(1, java.util.Arrays.copyOfRange(signature, numSize, signature.length)).toByteArray();

			SimpleASNWriter asn = new SimpleASNWriter();
			asn.writeByte(0x02);
 			asn.writeData(r);
 			asn.writeByte(0x02);
			asn.writeData(s);

			SimpleASNWriter asnEncoded = new SimpleASNWriter();
			asnEncoded.writeByte(0x30);
			asnEncoded.writeData(asn.toByteArray());

			byte[] encoded = asnEncoded.toByteArray();

			Signature sig = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithDSA) == null ? Signature
					.getInstance(JCEAlgorithms.JCE_SHA1WithDSA)
					: Signature
							.getInstance(
									JCEAlgorithms.JCE_SHA1WithDSA,
									JCEProvider
											.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithDSA));
			sig.initVerify(pubkey);
			sig.update(data);

            if(Log.isDebugEnabled()) {
            	Log.debug("Encoded Signature: " + Utils.bytesToHex(encoded));
            	Log.debug("R: " + Utils.bytesToHex(r) + " len=" + r.length + " numSize=" + numSize);
            	Log.debug("S: " + Utils.bytesToHex(s) + " len=" + s.length + " numSize=" + numSize);
            }
			return sig.verify(encoded);
		} catch (Exception ex) {
			throw new SshException(SshException.JCE_ERROR, ex);
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof SshDsaPublicKey) {
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

	public BigInteger getG() {
		return pubkey.getParams().getG();
	}

	public BigInteger getP() {
		return pubkey.getParams().getP();
	}

	public BigInteger getQ() {
		return pubkey.getParams().getQ();
	}

	public BigInteger getY() {
		return pubkey.getY();
	}

	@Override
	public String test() {
		
		try {
			KeyFactory keyFactory = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA) == null ? KeyFactory
					.getInstance(JCEAlgorithms.JCE_DSA) : KeyFactory.getInstance(
					JCEAlgorithms.JCE_DSA,
					JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA));
					
			@SuppressWarnings("unused")
			Signature sig = JCEProvider
					.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithDSA) == null ? Signature
					.getInstance(JCEAlgorithms.JCE_SHA1WithDSA)
					: Signature
							.getInstance(
									JCEAlgorithms.JCE_SHA1WithDSA,
									JCEProvider
											.getProviderForAlgorithm(JCEAlgorithms.JCE_SHA1WithDSA));
					
			return keyFactory.getProvider().getName();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
}
	public String getSigningAlgorithm() {
		return "ssh-dss";
	}

}
