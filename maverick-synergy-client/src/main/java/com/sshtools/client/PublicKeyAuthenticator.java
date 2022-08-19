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

import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.Connection;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public abstract class PublicKeyAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator {

	public final static int SSH_MSG_USERAUTH_PK_OK = 60;
	
	boolean isAuthenticating = false;
	TransportProtocolClient transport;
	String username;
	
	SignatureGenerator signatureGenerator;

	public PublicKeyAuthenticator() {

	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException {
		
		onStartAuthentication(transport.getConnection());
		
		this.transport = transport;
		this.username = username;

		if(hasCredentialsRemaining()) {
			doPublicKeyAuth();
		}

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
		} catch(InvalidPassphraseException e) {
			Log.error("Public key operation failed",e);
			failure();
		}
	}
	
	byte[] generateSignatureData() throws IOException,
			SshException, InvalidPassphraseException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeBinaryString(transport.getSessionKey());
			baw.write(AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
			baw.writeString(username);
			baw.writeString("ssh-connection");
			baw.writeString("publickey");
			baw.writeBoolean(isAuthenticating);
			writePublicKey(baw, getPublicKey());
			
			return baw.toByteArray();

		} 
	}
	
	protected abstract SshPublicKey getPublicKey() throws IOException;

	protected abstract SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException;
	
	protected abstract boolean hasCredentialsRemaining();
	
	private void writePublicKey(ByteArrayWriter baw, SshPublicKey key) throws IOException, SshException {

		baw.writeString(key.getAlgorithm());
		baw.writeBinaryString(key.getEncoded());
		
	}


	byte[] generateAuthenticationRequest(byte[] data) throws IOException, SshException, InvalidPassphraseException {

		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			baw.writeBoolean(isAuthenticating);
			writePublicKey(baw, getPublicKey());
	
			if (isAuthenticating) {
	
					byte[] signature = getSignatureGenerator().sign(getPublicKey(), getPublicKey().getSigningAlgorithm(), data);
					baw.writeBinaryString(signature);
			}
			
			return baw.toByteArray();
		
		} finally {
			baw.close();
		}

	}

	protected SignatureGenerator getSignatureGenerator() throws IOException, InvalidPassphraseException {
		return getAuthenticatingKey();
	}

	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException, SshException {
		
		switch(msg.read()) {
		case SSH_MSG_USERAUTH_PK_OK:
		{
 			if(Log.isDebugEnabled()) {
 				Log.debug("Received SSH_MSG_USERAUTH_PK_OK");
 				Log.debug("Server accepts {} {}", getPublicKey().getAlgorithm(), SshKeyUtils.getFingerprint(getPublicKey()));
 			}
 			
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
			if(hasCredentialsRemaining()) {
				isAuthenticating = false;
				doPublicKeyAuth();
				return true;
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
