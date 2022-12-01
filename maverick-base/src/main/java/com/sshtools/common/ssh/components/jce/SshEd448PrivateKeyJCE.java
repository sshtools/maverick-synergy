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
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.Utils;


public class SshEd448PrivateKeyJCE implements SshEd25519PrivateKey {

	public static final byte[] ASN_HEADER = { 0x30, 0x47, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x71, 0x04, 0x3B, 0x04, 0x39 };
	
	PrivateKey key;
	public SshEd448PrivateKeyJCE(byte[] sk) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
		loadPrivateKey(sk);
	}
	
	private void loadPrivateKey(byte[] sk) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException {
		KeyFactory keyFactory = KeyFactory.getInstance(JCEAlgorithms.ED448);
		byte[] seed = Arrays.copy(sk, 57);
		byte[] encoded = Arrays.cat(ASN_HEADER, seed);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(encoded);
		key = keyFactory.generatePrivate(pkcs8KeySpec);
	}
	
	public SshEd448PrivateKeyJCE(PrivateKey prv) {
		key = prv;
	}

	@Override
	public byte[] sign(byte[] data) throws IOException {
		return sign(data, getAlgorithm());
	}
	
	@Override
	public byte[] sign(byte[] data, String signingAlgorithm) throws IOException {
		try {
			Signature sgr = Signature.getInstance(JCEAlgorithms.ED448);
			sgr.initSign(key);
			sgr.update(data);
			return sgr.sign();
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public String getAlgorithm() {
		return SshEd448PublicKeyJCE.ALGORITHM_NAME;
	}

	@Override
	public PrivateKey getJCEPrivateKey() {
		return key;
	}

	public byte[] getSeed() {
		byte[] encoded = key.getEncoded();
		byte[] seed = Arrays.copy(encoded, ASN_HEADER.length, 57);
		return seed;
	}

	@Override
	public int hashCode() {
		return new String(Utils.bytesToHex(getSeed())).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof SshEd448PrivateKeyJCE)) {
			return false;
		}
		return Arrays.areEqual(getSeed(), ((SshEd448PrivateKeyJCE)obj).getSeed());
	}
}
