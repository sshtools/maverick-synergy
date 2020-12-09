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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;


public class SshEd25519PublicKeyJCE implements SshEd25519PublicKey {

	public static final String ALGORITHM_NAME = "ssh-ed25519";
	
	PublicKey publicKey;
	byte[] pk;
	
	public SshEd25519PublicKeyJCE() {
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.PARANOID;
	}
	
	public int getPriority() {
		return 9999;
	}
	
	public SshEd25519PublicKeyJCE(byte[] pk) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchProviderException {
		this.pk = pk;
		loadPublicKey(pk);
	}
	
	private void loadPublicKey(byte[] pk) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, NoSuchProviderException {
		KeyFactory keyFactory = KeyFactory.getInstance(JCEAlgorithms.ED25519, "BC");
		SubjectPublicKeyInfo pubKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), pk);
		EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKeyInfo.getEncoded());
		publicKey = keyFactory.generatePublic(x509KeySpec);
	}

	public SshEd25519PublicKeyJCE(PublicKey pub) {
		publicKey = pub;
	}

	public void init(byte[] blob, int start, int len) throws SshException {
		
		ByteArrayReader bar = new ByteArrayReader(blob, start, len);
		
		try {
			String name = bar.readString();
			
			if(!name.equals(ALGORITHM_NAME)) {
				throw new SshException("The encoded key is not ed25519",
						SshException.INTERNAL_ERROR);
			}
			
			byte[] pub = bar.readBinaryString();
			loadPublicKey(pub);
			
		} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
			Log.error("Failed to initialise public key", e);
			throw new SshException("Failed to read encoded key data", e);
		} finally {
			bar.close();
		}
	}

	public String getAlgorithm() {
		return ALGORITHM_NAME;
	}

	public String getEncodingAlgorithm() {
		return getAlgorithm();
	}
	
	public int getBitLength() {
		return 256;
	}

	public byte[] getEncoded() throws SshException {
		ByteArrayWriter baw = new ByteArrayWriter();
		try {

			baw.writeString(getAlgorithm());
			baw.writeBinaryString(decodeJCEKey());

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

	private byte[] decodeJCEKey() {
		ASN1InputStream asn = new ASN1InputStream(publicKey.getEncoded());
		try {
			DLSequence id = (DLSequence) asn.readObject();
			DERBitString raw = (DERBitString) id.getObjectAt(1).toASN1Primitive();
			return raw.getBytes();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to parse ASN output of JCE key",e);
		} finally {
			IOUtils.closeStream(asn);
		}
	}

	public byte[] getA() {
		return decodeJCEKey();
	}

	public String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getEncoded());
	}

	public boolean verifySignature(byte[] signature, byte[] data) throws SshException {

		try {
			ByteArrayReader bar = new ByteArrayReader(signature);
			try {

				long count = bar.readInt();
				if (count > 0 && count == getSigningAlgorithm().length()) {
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
	
	private boolean verifyJCESignature(byte[] signature, byte[] data) throws SshException {
		try {
			Signature sgr = Signature.getInstance(JCEAlgorithms.ED25519, "BC");
			sgr.initVerify(publicKey);
			sgr.update(data);
			return sgr.verify(signature);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new SshException(e, SshException.INTERNAL_ERROR);
		}
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
	public String getSigningAlgorithm() {
		return getAlgorithm();
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
	public PublicKey getJCEPublicKey() {
		return publicKey;
	}
}
