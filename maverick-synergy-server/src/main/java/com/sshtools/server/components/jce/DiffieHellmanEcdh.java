package com.sshtools.server.components.jce;

/*-
 * #%L
 * Server API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
import java.security.spec.InvalidKeySpecException;

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
import com.sshtools.common.util.Utils;
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

	protected byte[] Q_S;
	protected byte[] Q_C;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	KeyPairGenerator keyGen;
	protected KeyAgreement keyAgreement;
	protected KeyPair keyPair;
	
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

		hash.putInt(secret.length);
		hash.putBytes(secret);
		
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
			
			Q_C = decodeQC(reply.readBinaryString());
			Q_S = encodeQS();
			secret = generateSecret();
			
		} catch (Exception e) {
			throw new SshException(SshException.KEY_EXCHANGE_FAILED, e);
		} finally {
			reply.close();
		}

		calculateExchangeHash();

		int count = 0;
		while(true) {
			signature = prvkey.sign(exchangeHash, pubkey.getSigningAlgorithm());
	
			if(Log.isDebugEnabled()) {
				Log.debug("Verifying signature output to mitigate passive SSH key compromise vulnerability");
			}
			
			if(!pubkey.verifySignature(signature, exchangeHash)) {
				if(count++ >= 3) {
					throw new SshException(SshException.HOST_KEY_ERROR, "Detected invalid signautre from private key!");
				}
				if(Log.isDebugEnabled()) {
					Log.debug("Detected invalid signature output from {} implementation", pubkey.getSigningAlgorithm());
				}
			} else {
				break;
			}
		}

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

					baw.writeString(pubkey.getSigningAlgorithm());
					if(Log.isDebugEnabled()) {
						Log.debug("Using {} signature algorithm for host key of type {} with {} signature {}", 
								pubkey.getSigningAlgorithm(), 
								pubkey.getAlgorithm(), 
								signature.length,
								Utils.bytesToHex(signature, signature.length, true, false));
					}
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
	
	protected String getKeyAgreementAlgorithm() {
		return JCEAlgorithms.JCE_ECDH;
	}
	
	protected KeyAgreement getKeyAgreement() throws NoSuchAlgorithmException {
		return JCEProvider.getProviderForAlgorithm(getKeyAgreementAlgorithm())==null ? 
				KeyAgreement.getInstance(getKeyAgreementAlgorithm()) : 
					KeyAgreement.getInstance(getKeyAgreementAlgorithm(), 
							JCEProvider.getProviderForAlgorithm(getKeyAgreementAlgorithm()));
	}
	
	protected String getKeyPairGeneratorAlgorithm() {
		return JCEProvider.getECDSAAlgorithmName();
	}
	
	protected KeyPairGenerator getKeyPairGenerator() throws NoSuchAlgorithmException {
		return JCEProvider.getProviderForAlgorithm(getKeyPairGeneratorAlgorithm())==null ? 
				KeyPairGenerator.getInstance(getKeyPairGeneratorAlgorithm()) : 
					KeyPairGenerator.getInstance(getKeyPairGeneratorAlgorithm(), 
							JCEProvider.getProviderForAlgorithm(getKeyPairGeneratorAlgorithm()));
	}
	
	protected byte[] encodeQS() {
		ECPublicKey ec = (ECPublicKey) keyPair.getPublic();
		return ECUtils.toByteArray(ec.getW(), ec.getParams().getCurve());
	}
	
	protected byte[] decodeQC(byte[] q_c) {
		return q_c;
	}
	
	protected byte[] generateSecret() throws InvalidKeyException, IllegalStateException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
		keyAgreement.doPhase(ECUtils.decodeKey(Q_C, curve), true);

		byte[] tmp = keyAgreement.generateSecret();
		if ((tmp[0] & 0x80) == 0x80) {
			byte[] tmp2 = new byte[tmp.length + 1];
			System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
			tmp = tmp2;
		}
		
		return new BigInteger(tmp).toByteArray();
	}
	
	protected void initCrypto() throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());		
		
		keyGen = getKeyPairGenerator();
		keyAgreement = getKeyAgreement();
		
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
