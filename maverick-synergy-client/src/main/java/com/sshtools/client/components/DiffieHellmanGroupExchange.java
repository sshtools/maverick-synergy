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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client.components;

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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.AbstractKeyExchange;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group-exchange-sha1".
 */
public class DiffieHellmanGroupExchange extends SshKeyExchangeClient
		implements AbstractKeyExchange {

	final static int SSH_MSG_KEY_DH_GEX_REQUEST_OLD = 30;
	final static int SSH_MSG_KEY_DH_GEX_GROUP = 31;
	final static int SSH_MSG_KEY_DH_GEX_INIT = 32;
	final static int SSH_MSG_KEY_DH_GEX_REPLY = 33;
	final static int SSH_MSG_KEY_DH_GEX_REQUEST = 34;

	final static BigInteger ONE = BigInteger.valueOf(1);
	final static BigInteger TWO = BigInteger.valueOf(2);

	/** generator, RFC recommends using 2 */
	BigInteger g = null;
	BigInteger p = null;
	BigInteger e = null;
	BigInteger f = null;
	BigInteger y = null;
	BigInteger x = null;
	
	UnsignedInteger32 min = null;
	UnsignedInteger32 n = null;
	UnsignedInteger32 max = null;

	KeyPairGenerator dhKeyPairGen;
	KeyAgreement dhKeyAgreement;
	KeyFactory dhKeyFactory;
	KeyPair dhKeyPair;
	
	String kexAlgorithm;
	String hashAlgorithm;
	
	static int maxSupportedSize = -1;
	static int minSupportedSize = -1;
	
	/**
	 * Construct an uninitialized instance.
	 */
	public DiffieHellmanGroupExchange(String kexAlgorithm, String hashAlgorithm) {
		super(hashAlgorithm);
	}

	/**
	 * Get the algorithm name for this key exchange
	 * 
	 * @return "diffie-hellman-group1-sha1"
	 */
	public String getAlgorithm() {
		return kexAlgorithm;
	}

	public void init(final SshTransport<SshClientContext> transport,
			String clientIdentification, String serverIdentification,
			byte[] clientKexInit, byte[] serverKexInit,
			boolean firstPacketFollows, boolean useFirstPacket)
			throws IOException {

		this.clientId = clientIdentification;
		this.serverId = serverIdentification;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;
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
		
		verifyDHPrimeThresholds();
		

		 transport.postMessage(new SshMessage() {
	          public boolean writeMessageIntoBuffer(ByteBuffer buf) {

	      	    int minimumSize = maybeLog("Minimum DH prime", Math.min(maxSupportedSize, Math.max(transport.getContext().getMinDHGroupExchangeKeySize(), 1024)));
	    		int preferredKeySize = maybeLog("Preferred DH prime", Math.min(maxSupportedSize, transport.getContext().getPreferredDHGroupExchangeKeySize()));
	    	    int maximumSize = maybeLog("Maximum DH prime", Math.min(maxSupportedSize, transport.getContext().getMaxDHGroupExchangeKeySize()));
	    	    
				buf.put((byte) SSH_MSG_KEY_DH_GEX_REQUEST);
				buf.putInt(minimumSize);
				min = new UnsignedInteger32(minimumSize);
				buf.putInt(preferredKeySize);
				n = new UnsignedInteger32(preferredKeySize);
				buf.putInt(maximumSize);
				max = new UnsignedInteger32(maximumSize);
				return true;
			}

			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEY_DH_GEX_REQUEST");
			}
		}, true);
	}

    private int maybeLog(String txt, int size) {
	  if(Log.isDebugEnabled()) {
		  Log.debug(String.format("%s size is %d", txt, size));
	  }
	  return size;
    }
	  
	private void verifyDHPrimeThresholds() {
		
		if(minSupportedSize == -1) {
	  		  
			 Provider provider = dhKeyAgreement.getProvider();
			 if(provider!=null && provider.getName().equals("BC")) {
				 minSupportedSize = 1024;
				 maxSupportedSize = 8192;
				 
			  	 if(Log.isInfoEnabled()) {
					Log.info(String.format("Using BC for DH; prime range is %d to %d bits", minSupportedSize, maxSupportedSize));
			  	 }
			 } else {
			 
			  	 for(BigInteger p : DiffieHellmanGroups.allDefaultGroups()) {
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
						e.printStackTrace();
						continue;
					}
			  	 }
			  	 if(maxSupportedSize==-1) {
					throw new IllegalStateException("The diffie hellman algorithm does not appear to be configured correctly on this machine");
			  	 }
					
			  	 if(maxSupportedSize < 2048) {
			  		 throw new IllegalStateException(
			  				 String.format("The maximum supported DH prime is %d bits which is smaller than this algorithm requires", maxSupportedSize));
			  	 }
			  	 
			  	 if(Log.isInfoEnabled()) {
					Log.info(String.format("The supported DH prime range is %d to %d bits", minSupportedSize, maxSupportedSize));
			  	 }
			 }
		  }
	  }

	private void initCrypto() throws NoSuchAlgorithmException {
		dhKeyFactory = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null ? KeyFactory
				.getInstance(JCEAlgorithms.JCE_DH) : KeyFactory
				.getInstance(JCEAlgorithms.JCE_DH, JCEProvider
						.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
		dhKeyPairGen = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null ? KeyPairGenerator
				.getInstance(JCEAlgorithms.JCE_DH) : KeyPairGenerator
				.getInstance(JCEAlgorithms.JCE_DH, JCEProvider
						.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
		dhKeyAgreement = JCEProvider
				.getProviderForAlgorithm(JCEAlgorithms.JCE_DH) == null ? KeyAgreement
				.getInstance(JCEAlgorithms.JCE_DH) : KeyAgreement
				.getInstance(JCEAlgorithms.JCE_DH, JCEProvider
						.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));
	}
	
	public String getProvider() {
		if (dhKeyAgreement != null)
			return dhKeyAgreement.getProvider().getName();
		else
			return "";
	}

	public boolean exchangeGroup(ByteArrayReader msg) throws SshException, IOException {

		switch (msg.read()) {
		case SSH_MSG_KEY_DH_GEX_GROUP:

			p = msg.readBigInteger();
			g = msg.readBigInteger();
			break;
		default:
			// this message is not for us
			return false;
		}

		if(Log.isDebugEnabled()) {
			Log.debug(String.format("Received %d bit DH prime with group %s", p.bitLength(), g.toString(16)));
		}
		
		if(p.bitLength() > maxSupportedSize) {
			throw new SshException(String.format(
						"Server sent a prime larger than our configuration can handle! p=%d, max=%d", 
					p.bitLength(), maxSupportedSize), SshException.INTERNAL_ERROR);
		}
		
		if(g.compareTo(BigInteger.ONE) <= 0) {
			throw new SshException("Invalid DH g value [" + g.toString(16) + "]", SshException.PROTOCOL_VIOLATION);
		}
		
		if(p.bitLength() < Math.max(min.longValue(), 1024L)) {
			throw new SshException("Minimum DH p value not provided [" + p.bitLength() + "]", SshException.PROTOCOL_VIOLATION);
		}
		
		try {
			if(Boolean.getBoolean("maverick.dhBypassJCE") || p.bitLength() % 64 != 0) {
				calculateE();
			} else {
				calculateEwithJCE();
			}
		} catch (Throwable ex) {
			throw new SshException("Failed to generate DH value",
					SshException.JCE_ERROR);
		} 

		final byte[] eBytes = e.toByteArray();
		
		 transport.postMessage(new SshMessage() {
	          public boolean writeMessageIntoBuffer(ByteBuffer buf) {

				buf.put((byte) SSH_MSG_KEY_DH_GEX_INIT);
				buf.putInt(eBytes.length);
				buf.put(eBytes);

				return true;
			}
	        
			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEXDH_INIT");
			}
		}, true);

		return true;
	}
	
	private void calculateKwithJCE() throws InvalidKeySpecException, InvalidKeyException, IllegalStateException {
		
		// Calculate diffie hellman k value
		DHPublicKeySpec spec = new DHPublicKeySpec(f, p, g);

		DHPublicKey key = (DHPublicKey) dhKeyFactory
				.generatePublic(spec);

		dhKeyAgreement.doPhase(key, true);

		byte[] tmp = dhKeyAgreement.generateSecret();
		if ((tmp[0] & 0x80) == 0x80) {
			byte[] tmp2 = new byte[tmp.length + 1];
			System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
			tmp = tmp2;
		}
		// Calculate diffe hellman k value
		secret = new BigInteger(tmp);
	}

	private void calculateK() {
		secret = f.modPow(x, p);
	}

	private void calculateEwithJCE() throws SshException, InvalidKeyException {
		
		KeyPair dhKeyPair = null;
		int retry = 3;

		do {
			if (retry == 0) {
				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
						"Failed to generate key exchange value");
				throw new SshException(
						"Key exchange failed to generate e value",
						SshException.INTERNAL_ERROR);
			}

			retry--;

			try {

				DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
				dhKeyPairGen.initialize(dhSkipParamSpec);

				dhKeyPair = dhKeyPairGen.generateKeyPair();
				dhKeyAgreement.init(dhKeyPair.getPrivate());
				
				e = ((DHPublicKey) dhKeyPair.getPublic()).getY();

			} catch (InvalidAlgorithmParameterException ex) {
				throw new SshException("Failed to generate DH value: " + ex.getMessage(),
						SshException.JCE_ERROR, ex);
			} 
		} while (e.compareTo(ONE) < 0 || e.compareTo(p.subtract(ONE)) > 0);
	}

	private void calculateE() throws SshException, NoSuchAlgorithmException {
		
		if(Log.isDebugEnabled()) {
			if(Boolean.getBoolean("maverick.dhBypassJCE")) {
				Log.debug("Performing DH e parameter calculation manually because it has been forced by system configuration");
			} else {
				Log.debug(String.format("Performing DH e parameter calculation manually because P bit length is not multiple of 64 [%d]", p.bitLength()));
			}
		}
		
		int retry = 3;

		do {
			if (retry == 0) {
				transport.disconnect(TransportProtocol.KEY_EXCHANGE_FAILED,
						"Failed to generate key exchange value");
				throw new SshException(
						"Key exchange failed to generate e value",
						SshException.INTERNAL_ERROR);
			}

			retry--;

			SecureRandom rnd = JCEComponentManager.getSecureRandom();
	        int minBits = g.bitLength();
	        int maxBits = p.subtract(BigInteger.ONE).divide(new BigInteger("2")).bitLength();
	        
	        int genBits = (int) ( ( (maxBits - minBits + 1) * rnd.nextFloat()) + minBits);
	        x = new BigInteger(genBits, rnd);
	        
	        // Calculate e
	        e = g.modPow(x, p);
		} while (e.compareTo(ONE) < 0 || e.compareTo(p.subtract(ONE)) > 0);

		
	}

	public boolean processMessage(byte[] m) throws SshException, IOException {

		
		ByteArrayReader msg = new ByteArrayReader(m);
		
		try {
			if (exchangeGroup(msg)) {
				return true;
			}
	
			msg.reset();
			
			if (msg.read() != SSH_MSG_KEY_DH_GEX_REPLY) {
				return false;
			}
	
			try {
				hostKey = msg.readBinaryString();
				f = msg.readBigInteger();
				signature = msg.readBinaryString();
	
				if(Log.isTraceEnabled()) {
					Log.trace("P: " + p.toString(16));
					Log.trace("G: " + g.toString(16));
					Log.trace("F: " + f.toString(16));
					Log.trace("E: " + e.toString(16));
				}
				
				if(Log.isDebugEnabled()) {
					Log.debug("Verifying server DH parameters");
				}
				
				if (!DiffieHellmanGroups.verifyParameters(f, p)) {
					throw new SshException(String.format("Key exchange detected invalid f value %s", f.toString(16)),
							SshException.PROTOCOL_VIOLATION);
				}
				
				if(Log.isDebugEnabled()) {
					Log.debug("Verified DH parameters. Performing DH calculations");
				}
				
				if(Boolean.getBoolean("maverick.dhBypassJCE") || p.bitLength() % 64 != 0) {
					calculateK();
				} else {
					calculateKwithJCE();
				}
				
				if(Log.isDebugEnabled()) {
					Log.debug("Verifying calculated DH parameters");
				}
				
				if(!DiffieHellmanGroups.verifyParameters(secret, p)) {
					throw new SshException(String.format("Key exchange detected invalid k value %s", e.toString(16)),
							SshException.PROTOCOL_VIOLATION);
				}
	
				if(Log.isDebugEnabled()) {
					Log.debug("Calculating exchange hash");
				}
				
				// Calculate the exchange hash
				calculateExchangeHash();
	
				if(Log.isDebugEnabled()) {
					Log.debug("Completed key exchange calculations");
				}
				
				transport.sendNewKeys();
	
			} catch (Exception ex) {
				throw new SshException(
						"Failed to read SSH_MSG_KEXDH_REPLY from message buffer",
						SshException.INTERNAL_ERROR, ex);
			}
	
			return true;
		} finally {
			msg.close();
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

		Digest hash = (Digest) transport.getContext().getComponentManager().supportedDigests()
				.getInstance(getHashAlgorithm());

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

		hash.putInt(min.intValue());
		hash.putInt(n.intValue());
		hash.putInt(max.intValue());

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
	
	public boolean isKeyExchangeMessage(int messageid) {
		switch (messageid) {
		case SSH_MSG_KEY_DH_GEX_REQUEST_OLD:
		case SSH_MSG_KEY_DH_GEX_GROUP:
		case SSH_MSG_KEY_DH_GEX_INIT:
		case SSH_MSG_KEY_DH_GEX_REPLY:
		case SSH_MSG_KEY_DH_GEX_REQUEST:
			return true;
		default:
			return false;
		}
	}

	public void init(SshTransport<SshClientContext> transport, String clientId,
			String serverId, byte[] clientKexInit, byte[] serverKexInit,
			SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket)
			throws IOException, SshException {
		this.init(transport, clientId, serverId, clientKexInit, serverKexInit, firstPacketFollows, useFirstPacket);
	}

	public void test() throws IOException, SshException {
		try {
			initCrypto();
		} catch(Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
