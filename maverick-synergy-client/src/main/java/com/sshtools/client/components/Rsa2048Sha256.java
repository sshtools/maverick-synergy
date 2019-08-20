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
/* HEADER */
package com.sshtools.client.components;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.AbstractKeyExchange;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 *
 * <p>
 * Implementation of RFC 4432 https://tools.ietf.org/html/rfc4432
 * </p>
 * 
 * @author Lee David Painter
 */
public class Rsa2048Sha256 extends SshKeyExchangeClient implements AbstractKeyExchange {

	/**
	 * Constant for the algorithm name "rsa2048-sha256".
	 */
	public static final String RSA_2048_SHA256 = "rsa2048-sha256";

	final static int SSH_MSG_KEXRSA_PUBKEY = 30;
	final static int SSH_MSG_KEXRSA_SECRET = 31;
	final static int SSH_MSG_KEXRSA_DONE = 32;

	Cipher cipher;
	byte[] tk;
	byte[] encryptedSecret;
	private String clientId;
	private String serverId;
	private byte[] clientKexInit;
	private byte[] serverKexInit;
	private byte[] s = new byte[185];
	
	/**
	 * Construct an uninitialized instance.
	 */
	public Rsa2048Sha256() {
		super("SHA-256");
	}

	/**
	 * Get the algorithm name for this key exchange
	 * 
	 * @return "diffie-hellman-group1-sha1"
	 */
	public String getAlgorithm() {
		return RSA_2048_SHA256;
	}

	public String getProvider() {
		return "";
	}

	public void test() {
		try {
			ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	void initCrypto() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
			NoSuchPaddingException {
		cipher = Cipher.getInstance(JCEProvider.getRSAOAEPSHA256AlgorithmName());
	}

	@Override
	public void init(SshTransport<SshClientContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {
		
		this.transport = transport;
		this.clientId = clientId;
		this.serverId = serverId;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;

		try {
			initCrypto();
		} catch (Exception ex) {
			throw new SshException(ex, SshException.JCE_ERROR);
		}
	}

	@Override
	public boolean processMessage(byte[] tmp) throws SshException, IOException {

		switch(tmp[0]) {
		case SSH_MSG_KEXRSA_PUBKEY:

			try (ByteArrayWriter msg = new ByteArrayWriter()) {

				try (ByteArrayReader r = new ByteArrayReader(tmp)) {
					r.skip(1);
					hostKey = r.readBinaryString();
					tk = r.readBinaryString();
				}

				SshRsaPublicKey key = ((SshRsaPublicKey) SshPublicKeyFileFactory.decodeSSH2PublicKey(tk));
				
				JCEComponentManager.getSecureRandom().nextBytes(s);
				
				cipher.init(Cipher.ENCRYPT_MODE, key.getJCEPublicKey());

				try (ByteArrayWriter w = new ByteArrayWriter()) {
					w.writeBinaryString(s);
					encryptedSecret = cipher.doFinal(w.toByteArray());
				}

				if(Log.isDebugEnabled()) {
					Log.debug("Sending SSH_MSG_KEXRSA_SECRET");
				}
				
				transport.postMessage(new SshMessage() {
					public boolean writeMessageIntoBuffer(ByteBuffer buf) {

						buf.put((byte) SSH_MSG_KEXRSA_SECRET);
						buf.putInt(encryptedSecret.length);
						buf.put(encryptedSecret);

						return true;
					}

					public void messageSent(Long sequenceNo) {
						if (Log.isDebugEnabled())
							Log.debug("Sent SSH_MSG_KEX_ECDH_INIT");
					}
				}, true);

			} catch (Throwable ex) {
				throw new SshException("Failed to write SSH_MSG_KEXRSA_SECRET to message buffer", SshException.INTERNAL_ERROR);
			}
			return true;
		case SSH_MSG_KEXRSA_DONE:
			
			if(Log.isDebugEnabled()) {
				Log.debug("Received SSH_MSG_KEXRSA_DONE");
			}

			ByteArrayReader bar = new ByteArrayReader(tmp, 1, tmp.length - 1);

			try {
				signature = bar.readBinaryString();
				secret = new BigInteger(s);

				// Calculate the exchange hash
				calculateExchangeHash();

				transport.sendNewKeys();
			} catch (IOException ex) {
				Log.error("Key exchange failed", ex);
				throw new SshException("Failed to read SSH_MSG_KEXRSA_DONE", SshException.INTERNAL_ERROR);
			} finally {
				bar.close();
			}
			
			return true;
		default: 
			transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, "Key exchange failed");
			throw new SshException("Key exchange failed [id=" + tmp[0] + "]", SshException.INTERNAL_ERROR);
		}

	}

	/**
	 * <p>
	 * Calculates the exchange hash as an SHA1 hash of the following data.
	 * <blockquote>
	 * 
	 * <pre>
	 *  String         the client's version string (CR and NL excluded)
	 *  String         the server's version string (CR and NL excluded)
	 *  String         the payload of the client's SSH_MSG_KEXINIT
	 *  String         the payload of the server's SSH_MSG_KEXINIT
	 *  String         the host key
	 *  BigInteger     e, exchange value sent by the client
	 *  BigInteger     f, exchange value sent by the server
	 *  BigInteger     K, the shared secret
	 * </pre>
	 * 
	 * </blockquote>
	 * </p>
	 *
	 * @throws IOException
	 */
	protected void calculateExchangeHash() throws SshException {

		Digest hash = (Digest) ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());

		// The local software version comments
		hash.putString(clientId);

		// The remote software version comments
		hash.putString(serverId);

		// The local kex init payload
		hash.putInt(clientKexInit.length);
		hash.putBytes(clientKexInit);

		// The remote kex init payload
		hash.putInt(serverKexInit.length);
		hash.putBytes(serverKexInit);

	    // The host key
	    hash.putInt(hostKey.length);
	    hash.putBytes(hostKey);

	    hash.putInt(tk.length);
	    hash.putBytes(tk);
	    
	    hash.putInt(encryptedSecret.length);
	    hash.putBytes(encryptedSecret);
	    
	    // The diffie hellman k value
	    hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

}
