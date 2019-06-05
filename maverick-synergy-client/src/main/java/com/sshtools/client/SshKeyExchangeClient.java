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
package com.sshtools.client;

import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.ssh.components.SshKeyExchange;
import com.sshtools.common.ssh.components.SshPublicKey;

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
    
    /**
     * The transport protocol for sending/receiving messages
     */
    protected SshTransport<SshClientContext> transport;
    
    String hashAlgorithm;
    
    /**
     * Contruct an uninitialized key exchange
     */
    public SshKeyExchangeClient(String hashAlgorithm) {
    		this.hashAlgorithm = hashAlgorithm;
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
