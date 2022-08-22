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

package com.sshtools.server.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.components.jce.AbstractKeyExchange;

/**
 *
 * <p>
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 * </p>
 * 
 */
public abstract class DiffieHellmanGroup extends SshKeyExchangeServer implements AbstractKeyExchange {

	/**
	 * Constant for the algorithm name "diffie-hellman-group14-sha1".
	 */
	public static final String DIFFIE_HELLMAN_GROUP14_SHA1 = "diffie-hellman-group14-sha1";

	final static int SSH_MSG_KEXDH_INIT = 30;
	final static int SSH_MSG_KEXDH_REPLY = 31;

	final static BigInteger ONE = BigInteger.valueOf(1);
	final static BigInteger TWO = BigInteger.valueOf(2);

	/** generator, RFC recommends using 2 */
	final static BigInteger g = TWO;

	/** large safe prime, this comes from ....?? */
	BigInteger p = null;

	BigInteger e = null;
	BigInteger f = null;

	KeyPairGenerator dhKeyPairGen;
	KeyAgreement dhKeyAgreement;
	KeyFactory dhKeyFactory;

	String kexAlgorithm;

	/**
	 * Construct an uninitialized instance.
	 */
	DiffieHellmanGroup(String kexAlgorithm, String hashAlgorithm, BigInteger p, SecurityLevel securityLevel, int priority) {
		super(hashAlgorithm, securityLevel, priority);
		this.kexAlgorithm = kexAlgorithm;
		this.p = p;
	}

	/**
	 * Get the algorithm name for this key exchange
	 * 
	 * @return
	 */
	public String getAlgorithm() {
		return kexAlgorithm;
	}

	public void init(SshTransport<SshServerContext> transport, String clientId, String serverId, byte[] clientKexInit,
			byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey, boolean firstPacketFollows,
			boolean useFirstPacket) throws IOException {
		this.clientId = clientId;
		this.serverId = serverId;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;
		this.prvkey = prvkey;
		this.pubkey = pubkey;
		this.firstPacketFollows = firstPacketFollows;
		this.useFirstPacket = useFirstPacket;
		this.transport = transport;


		try {
			initCrypto();
			
			DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);

			dhKeyPairGen.initialize(dhSkipParamSpec, JCEProvider.getSecureRandom());

			KeyPair dhKeyPair = dhKeyPairGen.generateKeyPair();
			dhKeyAgreement.init(dhKeyPair.getPrivate());
			// y = ((DHPrivateKey)dhKeyPair.getPrivate()).getX();
			f = ((DHPublicKey) dhKeyPair.getPublic()).getY();
		} catch (Exception ex) {
			throw new IOException("Failed to generate DH value: " + ex.getMessage());
		}
	}

	private void initCrypto() throws NoSuchAlgorithmException {
		
		dhKeyFactory = JCEProvider.getDHKeyFactory();
		dhKeyPairGen = JCEProvider.getDHKeyGenerator();
		dhKeyAgreement = JCEProvider.getDHKeyAgreement();
	}

	public void test() throws IOException {
		try {
			initCrypto();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public String getProvider() {
		if (dhKeyAgreement != null)
			return dhKeyAgreement.getProvider().getName();
		else
			return "";
	}

	public boolean processMessage(byte[] msg) throws SshException, IOException {

		switch (msg[0]) {
		case SSH_MSG_KEXDH_INIT:

			// Discard this message if it was guessed wrong
			if (firstPacketFollows && !useFirstPacket) {
				if (Log.isDebugEnabled()) {
					Log.debug(
							"Client attempted to guess the kex in use but we determined it was wrong so we're waiting for another SSH_MSG_KEXDH_INIT");
				}
				firstPacketFollows = false;
				return true;
			}

			// Process the actual message
			ByteArrayReader bar = new ByteArrayReader(msg);
			bar.skip(1);
			e = bar.readBigInteger();

			if (Log.isDebugEnabled()) {
				Log.debug("Received SSH_MSG_KEXDH_INIT e={}", e.toString(16));
			}

			DHPublicKeySpec spec = new DHPublicKeySpec(e, p, g);

			try {
				DHPublicKey key = (DHPublicKey) dhKeyFactory.generatePublic(spec);

				dhKeyAgreement.doPhase(key, true);

				byte[] tmp = dhKeyAgreement.generateSecret();
				if ((tmp[0] & 0x80) == 0x80) {
					byte[] tmp2 = new byte[tmp.length + 1];
					System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
					tmp = tmp2;
				}
				// Calculate diffe hellman k value
				secret = new BigInteger(tmp);
			} catch (Exception e1) {
				throw new SshException(e1);
			}

			// Get out host key so we can generate the exchange hash
			hostKey = pubkey.getEncoded();

			// Calculate the exchange hash
			calculateExchangeHash();

			// Generate signature
			signature = prvkey.sign(exchangeHash, pubkey.getSigningAlgorithm());

			// Send our reply message
			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					ByteArrayWriter baw = new ByteArrayWriter();
					try {
						buf.put((byte) SSH_MSG_KEXDH_REPLY);
						buf.putInt(hostKey.length);
						buf.put(hostKey);
						byte[] tmp = f.toByteArray();
						buf.putInt(tmp.length);
						buf.put(tmp);

						baw.writeString(pubkey.getAlgorithm());
						baw.writeBinaryString(signature);
						tmp = baw.toByteArray();

						buf.putInt(tmp.length);
						buf.put(tmp);

					} catch (IOException ex) {
						transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, "Could not read host key");
					} finally {
						try {
							baw.close();
						} catch (IOException e) {
						}
					}

					return true;
				}

				public void messageSent(Long sequenceNo) {
					if (Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_KEXDH_REPLY");
				}
			}, true);

			transport.sendNewKeys();

			return true;

		default:
			return false;
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
		Digest hash = (Digest) ComponentManager.getDefaultInstance().supportedDigests().getInstance(getHashAlgorithm());

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

		// The diffie hellman e value
		hash.putBigInteger(e);

		// The diffie hellman f value
		hash.putBigInteger(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

}
