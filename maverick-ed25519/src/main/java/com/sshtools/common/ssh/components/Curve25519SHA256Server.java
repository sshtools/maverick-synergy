package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshKeyExchangeLegacy;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;

public class Curve25519SHA256Server extends SshKeyExchangeServer implements
		SshKeyExchangeLegacy {

	public static final int SSH_MSG_KEX_ECDH_INIT = 30;
	public static final int SSH_MSG_KEX_ECDH_REPLY = 31;

	public static final String CURVE25519_SHA2 = "curve25519-sha256@libssh.org";

	byte[] f;
	byte[] privateKey;
	byte[] e;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	
	public Curve25519SHA256Server() {
		super("SHA-256");
	}

	@Override
	public String getAlgorithm() {
		return CURVE25519_SHA2;
	}
	
	public String getProvider() {
		return "";
	}

	protected void calculateExchangeHash() throws SshException {
		Digest hash = (Digest) ComponentManager.getInstance()
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

		hash.putInt(e.length);
		hash.putBytes(e);

		hash.putInt(f.length);
		hash.putBytes(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

	@Override
	public void init(SshTransport<SshServerContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

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
	
	private void initCrypto() throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		f = new byte[32];
		privateKey = new byte[32];
		JCEComponentManager.getSecureRandom().nextBytes(privateKey);
		com.sshtools.common.ssh.components.Curve25519.keygen(f, null, privateKey);
	}
	
	public void test() {
		
		try {
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
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
			
			e = reply.readBinaryString();

			byte[] k = new byte[32];
			com.sshtools.common.ssh.components.Curve25519.curve(k, privateKey, e);
			secret = new BigInteger(1, k);
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
					byte[] tmp = f;
					buf.putInt(tmp.length);
					buf.put(tmp);

					baw.writeString(pubkey.getSigningAlgorithm());
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
}
