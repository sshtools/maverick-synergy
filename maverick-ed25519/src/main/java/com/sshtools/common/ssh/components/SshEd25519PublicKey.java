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
package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.Utils;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class SshEd25519PublicKey implements SshPublicKey {

	public static final String ALGORITHM_NAME = "ssh-ed25519";
	
	EdDSAPublicKey publicKey;
	
	public SshEd25519PublicKey() {
		
	}
	
	public SshEd25519PublicKey(byte[] pk) {
		EdDSAPublicKeySpec spec = new EdDSAPublicKeySpec(pk, EdDSANamedCurveTable.getByName("Ed25519"));
		publicKey = new EdDSAPublicKey(spec);
	}
	
	public SshEd25519PublicKey(PublicKey pub) {
		if(!(pub instanceof EdDSAPublicKey)) {
			throw new IllegalArgumentException("Invalid PublicKey type passed to SshEd25519PublicKey");
		}
		publicKey = (EdDSAPublicKey) pub;
	}
	
	public SecurityLevel getSecurityLevel() {
		return SecurityLevel.PARANOID;
	}
	

	@Override
	public int getPriority() {
		return 5000;
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
			EdDSAPublicKeySpec spec = new EdDSAPublicKeySpec(pub, EdDSANamedCurveTable.getByName("Ed25519"));
			publicKey = new EdDSAPublicKey(spec);
		  
			if(Utils.equal(publicKey.getAbyte(), pub)==0) {
				throw new IOException("Not sure how to encode yet");
			}
		} catch (IOException ioe) {
			throw new SshException("Failed to read encoded key data",
					SshException.INTERNAL_ERROR);
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
		return 0;
	}

	public byte[] getEncoded() throws SshException {
		ByteArrayWriter baw = new ByteArrayWriter();
		try {

			baw.writeString(getAlgorithm());
			baw.writeBinaryString(publicKey.getAbyte());

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

	public byte[] getA() {
		return publicKey.getAbyte();
	}
	
	public GroupElement getGroupElement() {
		return publicKey.getA();
	}
	
	public String getFingerprint() throws SshException {
		return SshKeyFingerprint.getFingerprint(getEncoded());
	}

	public boolean verifySignature(byte[] signature, byte[] data) throws SshException {

		try {
			ByteArrayReader bar = new ByteArrayReader(signature);
			try {

				long count = bar.readInt();
				if (count > 0 && count == getAlgorithm().length()) {
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
			Signature sgr = new EdDSAEngine();
			sgr.initVerify(publicKey);
			sgr.update(data);
			return sgr.verify(signature);
		} catch (InvalidKeyException | SignatureException e) {
			throw new SshException(e, SshException.INTERNAL_ERROR);
		}
//		
//		try {
//			return ed25519.checkvalid(signature, data, publicKey.getAbyte());
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof SshEd25519PublicKey) {
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
		return "net.i2p.crypto/eddsa";
	}

	@Override
	public Key getJCEPublicKey() {
		throw new UnsupportedOperationException("ed25519 is not part of JCE");
	}
}
