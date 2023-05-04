
package com.sshtools.server.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.server.components.SshKeyExchangeServerFactory;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.components.jce.AbstractKeyExchange;

/**
 *
 * <p>
 * Implementation of RFC 4432 https://tools.ietf.org/html/rfc4432
 * </p>
 * 
 * @author Lee David Painter
 */
public class Rsa2048SHA2KeyExchange extends SshKeyExchangeServer implements AbstractKeyExchange {

	/**
	 * Constant for the algorithm name "rsa2048-sha256".
	 */
	public static final String RSA_2048_SHA2 = "rsa2048-sha256";
	
	public static class Rsa2048SHA2KeyExchangeFactory implements SshKeyExchangeServerFactory<Rsa2048SHA2KeyExchange> {
		@Override
		public Rsa2048SHA2KeyExchange create() throws NoSuchAlgorithmException, IOException {
			return new Rsa2048SHA2KeyExchange();
		}

		@Override
		public String[] getKeys() {
			return new String[] { RSA_2048_SHA2 };
		}
	}

	final static int SSH_MSG_KEXRSA_PUBKEY = 30;
	final static int SSH_MSG_KEXRSA_SECRET = 31;
	final static int SSH_MSG_KEXRSA_DONE = 32;

	Cipher cipher;
	SshKeyPair transientKey;
	byte[] encryptedSecret;

	/**
	 * Construct an uninitialized instance.
	 */
	public Rsa2048SHA2KeyExchange() {
		super("SHA-256", SecurityLevel.STRONG, 2000);
	}

	/**
	 * Get the algorithm name for this key exchange
	 * 
	 * @return "diffie-hellman-group1-sha1"
	 */
	public String getAlgorithm() {
		return RSA_2048_SHA2;
	}

	public void test() {
		try {
			ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	void initCrypto() throws SshException, NoSuchAlgorithmException, NoSuchPaddingException {
		transientKey = JCEComponentManager.getInstance().generateRsaKeyPair(2048, 2);
		cipher = Cipher.getInstance(JCEProvider.getRSAOAEPSHA256AlgorithmName());
	}

	@Override
	public void init(SshTransport<SshServerContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

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
		} catch (Exception ex) {
			throw new IOException("JCE does not support " + getAlgorithm() + " key exchange");
		}

		transport.postMessage(new SshMessage() {

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				try {
					buf.put((byte) SSH_MSG_KEXRSA_PUBKEY);
					byte[] hostkey = pubkey.getEncoded();
					buf.putInt(hostkey.length);
					buf.put(hostkey);

					byte[] tk = transientKey.getPublicKey().getEncoded();
					buf.putInt(tk.length);
					buf.put(tk);

				} catch (SshException e) {
					Rsa2048SHA2KeyExchange.this.transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
							"Could not send transient key");
				}
				return true;
			}

			@Override
			public void messageSent(Long sequenceNo) {
				if (Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEXRSA_PUBKEY");
			}

		}, true);

	}

	public String getProvider() {
		return cipher.getProvider().getName();
	}

	public boolean processMessage(byte[] msg) throws SshException, IOException {

		switch (msg[0]) {
		case SSH_MSG_KEXRSA_SECRET:
			if (Log.isDebugEnabled()) {
				Log.debug("Processing SSH_MSG_KEXRSA_SECRET");
			}

			// Process the actual message
			ByteArrayReader bar = new ByteArrayReader(msg);
			bar.skip(1);

			encryptedSecret = bar.readBinaryString();

			try {
				cipher.init(Cipher.DECRYPT_MODE, transientKey.getPrivateKey().getJCEPrivateKey());
				byte[] tmp = cipher.doFinal(encryptedSecret);
				try (ByteArrayReader r = new ByteArrayReader(tmp)) {
					tmp = r.readBinaryString();
					secret = new BigInteger(tmp);
				}
			} catch (Throwable t) {
				Rsa2048SHA2KeyExchange.this.transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
						"Could not decrypt secret");
				throw new SshException(t);
			}

			// Get our host key so we can generate the exchange hash
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
						buf.put((byte) SSH_MSG_KEXRSA_DONE);

						baw.writeString(pubkey.getSigningAlgorithm());
						baw.writeBinaryString(signature);

						byte[] h = baw.toByteArray();
						buf.putInt(h.length);
						buf.put(h);

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
						Log.debug("Sent SSH_MSG_KEXRSA_DONE");
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

		byte[] tk = transientKey.getPublicKey().getEncoded();
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
