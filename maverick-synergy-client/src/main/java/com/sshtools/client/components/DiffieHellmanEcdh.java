package com.sshtools.client.components;

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

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.ECUtils;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;

public class DiffieHellmanEcdh extends SshKeyExchangeClient implements
		SshKeyExchange<SshClientContext> {

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

	SshPrivateKey prvkey;
	SshPublicKey pubkey;
	
	KeyPairGenerator keyGen;
	KeyAgreement keyAgreement;
	KeyPair keyPair;
	
	protected DiffieHellmanEcdh(String name, String curve, String hashAlgorithm) {
		super(hashAlgorithm);
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
	public void init(SshTransport<SshClientContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

		try {
			this.transport = transport;
			this.clientId = clientId;
			this.serverId = serverId;
			this.clientKexInit = clientKexInit;
			this.serverKexInit = serverKexInit;
			this.firstPacketFollows = firstPacketFollows;
			this.useFirstPacket = useFirstPacket;


			initCrypto();
			
			ECPublicKey ec = (ECPublicKey) keyPair.getPublic();
			Q_C = ECUtils.toByteArray(ec.getW(), ec.getParams().getCurve());
			
			transport.postMessage(new SshMessage() {
		          public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					buf.put((byte) SSH_MSG_KEX_ECDH_INIT);
					buf.putInt(Q_C.length);
					buf.put(Q_C);

					return true;
				}
		        
				public void messageSent(Long sequenceNo) {
					if(Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_KEX_ECDH_INIT");
				}
			}, true);

		} catch (SshException e) {
			throw new SshIOException(e);
		} catch(Exception e) {
			throw new SshException(e, SshException.KEY_EXCHANGE_FAILED);
		}

	}

	@Override
	public boolean processMessage(byte[] msg) throws SshException, IOException {

		if (msg[0] != SSH_MSG_KEX_ECDH_REPLY) {
			return false;
		}

		ByteArrayReader reply = new ByteArrayReader(msg, 1, msg.length-1);
		
		try {
			hostKey = reply.readBinaryString();
			Q_S = reply.readBinaryString();
			signature = reply.readBinaryString();
			
			keyAgreement.doPhase(ECUtils.decodeKey(Q_S, curve), true);
			
			byte[] tmp = keyAgreement.generateSecret();
	        if((tmp[0] & 0x80)==0x80) {
	        	byte[] tmp2 = new byte[tmp.length+1];
	        	System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
	        	tmp = tmp2;
	        }
	        
	        // Calculate diffe hellman k value
	        secret = new BigInteger(tmp);
	        
			calculateExchangeHash();

			transport.sendNewKeys();

			return true;
		} catch(Exception e) { 
			throw new SshException(e, SshException.KEY_EXCHANGE_FAILED);
		} finally {
			reply.close();
		}


	}

	@Override
	public String getProvider() {
		return keyGen.getProvider().getName();
	}
	
	private void initCrypto() throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		ComponentManager.getDefaultInstance().supportedDigests().getInstance(getHashAlgorithm());		
		
		keyGen = JCEProvider.getDHKeyGenerator();
		keyAgreement = JCEProvider.getDHKeyAgreement();

		
		ECGenParameterSpec namedSpec = new ECGenParameterSpec(curve);
		keyGen.initialize(namedSpec);
		keyPair = keyGen.generateKeyPair();
		keyAgreement.init(keyPair.getPrivate());
	}

	@Override
	public void test() throws IOException, SshException {
		try {
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
}
