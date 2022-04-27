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

package com.sshtools.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.Connection;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public class PublicKeyAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator /*, SignatureGenerator*/ {

	public final static int SSH_MSG_USERAUTH_PK_OK = 60;
	
	boolean isAuthenticating = false;
	TransportProtocolClient transport;
	String username;
	Collection<SshKeyPair> keypairs;
	
	SignatureGenerator signatureGenerator;
	
	SshKeyPair authenticatingPair = null;
	SshPrivateKeyFile authenticatingFile = null;
	
	public PublicKeyAuthenticator() {

	}
	
	public PublicKeyAuthenticator(SshKeyPair... keys) {
		keypairs = new ArrayList<SshKeyPair>();
		keypairs.addAll(Arrays.asList(keys));
	}
	
	public void setKeyPair(SshKeyPair... keys) {
		keypairs = new ArrayList<SshKeyPair>();
		keypairs.addAll(Arrays.asList(keys));
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException {
		
		onStartAuthentication(transport.getConnection());
		
		this.transport = transport;
		this.username = username;

		doPublicKeyAuth();

	}

	protected void onStartAuthentication(Connection<SshClientContext> con) {
		
	}
	
	void doPublicKeyAuth() throws SshException, IOException {
		
		try {

			final byte[] msg = generateAuthenticationRequest(generateSignatureData());
			
			transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "publickey") {

				@Override
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					super.writeMessageIntoBuffer(buf);
					buf.put(msg);
					return true;
				}
				
			});
		} catch (IOException e) {
			Log.error("Public key operation failed",e);
			failure();
		} catch (SshException e) {
			Log.error("Public key operation failed",e);
			failure();
		}
	}
	
	byte[] generateSignatureData() throws IOException,
			SshException {
		
		if(Objects.isNull(authenticatingPair) && !keypairs.isEmpty()) {
			authenticatingPair = keypairs.iterator().next();
		}
	
		if(Objects.isNull(authenticatingPair)) {
			throw new IOException("No suitable key found");
		}
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeBinaryString(transport.getSessionKey());
			baw.write(AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
			baw.writeString(username);
			baw.writeString("ssh-connection");
			baw.writeString("publickey");
			baw.writeBoolean(isAuthenticating);
			writePublicKey(baw, getPublicKey(authenticatingPair));
			

			return baw.toByteArray();

		} 
	}

	private void writePublicKey(ByteArrayWriter baw, SshPublicKey key) throws IOException, SshException {

		baw.writeString(key.getAlgorithm());
		baw.writeBinaryString(key.getEncoded());
		
	}
	
	private SshPublicKey getPublicKey(SshKeyPair pair) {
		if(pair instanceof SshCertificate) {
			return ((SshCertificate)pair).getCertificate();
		}
		return pair.getPublicKey();
	}

	byte[] generateAuthenticationRequest(byte[] data) throws IOException, SshException {

		ByteArrayWriter baw = new ByteArrayWriter();
		
		
		try {
			baw.writeBoolean(isAuthenticating);
			writePublicKey(baw, getPublicKey(authenticatingPair));
	
			if (isAuthenticating) {
	
					byte[] signature = sign(authenticatingPair.getPrivateKey(), 
							authenticatingPair.getPublicKey().getSigningAlgorithm(), data);
					
					// Format the signature correctly
					ByteArrayWriter sig = new ByteArrayWriter();
		
					try {
						sig.writeString(authenticatingPair.getPublicKey().getSigningAlgorithm());
						sig.writeBinaryString(signature);
						baw.writeBinaryString(sig.toByteArray());
					} finally {
						sig.close();
					}

			}
			
			return baw.toByteArray();
		
		} finally {
			baw.close();
		}

	}

	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException, SshException {
		
		switch(msg.read()) {
		case SSH_MSG_USERAUTH_PK_OK:
		{
			isAuthenticating = true;
			try {
				doPublicKeyAuth();
			} catch (SshException | IOException e) {
				Log.error("Public key operation failed",e);
				failure();
			}
			return true;
		}
		case AuthenticationProtocolClient.SSH_MSG_USERAUTH_FAILURE:
		{
			if(!isAuthenticating) {
				keypairs.remove(authenticatingPair);
				authenticatingPair = null;
				if(!keypairs.isEmpty()) {
					doPublicKeyAuth();
					return true;
				}
			}
		}
		}

		return false;
	}

	public byte[] sign(SshPrivateKey prv, String signingAlgorithm, byte[] data) throws SshException {

		try {
			return prv.sign(data, signingAlgorithm);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	@Override
	public String getName() {
		return "publickey";
	}
}
