/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.server.components.jce;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.server.SshServerContext;
import com.sshtools.server.components.SshKeyExchangeServer;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.TransportProtocol;
import com.sshtools.synergy.ssh.components.jce.AbstractKeyExchange;

public class DiffieHellmanGroupExchangeSha1JCE extends SshKeyExchangeServer
		implements AbstractKeyExchange {

	final static int SSH_MSG_KEY_DH_GEX_REQUEST_OLD = 30;
	final static int SSH_MSG_KEY_DH_GEX_GROUP = 31;
	final static int SSH_MSG_KEY_DH_GEX_INIT = 32;
	final static int SSH_MSG_KEY_DH_GEX_REPLY = 33;
	final static int SSH_MSG_KEY_DH_GEX_REQUEST = 34;

	final static BigInteger ONE = BigInteger.valueOf(1);
	final static BigInteger TWO = BigInteger.valueOf(2);

	BigInteger g = null;
	BigInteger p = null;
	BigInteger e = null;
	BigInteger f = null;

	UnsignedInteger32 min = null;
	UnsignedInteger32 n = null;
	UnsignedInteger32 max = null;

	KeyPairGenerator dhKeyPairGen;
	KeyAgreement dhKeyAgreement;
	KeyFactory dhKeyFactory;

	static int maxSupportedSize = -1;
	static int minSupportedSize = -1;
	
	/**
	 * Constant for the algorithm name "diffie-hellman-group1-sha1".
	 */
	public static final String DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1 = "diffie-hellman-group-exchange-sha1";

	/**
	 * Construct an uninitialized instance.
	 */
	public DiffieHellmanGroupExchangeSha1JCE() {
		super(JCEAlgorithms.JCE_SHA1, SecurityLevel.WEAK, 1000);
	}

	public DiffieHellmanGroupExchangeSha1JCE(String hashAlgorithm, SecurityLevel securityLevel, int priority) {
		super(hashAlgorithm, securityLevel, priority);
	}

	/**
	 * Get the algorithm name for this key exchange
	 * 
	 * @return "diffie-hellman-group1-sha1"
	 */
	public String getAlgorithm() {
		return DIFFIE_HELLMAN_GROUP_EXCHANGE_SHA1;
	}

	void initCrypto() throws NoSuchAlgorithmException {
		dhKeyFactory = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null
				? KeyFactory.getInstance(JCEAlgorithms.JCE_DH)
				: KeyFactory.getInstance(JCEAlgorithms.JCE_DH,
						JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
		dhKeyPairGen = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null
				? KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH)
				: KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH,
						JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
		dhKeyAgreement = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null
				? KeyAgreement.getInstance(JCEAlgorithms.JCE_DH)
				: KeyAgreement.getInstance(JCEAlgorithms.JCE_DH,
						JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
	}
	
	public void init(SshTransport<SshServerContext> transport, String clientIdentification,
			String serverIdentification, byte[] clientKexInit,
			byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket)
			throws IOException {

		this.clientId = clientIdentification;
		this.serverId = serverIdentification;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;
		this.prvkey = prvkey;
		this.pubkey = pubkey;
		this.firstPacketFollows = firstPacketFollows;
		this.useFirstPacket = useFirstPacket;
		this.transport = transport;

		try {
			initCrypto();
		} catch (NoSuchAlgorithmException ex) {
			throw new SshIOException(new SshException(
					"JCE does not support Diffie Hellman key exchange",
					SshException.JCE_ERROR));
		}

	}

	public String getProvider() {
		if (dhKeyAgreement != null)
			return dhKeyAgreement.getProvider().getName();
		else
			return "";
	}

	public boolean exchangeGroup(byte[] msg) throws SshException, IOException {

		// Discard this message if it was guessed wrong
		if (firstPacketFollows && !useFirstPacket) {
			if(Log.isDebugEnabled())
				Log.debug("Client attempted to guess the kex in use but we determined it was wrong so we're waiting for another message");
			firstPacketFollows = false;
			return true;
		}

		ByteArrayReader bar = new ByteArrayReader(msg);

		try {
			switch (bar.read()) {
			case SSH_MSG_KEY_DH_GEX_REQUEST_OLD:
			{
				
				n = bar.readUINT32();

				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_MSG_KEY_DH_GEX_REQUEST_OLD n={}", n.longValue());
				}
				
				// get prime that is close to n but not larger than it
				DiffieHellmanGroups.DHGroup group = DiffieHellmanGroups.getSafePrime(new UnsignedInteger32(Math.min(maxSupportedSize,
						Math.max(Math.min(n.intValue(), transport.getContext().getMaxDHGroupExchangeKeySize()),
								transport.getContext().getMinDHGroupExchangeKeySize()))));
				p = group.getP();
				g = group.getG();
				
				break;
			}
			case SSH_MSG_KEY_DH_GEX_REQUEST:
			{
				
				min = bar.readUINT32();
				n = bar.readUINT32();
				max = bar.readUINT32();

				if(Log.isDebugEnabled()) {
					Log.debug("Recieved SSH_MSG_KEY_DH_GEX_REQUEST min={} n={} max={}", 
							min.longValue(), n.longValue(), max.longValue());
				}
				// get prime that is close to max but not larger than it
				DiffieHellmanGroups.DHGroup group = DiffieHellmanGroups.getSafePrime(new UnsignedInteger32(Math.min(maxSupportedSize,
						Math.max(Math.min(n.intValue(), transport.getContext().getMaxDHGroupExchangeKeySize()),
								transport.getContext().getMinDHGroupExchangeKeySize()))));
				
				p = group.getP();
				g = group.getG();
				
				if(Log.isDebugEnabled()) {
					Log.debug("Selected {} bit prime and {} bit group", p.bitLength(), g.bitLength());
				}
				break;
			}
			default:
				// this message is not for us
				return false;
			}

			if(!Boolean.getBoolean("maverick.disableAsyncKex")) {
				transport.getContext().getExecutorService().submit(new Runnable() {
					public void run() {
						prepareGroup();
					}
				});
			} else {
				prepareGroup();
			}

			return true;

		} finally {
			bar.close();
		}
	}

	public void prepareGroup() {
		
		try {
			int retry = 3;
			KeyPair dhKeyPair;
			
			do {

				if (retry == 0) {
					transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
							"Failed to generate key exchange value");
					return;
				}

				retry--;

				DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
				dhKeyPairGen.initialize(dhSkipParamSpec);

				dhKeyPair = dhKeyPairGen.generateKeyPair();
				dhKeyAgreement.init(dhKeyPair.getPrivate());

				f = ((DHPublicKey) dhKeyPair.getPublic()).getY();

		    } while(!DiffieHellmanGroups.verifyParameters(f, p)); 
	
			// return p and g to client
			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					buf.put((byte) SSH_MSG_KEY_DH_GEX_GROUP);
					byte[] tmp = p.toByteArray();
					buf.putInt(tmp.length);
					buf.put(tmp);

					tmp = g.toByteArray();
					buf.putInt(tmp.length);
					buf.put(tmp);

					return true;
				}

				public void messageSent(Long sequenceNo) {
					if(Log.isDebugEnabled()) {
						Log.debug("Sent SSH_MSG_KEY_DH_GEX_GROUP p={}, g={}", p.toString(16), g.toString(16));
					}
				}
			}, true);
			
		} catch (InvalidKeyException ex) {
			transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, ex.getMessage());
		} catch (InvalidAlgorithmParameterException ex) {
			transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, ex.getMessage());
		}
	}
	
	public boolean processMessage(byte[] msg) throws SshException, IOException {

		if (exchangeGroup(msg)) {
			return true;
		}
		ByteArrayReader bar = new ByteArrayReader(msg);

		try {
			if (bar.read() != SSH_MSG_KEY_DH_GEX_INIT) {
				// this message is not for us
				return false;
			}
			
			e = bar.readBigInteger();
			
			if(Log.isDebugEnabled()) {
				Log.debug("Recieved SSH_MSG_KEY_DH_GEX_INIT e={}", e.toString(16));
			}
			
			if (!DiffieHellmanGroups.verifyParameters(e, p)) {
				throw new SshException(String.format("Key exchange detected invalid e value {}", e.toString(16)),
						SshException.PROTOCOL_VIOLATION);
			}
			
			if(!Boolean.getBoolean("maverick.disableAsyncKex")) {
				transport.getContext().getExecutorService().submit(new Runnable() {
					public void run() {
						doKex();
					}
				});
			} else {
				doKex();
			}

			return true;
		} finally {
			bar.close();
		}
	}

	private void doKex() {

		try {
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
			
			if(!DiffieHellmanGroups.verifyParameters(secret, p)) {
				throw new SshException(String.format("Key exchange detected invalid k value {}", e.toString(16)),
						SshException.PROTOCOL_VIOLATION);
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
						buf.put((byte) SSH_MSG_KEY_DH_GEX_REPLY);
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
					if(Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_KEY_DH_GEX_REPLY f={}", f.toString(16));
				}
			}, true);

			transport.sendNewKeys();
		
		} catch (Exception e) {
			transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED, e.getMessage());
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
	 *  UnsignedInteger32	min
	 *  UnsignedInteger32	n
	 *  UnsignedInteger32	max
	 *  BigInteger	p
	 *  BigInteger	g
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

		// if using SSH_MSG_KEY_DH_GEX_REQUEST_OLD then we wont have a min value
		if (min != null) {
			// min, going into a hash so doesn't matter about converting to an
			// int
			hash.putInt(min.intValue());
		}

		// n, going into a hash so doesn't matter about converting to an int
		hash.putInt(n.intValue());

		// if using SSH_MSG_KEY_DH_GEX_REQUEST_OLD then we wont have a max value
		if (max != null) {
			// max, going into a hash so doesn't matter about converting to an
			// int
			hash.putInt(max.intValue());
		}

		// the safe prime
		hash.putBigInteger(p);
		// the generator
		hash.putBigInteger(g);

		// The diffie hellman e value
		hash.putBigInteger(e);

		// The diffie hellman f value
		hash.putBigInteger(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

	@Override
	public void test() {
		try {
			ComponentManager.getDefaultInstance().supportedDigests().getInstance(getHashAlgorithm());
			initCrypto();

			if(minSupportedSize==-1) {
				
				Provider provider = JCEComponentManager.getProviderForAlgorithm(JCEAlgorithms.JCE_DH);
				 if(provider!=null && provider.getName().equals("BC")) {
					 minSupportedSize = 1024;
					 maxSupportedSize = 8192;
					 
				  	 if(Log.isInfoEnabled()) {
						Log.info("Using BC for DH; prime range is {} to {} bits", minSupportedSize, maxSupportedSize);
				  	 }
				 } else {
					maxSupportedSize = -1;
					for (BigInteger p : DiffieHellmanGroups.allDefaultGroups()) {
						try {
							DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, TWO);
							dhKeyPairGen.initialize(dhSkipParamSpec);
							KeyPair dhKeyPair = dhKeyPairGen.generateKeyPair();
							dhKeyAgreement.init(dhKeyPair.getPrivate());
							if(minSupportedSize==-1) {
								minSupportedSize = p.bitLength();
							}
							maxSupportedSize = p.bitLength();
						} catch (Exception e) {
							continue;
						}
					}
		
					if(maxSupportedSize==-1) {
						throw new IllegalStateException("The diffie hellman algorithm does not appear to be configured correctly on this machine");
					}
					
					if(Log.isInfoEnabled()) {
						Log.info("The supported DH prime range is {} to {} bits", minSupportedSize, maxSupportedSize);
					}
				 }
			}

		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
}
