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
package com.sshtools.common.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocolSpecification;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.sshd.AbstractServerTransport;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Provides an {@link AuthenticationMechanism} that enables the public key
 * authentication. The current implementation will look for an
 * <em>authorized_keys</em> file in the users home directory which is returned
 * from the
 * {@link com.maverick.sshd.platform.NativeAuthenticationProvider#getHomeDirectory(String)}
 * method. The format of this file is identical to that required by OpenSSH.
 * 
 * @author Lee David Painter
 */
public class PublicKeyAuthentication<C extends Context> implements AuthenticationMechanism {
	
	public static final int SSH_MSG_USERAUTH_PK_OK = 60;
	AbstractServerTransport<C> transport;
	AbstractAuthenticationProtocol<C> authentication;
	SshConnection con;
	PublicKeyAuthenticationProvider[] providers;
	
	public static final String AUTHENTICATION_METHOD = "publickey";
	
	public PublicKeyAuthentication(AbstractServerTransport<C> transport,
			AbstractAuthenticationProtocol<C> authentication, SshConnection con,
			PublicKeyAuthenticationProvider[] providers) {
		this.transport = transport;
		this.authentication = authentication;
		this.con = con;
		this.providers = providers;
	}

	public String getMethod() {
		return "publickey";
	}

	public boolean startRequest(String username, byte[] msg) throws IOException {

		transport.addTask(ExecutorOperationSupport.EVENTS, new PublicKeyAuthenticationTask(con, username, msg));
		return true;
	}

	private SshPublicKey lookupAuthorizedKey(String algorithm, byte[] keyblob,
			SshConnection con, InetAddress remoteAddress, boolean verify) {

		try {
			SshPublicKey key = SshPublicKeyFileFactory.decodeSSH2PublicKey(
					algorithm, keyblob);

			if(con.getProperty(key.getFingerprint())!=null) {
				return key;
			}
			
			if(providers!=null) {
				for(PublicKeyAuthenticationProvider provider : providers) {
					if (provider.checkKey(
							key,
							con)) {
						con.setProperty(key.getFingerprint(), key);
						return key;
					}
				}
			}

			return null;
		} catch (IOException ex) {
			if(Log.isDebugEnabled()) {
				Log.error("Failed to lookup authorized key", ex);
			}
			transport.disconnect(TransportProtocolSpecification.BY_APPLICATION, ex.getMessage());
			return null;
		} catch(SshException ex) {
			if(Log.isDebugEnabled())
				Log.debug("Client provided unreadable key for authentication", ex);
			return null;
		}
	}

	public boolean processMessage(byte[] msg) throws IOException {
		return false;
	}

	class PublicKeyAuthenticationTask extends ConnectionAwareTask {
		
		String username;
    	byte[] msg;
		
    	PublicKeyAuthenticationTask(SshConnection con, String username, byte[] msg) {
    		super(con);
    		this.username = username;
    		this.msg = msg;
    	}
		
		protected void doTask() {
			// Create ByteArrayReader so can read msg as if it was stream.
			ByteArrayReader bar = new ByteArrayReader(msg);

			try {

				boolean verify = bar.read() == 0 ? false : true;
				final String algorithm = bar.readString();

				// Make sure this only processes supported key types - this is a bug
				// fix and we need to provide better support for additional
				// algorithms
				// such
				// as "x509v3-sign-rsa"
				if (!transport.getContext().getComponentManager().supportedPublicKeys()
						.contains(algorithm)) {
					authentication.failedAuthentication();
					if(Log.isDebugEnabled())
						Log.debug("Unsupported public key algorithm");
					return;
				}

				// read the clients public key
				final byte[] keyblob = bar.readBinaryString();

				byte[] signature = null;
				if (verify) {
					/**
					 * Look for a match in the users authorized_keys file and then
					 * verify the signature of the private key
					 */
					signature = bar.readBinaryString();

					SshPublicKey key = lookupAuthorizedKey(algorithm, keyblob,
							con, con.getRemoteAddress(), verify);
					if (key != null) {

						// string session identifier
						// byte SSH_MSG_USERAUTH_REQUEST
						// string user name
						// string service
						// string "publickey"
						// boolean TRUE
						// string public key algorithm name
						// string public key to be used for authentication

						ByteArrayWriter baw = new ByteArrayWriter();

						try {
							baw.writeBinaryString(transport.getSessionKey());
							baw.write(AbstractAuthenticationProtocol.SSH_MSG_USERAUTH_REQUEST);
							baw.writeString(username);
							baw.writeString("ssh-connection");
							baw.writeString("publickey");
							baw.write(1);
							baw.writeString(algorithm);
							baw.writeBinaryString(keyblob);

							byte[] data = baw.toByteArray();

							if (key.verifySignature(signature, data)) {
								authentication.completedAuthentication();
							} else {
								authentication.failedAuthentication();
							}
						} catch (SshException ex) {
							Log.error("Received SSH exception", ex);
							throw new IOException();
						} finally {
							baw.close();
						}

					} else {
						authentication.failedAuthentication();
					}
				} else {

					Integer count = (Integer) con.getProperty("publickey.max.verify");

					if (count == null)
						count = new Integer(1);
					else
						count = new Integer(count.intValue() + 1);

					con.setProperty("publickey.max.verify", count);

					if (count.intValue() > transport.getContext().getPolicy(AuthenticationPolicy.class).getMaximumPublicKeyVerificationAttempts()) {
						transport
								.disconnect(
										TransportProtocolSpecification.NO_MORE_AUTH_METHODS_AVAILABLE,
										"Too many publickey verification attempts were made.");

						return;
					}

					/**
					 * Simply look for a match in the users authorized keys file
					 */
					if (lookupAuthorizedKey(algorithm, keyblob, con,
							con.getRemoteAddress(), verify) != null) {

						authentication.discardAuthentication();
						
						/**
						 * Send a SSH_MSG_USERAUTH_PK_OK
						 */
						transport.postMessage(new SshMessage() {
							public boolean writeMessageIntoBuffer(ByteBuffer buf) {
								buf.put((byte) SSH_MSG_USERAUTH_PK_OK);
								buf.putInt(algorithm.length());
								buf.put(algorithm.getBytes());
								buf.putInt(keyblob.length);
								buf.put(keyblob);
								return true;
							}

							public void messageSent(Long sequenceNo) {
								if(Log.isDebugEnabled())
									Log.debug("Sent SSH_MSG_USERAUTH_PK_OK");
							}
						});

					} else {
						authentication.failedAuthentication(false, 
								!transport.getContext().getPolicy(AuthenticationPolicy.class).isPublicKeyVerificationFailedAuth());
					}

				}

			} catch(IOException ex) { 
				if(Log.isDebugEnabled()) {
					Log.error("Failed to authenticate public key", ex);
				}
				transport.disconnect(TransportProtocolSpecification.PROTOCOL_ERROR, ex.getMessage());
			} finally {
				bar.close();
			}

		}
	}
}
