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

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshKeyFingerprint;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshEd25519PublicKeyJCE implements SshEd25519PublicKey {

	public final static byte[] ASN_HEADER = { 0x30, 0x2A, 0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70, 0x03, 0x21, 0x00 };
	
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
		KeyFactory keyFactory = JCEProvider.getKeyFactory(JCEAlgorithms.ED25519);
		byte[] encoded = Arrays.cat(ASN_HEADER, pk);
		EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(encoded);
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
		byte[] encoded = publicKey.getEncoded();
		byte[] seed = Arrays.copy(encoded, encoded.length-32, 32);
		return seed;
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
			Signature sgr = JCEProvider.getSignature(JCEAlgorithms.ED25519);
			sgr.initVerify(publicKey);
			sgr.update(data);
			return sgr.verify(signature);
		} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException e) {
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
			KeyFactory factory = JCEProvider.getKeyFactory(JCEAlgorithms.ED25519);
			return factory.getProvider().getName();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
			 

	}

	@Override
	public PublicKey getJCEPublicKey() {
		return publicKey;
	}
}
