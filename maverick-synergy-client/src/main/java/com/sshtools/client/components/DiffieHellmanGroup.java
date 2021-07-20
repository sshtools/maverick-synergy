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

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.ssh.SshTransport;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 */
public class DiffieHellmanGroup extends SshKeyExchangeClient {

  final static int SSH_MSG_KEXDH_INIT = 30;
  final static int SSH_MSG_KEXDH_REPLY = 31;

  final static BigInteger ONE = BigInteger.valueOf(1);
  final static BigInteger TWO = BigInteger.valueOf(2);

  
  /** generator, RFC recommends using 2*/
  final static BigInteger g = TWO;
  
  /** large safe prime, this comes from ....??*/
  BigInteger p = null;

  KeyPairGenerator dhKeyPairGen;
  KeyAgreement dhKeyAgreement;
  KeyFactory dhKeyFactory;
  KeyPair dhKeyPair;
  
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
   * @return "diffie-hellman-group14-sha1"
   */
  public String getAlgorithm() {
    return kexAlgorithm;
  }

	@Override
	public void init(SshTransport<SshClientContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {
    
	this.clientId = clientId;
    this.serverId = serverId;
    this.clientKexInit = clientKexInit;
    this.serverKexInit = serverKexInit;
    this.firstPacketFollows = firstPacketFollows;
    this.useFirstPacket = useFirstPacket;
    this.transport = transport;
	
    // Generate a random number y
    

       try {
    	   	initCrypto();
    	   	 e = ((DHPublicKey)dhKeyPair.getPublic()).getY();
       } catch(Exception ex) {
         throw new IOException("Failed to generate DH value: " + ex.getMessage());
       } 

       final byte[] eBytes = e.toByteArray();
       transport.postMessage(new SshMessage() {
	          public boolean writeMessageIntoBuffer(ByteBuffer buf) {

				buf.put((byte) SSH_MSG_KEXDH_INIT);
				buf.putInt(eBytes.length);
				buf.put(eBytes);

				return true;
			}
	        
			public void messageSent(Long sequenceNo) {
				if(Log.isDebugEnabled())
					Log.debug("Sent SSH_MSG_KEXDH_INIT");
			}
		}, true);
  }
  
	private void initCrypto() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
		 
    	dhKeyFactory = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH)==null ? 
       		  KeyFactory.getInstance(JCEAlgorithms.JCE_DH) : 
       			 KeyFactory.getInstance(JCEAlgorithms.JCE_DH, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH)); 
        dhKeyPairGen = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH)==null ? 
      		  KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH) : 
      	      KeyPairGenerator.getInstance(JCEAlgorithms.JCE_DH, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH)); 
        dhKeyAgreement = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH)==null ? 
      		  KeyAgreement.getInstance(JCEAlgorithms.JCE_DH) : 
      			  KeyAgreement.getInstance(JCEAlgorithms.JCE_DH, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DH));

  		DHParameterSpec dhSkipParamSpec = new DHParameterSpec(p, g);
        
        dhKeyPairGen.initialize(dhSkipParamSpec, JCEProvider.getSecureRandom());

        dhKeyPair = dhKeyPairGen.generateKeyPair();
        dhKeyAgreement.init(dhKeyPair.getPrivate());
    
	}
	
	public String getProvider() {
		if(dhKeyAgreement!=null)
			return dhKeyAgreement.getProvider().getName();
		else
			return "";
	}

  public boolean processMessage(byte[] m) throws SshException, IOException {

	ByteArrayReader msg = new ByteArrayReader(m);
	try {
		int msgId = msg.read();
	    
		switch (msgId) {
	      case SSH_MSG_KEXDH_REPLY:
	    	  
			
				try {
					hostKey = msg.readBinaryString();
					f = msg.readBigInteger();
					signature = msg.readBinaryString();
	
					if(Log.isDebugEnabled()) {
		    	    	Log.debug("Received SSH_MSG_KEXDH_INIT f={}", f.toString(16));
		    	    	Log.debug("Host key: {}", SshKeyUtils.getOpenSSHFormattedKey(
		    	    			SshPublicKeyFileFactory.decodeSSH2PublicKey(hostKey)));
		    	    	Log.debug("Signature: {}", Utils.bytesToHex(signature));
		    	    }
					
					DHPublicKeySpec spec = new DHPublicKeySpec(f, p, g);
	
					dhKeyAgreement.doPhase(dhKeyFactory.generatePublic(spec), true);
	
					byte[] tmp = dhKeyAgreement.generateSecret();
					if ((tmp[0] & 0x80) == 0x80) {
						byte[] tmp2 = new byte[tmp.length + 1];
						System.arraycopy(tmp, 0, tmp2, 1, tmp.length);
						tmp = tmp2;
					}
					// Calculate diffe hellman k value
					secret = new BigInteger(tmp);
	
					// Calculate the exchange hash
					calculateExchangeHash();
	
					transport.sendNewKeys();
					
				} catch (Exception ex) {
					throw new SshException(
							"Failed to read SSH_MSG_KEXDH_REPLY from message buffer",
							SshException.INTERNAL_ERROR, ex);
				}
	
				return true;
	
	      default:
	        return false;
	    }
	} finally {
		msg.close();
	}
  }
  
   public boolean isKeyExchangeMessage(int messageid) {
		switch (messageid) {
		case SSH_MSG_KEXDH_INIT:
		case SSH_MSG_KEXDH_REPLY:
			return true;
		default:
			return false;
		}
	}
	
	public void test() throws IOException, SshException {
		try {
			initCrypto();
		} catch(Exception e) {
			throw new IOException(e.getMessage(), e);
		}
	}
}
