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

package com.sshtools.client;

import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.synergy.ssh.SshTransport;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

/**
 *  An abstract base class for all key exchange implementations.
 */
public abstract class SshKeyExchangeClient implements SshKeyExchange<SshClientContext> {
	/**
     * The secret value produced during key exchange.
     */
    protected BigInteger secret;

    /**
     * The exchange hash produced during key exchange.
     */
    protected byte[] exchangeHash;

    /**
     * The server's host key.
     */
    protected byte[] hostKey;

    /**
     * The signature generated over the exchange hash
     */
    protected byte[] signature;

    protected String clientId;
    protected String serverId;
    protected byte[] clientKexInit;
    protected byte[] serverKexInit;
    protected SshPublicKey key;
    protected boolean firstPacketFollows;
    protected boolean useFirstPacket;
    boolean sentNewKeys = false;
    boolean receivedNewKeys = false;

    protected BigInteger e = null;
    protected BigInteger f = null;
    
    final private SecurityLevel securityLevel;
    final int priority;
    /**
     * The transport protocol for sending/receiving messages
     */
    protected SshTransport<SshClientContext> transport;
    
    String hashAlgorithm;
    
    /**
     * Contruct an uninitialized key exchange
     */
    public SshKeyExchangeClient(String hashAlgorithm, SecurityLevel securityLevel, int priority) {
    		this.hashAlgorithm = hashAlgorithm;
    		this.securityLevel = securityLevel;
    		this.priority = priority;
    }
    
    public SecurityLevel getSecurityLevel() {
    	return securityLevel;
    }
    

    public int getPriority() {
    	return priority;
    }

    public void setReceivedNewKeys(boolean receivedNewKeys) {
        this.receivedNewKeys = receivedNewKeys;
    }

    public void setSentNewKeys(boolean sentNewKeys) {
        this.sentNewKeys = sentNewKeys;
    }

    public boolean hasSentNewKeys() {
        return sentNewKeys;
    }

    public boolean hasReceivedNewKeys() {
        return receivedNewKeys;
    }

    /**
     * Get the output of the key exchange
     * 
     * @return the exchange hash output.
     */
    public byte[] getExchangeHash() {
        return exchangeHash;
    }

    /**
     * Get the host key supplied during key exchange.
     * 
     * @return the server's host key
     */
    public byte[] getHostKey() {
        return hostKey;
    }

    /**
     * Get the secret value produced during key exchange.
     * 
     * @return The secret value produced during key exchange
     */
    public BigInteger getSecret() {
        return secret;
    }

    /**
     * Get the signature produced during key exchange.
     * 
     * @return the signature produced from the exchange hash.
     */
    public byte[] getSignature() {
        return signature;
    }
    
    /**
     * Process a key exchange message
     * 
     * @param msg
     * @return boolean, indicating whether it has processed the message or not
     * @throws IOException
     */
    public abstract boolean processMessage(byte[] msg) throws SshException, IOException;

    /**
     * Reset the key exchange.
     */
    public void reset() {
        exchangeHash = null;
        hostKey = null;
        signature = null;
        secret = null;
    }
    
    public boolean isComplete() {
    		return sentNewKeys && receivedNewKeys;
    }
    
    public String getHashAlgorithm() {
    		return hashAlgorithm;
    }
    /**
     * <p>Calculates the exchange hash as an SHA1 hash of the following data.
     * <blockquote><pre>
     *  String         the client's version string (CR and NL excluded)
     *  String         the server's version string (CR and NL excluded)
     *  String         the payload of the client's SSH_MSG_KEXINIT
     *  String         the payload of the server's SSH_MSG_KEXINIT
     *  String         the host key
     *  BigInteger     e, exchange value sent by the client
     *  BigInteger     f, exchange value sent by the server
     *  BigInteger     K, the shared secret
     * </pre></blockquote></p>
     *
     * @throws IOException
     */
    protected void calculateExchangeHash() throws SshException {
  	  
  	  Digest hash = (Digest) transport.getContext().getComponentManager().supportedDigests().getInstance(hashAlgorithm);


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
