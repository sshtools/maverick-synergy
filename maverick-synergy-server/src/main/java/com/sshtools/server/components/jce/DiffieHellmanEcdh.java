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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.KeyAgreement;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

public abstract class DiffieHellmanEcdh extends SshKeyExchangeServer implements
		SshKeyExchange<SshServerContext> {

	public static final int SSH_MSG_KEX_ECDH_INIT = 30;
	public static final int SSH_MSG_KEX_ECDH_REPLY = 31;

	String name;
	String curve;

	byte[] Q_S;
	byte[] Q_C;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	KeyPairGenerator keyGen;
	KeyAgreement keyAgreement;
	KeyPair keyPair;
	
	protected DiffieHellmanEcdh(String name, String curve, String hashAlgorithm, SecurityLevel securityLevel, int priority) {
		super(hashAlgorithm, securityLevel, priority);
		this.name = name;
		this.curve = curve;
	}

	@Override
	public String getAlgorithm() {
		return name;
	}

	protected void calculateExchangeHash() throws SshException {
		Digest hash = (Digest) ComponentManager.getDefaultInstance()
				.supportedDigests().getInstance(getHashAlgorithm());

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

		hash.putInt(Q_C.length);
		hash.putBytes(Q_C);

		hash.putInt(Q_S.length);
		hash.putBytes(Q_S);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

	@Override
	public void init(SshTransport<SshServerContext> transport, String clientId,
			String serverId, byte[] clientKexInit, byte[] serverKexInit,
			SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket)
			throws IOException {

		try {
			this.transport = transport;
			this.clientId = clientId;
			this.serverId = serverId;
			this.clientKexInit = clientKexInit;
			this.serverKexInit = serverKexInit;
			this.hostKey = pubkey.getEncoded();
			this.prvkey = prvkey;
			this.pubkey = pubkey;
			this.firstPacketFollows = firstPacketFollows;
			this.useFirstPacket = useFirstPacket;
		} catch (SshException e) {
			throw new SshIOException(e);
		}

	}

	@Override
	public boolean processMessage(byte[] msg) throws SshException, IOException {

		if (msg[0] != SSH_MSG_KEX_ECDH_INIT) {
			return false;
		}

		// Discard this message if it was guessed wrong
		if (firstPacketFollows && !useFirstPacket) {
			if(Log.isDebugEnabled()) {
				Log.debug("Client attempted to guess the kex in use but we determined it was wrong so we're waiting for another SSH_MSG_KEX_ECDH_INIT");
			}
			firstPacketFollows = false;
			return true;
		}

		ByteArrayReader reply = new ByteArrayReader(msg, 1, msg.length - 1);

		try {
			
			initCrypto();
			
			Q_C = reply.readBinaryString();
			
			ECPublicKey ec = (ECPublicKey) keyPair.getPublic();
			
			Q_S = ECUtils.toByteArray(ec.getW(), ec.getParams().getCurve());

			keyAgreement.doPhase(ECUtils.decodeKey(Q_C, curve), true);

			byte[] tmp = keyAgreement.generateSecret();
			if ((tmp[0] & 0x80) == 0x80) {
				byte[] tmp2 = new byte[tmp.length + 1];
				System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
				tmp = tmp2;
			}

			// Calculate diffe hellman k value
			secret = new BigInteger(tmp);
		} catch (Exception e) {
			throw new SshException(SshException.KEY_EXCHANGE_FAILED, e);
		} finally {
			reply.close();
		}

		calculateExchangeHash();

		signature = prvkey.sign(exchangeHash, pubkey.getSigningAlgorithm());

		transport.postMessage(new SshMessage() {
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {

				ByteArrayWriter baw = new ByteArrayWriter();
				try {
					buf.put((byte) SSH_MSG_KEX_ECDH_REPLY);
					buf.putInt(hostKey.length);
					buf.put(hostKey);
					byte[] tmp = Q_S;
					buf.putInt(tmp.length);
					buf.put(tmp);

					baw.writeString(pubkey.getAlgorithm());
					baw.writeBinaryString(signature);
					tmp = baw.toByteArray();

					buf.putInt(tmp.length);
					buf.put(tmp);

				} catch (IOException ex) {
					transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
							"Could not read host key");
				} finally {
					try {
						baw.close();
					} catch (IOException e) {
					}
				}

				return true;
			}

			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEX_ECDH_REPLY");
			}
		}, true);

		transport.sendNewKeys();

		return true;
	}

	@Override
	public String getProvider() {
		return keyGen.getProvider().getName();
	}
	
	private void initCrypto() throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());
		
		keyGen = JCEProvider.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName())==null ? 
				KeyPairGenerator.getInstance(JCEProvider.getECDSAAlgorithmName()) : 
					KeyPairGenerator.getInstance(JCEProvider.getECDSAAlgorithmName(), 
							JCEProvider.getProviderForAlgorithm(JCEProvider.getECDSAAlgorithmName()));
		keyAgreement = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_ECDH)==null ? 
				KeyAgreement.getInstance(JCEAlgorithms.JCE_ECDH) : 
					KeyAgreement.getInstance(JCEAlgorithms.JCE_ECDH, 
							JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_ECDH));

		
		ECGenParameterSpec namedSpec = new ECGenParameterSpec(curve);
		keyGen.initialize(namedSpec);
		keyPair = keyGen.generateKeyPair();
		keyAgreement.init(keyPair.getPrivate());
	}
	
	public void test() throws IOException {
		try {
			initCrypto();
		} catch (InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | SshException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
}
