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

package com.sshtools.server.callback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import com.sshtools.client.AuthenticationMessage;
import com.sshtools.client.SimpleClientAuthenticator;
import com.sshtools.client.TransportProtocolClient;
import com.sshtools.common.auth.MutualKeyAuthenticatonStore;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.TransportProtocol;

public class MutualCallbackAuthenticator extends SimpleClientAuthenticator {

	public static final int SSH_MSG_USERAUTH_SIGNED_CHALLENGE = 60;
	public static final String MUTUAL_KEY_AUTHENTICATION = "mutual-key-auth@sshtools.com";
	
	TransportProtocolClient transport;
	String username;
	byte[] ourChallenge;
	
	MutualKeyAuthenticatonStore authenticationStore;
	
	public MutualCallbackAuthenticator(MutualKeyAuthenticatonStore authenticationStore) {
		this.authenticationStore = authenticationStore;
	}
	
	@Override
	public String getName() {
		return MUTUAL_KEY_AUTHENTICATION;
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException {
		this.transport = transport;
		this.username = username;
		
		transport.addOutgoingTask(new InitialChallenge(transport.getConnection()));

	}
	
	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException {
		if(msg.read()!=SSH_MSG_USERAUTH_SIGNED_CHALLENGE) {
			return false;
		}
		
		transport.addOutgoingTask(new ProcessChallengeResponse(transport.getConnection(), msg));
		return true;
	}
	
	class ProcessChallengeResponse extends ConnectionAwareTask {

		ByteArrayReader msg;
		public ProcessChallengeResponse(SshConnection con, ByteArrayReader msg) {
			super(con);
			this.msg = msg;
		}

		@Override
		protected void doTask() throws Throwable {
			
			byte[] signature = msg.readBinaryString();
			byte[] theirChallenge = msg.readBinaryString();
			
			try(ByteArrayWriter writer = new ByteArrayWriter()) {
				writer.writeBinaryString(ourChallenge);
				writer.writeString(username);
				writer.writeBinaryString(transport.getSessionKey());
				
				SshPublicKey remotePublicKey = authenticationStore.getPublicKey(con);

				if(Log.isDebugEnabled()) {
					Log.debug("Mutual authentication is using the remote key {}", SshKeyUtils.getOpenSSHFormattedKey(remotePublicKey));
				}
				if(Objects.isNull(remotePublicKey)) {
					failure();
					transport.disconnect(TransportProtocol.AUTH_CANCELLED_BY_USER, "There was no public key configured for the user");
					return;
				} 
				
				if(!remotePublicKey.verifySignature(signature, writer.toByteArray())) {
					failure();
					transport.disconnect(TransportProtocol.AUTH_CANCELLED_BY_USER, "Failed to verify remote public key signature");
					return;
				}
				
				writer.reset();
				
				writer.writeBinaryString(theirChallenge);
				writer.writeString(username);
				writer.writeBinaryString(transport.getSessionKey());
				
				SshKeyPair localPrivateKey = authenticationStore.getPrivateKey(con);
				
				if(Log.isDebugEnabled()) {
					Log.debug("Mutual authentication is using the local key {}", SshKeyUtils.getOpenSSHFormattedKey(localPrivateKey.getPublicKey()));
				}
				
				byte[] signature2 = localPrivateKey.getPrivateKey().sign(writer.toByteArray());
				
				transport.postMessage(new SshMessage() {
					public boolean writeMessageIntoBuffer(ByteBuffer buf) {
						buf.put((byte) SSH_MSG_USERAUTH_SIGNED_CHALLENGE);
						buf.putInt(signature2.length);
						buf.put(signature2);
						return true;
					}

					public void messageSent(Long sequenceNo) {
						if(Log.isDebugEnabled())
							Log.debug("Sent SSH_MSG_USERAUTH_SIGNED_CHALLENGE");
					}
				});
			}
		}
	}
		
	class InitialChallenge extends ConnectionAwareTask {

		public InitialChallenge(SshConnection con) {
				super(con);
		}
	
		public void doTask() {
			
			try {
	
				final byte[] msg = generateAuthenticationRequest();
				
				transport.postMessage(new AuthenticationMessage(username, "ssh-connection", MUTUAL_KEY_AUTHENTICATION) {
	
					@Override
					public boolean writeMessageIntoBuffer(ByteBuffer buf) {
	
						super.writeMessageIntoBuffer(buf);
						buf.put(msg);
						return true;
					}
					
				});
			} catch (Throwable e) {
				failure();
				transport.disconnect(TransportProtocol.BY_APPLICATION, "Internal error");
			} 
		}
	
		byte[] generateAuthenticationRequest() throws IOException, SshException, NoSuchAlgorithmException {
	
			try(ByteArrayWriter baw = new ByteArrayWriter()) {
				ourChallenge = new byte[512];
				JCEComponentManager.getSecureRandom().nextBytes(ourChallenge);
				baw.writeBinaryString(ourChallenge);
				return baw.toByteArray();
			} 
		}
	}

}
