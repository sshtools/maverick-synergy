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
package com.sshtools.agent.openssh;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.agent.server.SshAgentConnection;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Represents a single connection on the agent server.
 */
public class OpenSSHAgentConnection implements Runnable, SshAgentConnection {
   
	private static final int SSH_AGENT_RSA_SHA2_256 = 2;
	private static final int SSH_AGENT_RSA_SHA2_512 = 4;
	
    InputStream in;
    OutputStream out;
    KeyStore keystore;
    Closeable closeable;
    boolean isRFCAgent = true;
    
    public OpenSSHAgentConnection(KeyStore keystore, InputStream in, OutputStream out, Closeable closeable) {
        this.keystore = keystore;
        this.in = in;
        this.out = out;
        this.closeable = closeable;
    }


    /**
     * Send a success message.
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentSuccess() throws IOException {
        AgentMessage msg = new AgentMessage(OpenSSHAgentMessages.SSH_AGENT_SUCCESS);
        sendMessage(msg);
    }

    /**
     * Send a failure message
     *
     * @param errorcode the error code of the failure
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentFailure() throws IOException {
        SshAgentFailure msg = new SshAgentFailure();
        sendMessage(msg);
    }

    /**
     * Send the agents key list to the remote side. This supplies all the
     * public keys.
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentKeyList() throws IOException {
        AgentMessage msg=null;
    	msg = new SshAgentKeyList(keystore.getPublicKeys());
    	sendMessage(msg);
    }

    /**
     * Send the completed signing operation data.
     *
     * @param data the data generating from the signing operation
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendOperationComplete(byte[] data) throws IOException {
        SshAgentOperationComplete msg = new SshAgentOperationComplete(data);
        sendMessage(msg);
    }


    /**
     * Sends a subsystem message.
     *
     * @param msg the subsystem message to send
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendMessage(AgentMessage msg) throws IOException {
        Log.info("Sending message " + msg.getMessageName());

        byte[] msgdata;
		try {
			msgdata = msg.toByteArray();
			 out.write(ByteArrayWriter.encodeInt(msgdata.length));
		     out.write(msgdata);
		     out.flush();
		} catch (InvalidMessageException e) {
			throw new IOException(e.getMessage());
		}
       
    }

    /**
     * Called when the remote side adds a key the agent.
     *
     * @param msg the message containing the key
     *
     * @throws IOException if an IO error occurs
     */
    protected void onAddKey(SshAgentAddKey msg) throws IOException {
    	if(!keystore.isLocked()){
    		if (keystore.addKey(msg.getPrivateKey(), msg.getPublicKey(),
                    msg.getDescription(), msg.getKeyConstraints())) {
    			sendAgentSuccess();
    		} else {
    			sendAgentFailure();
    		}
        }else{
        	sendAgentFailure();
        }
    	
    }

    /**
     * Called when the remote side requests that all keys be removed from the
     * agent.
     *
     * @param msg the delete all keys message
     *
     * @throws IOException if an IO error occurs
     */
    protected void onDeleteAllKeys(AgentMessage msg)
        throws IOException {
    	if(!keystore.isLocked()){
    		if(keystore.deleteAllKeys()) {
    			sendAgentSuccess();
    		}     
        }
        
    	sendAgentFailure();
        
        
    }

    /**
     * Called by the remote side when a list of the agents keys is required
     *
     * @param msg the list all keys message
     *
     * @throws IOException if an IO error occurs
     */
    protected void onListKeys(AgentMessage msg) throws IOException {
    	if(!keystore.isLocked()){
    		sendAgentKeyList();
        }else{
        	sendAgentFailure();
        }
        
    }

    /**
     * Called by the remote side to initiate a private key operation.
     *
     * @param msg the private key operation message
     *
     * @throws IOException if an IO error occurs
     */
    protected void onPrivateKeyOp(SshAgentPrivateKeyOp msg)
        throws IOException {
        try {
        	SshPublicKey key = msg.getPublicKey();
            byte[] sig = keystore.performHashAndSign(key,
                        null, msg.getOperationData(), 
                        msg.getFlags().intValue());
            
            sendOperationComplete(encodeSignature(key, msg.getFlags().intValue(), sig));
            
        } catch (KeyTimeoutException ex) {
            sendAgentFailure();
        } catch (InvalidMessageException ex) {
            sendAgentFailure();
        } catch (SshException e) {
        	sendAgentFailure();
			
		}
    }

    protected byte[] encodeSignature(SshPublicKey key, int flags, byte[] signature) throws IOException {
		ByteArrayWriter sig = new ByteArrayWriter();

		try {
			switch(flags) {
			case SSH_AGENT_RSA_SHA2_256:
				sig.writeString("rsa-sha2-256");
				break;
			case SSH_AGENT_RSA_SHA2_512:
				sig.writeString("rsa-sha2-512");
				break;
			default:
				sig.writeString(key.getSigningAlgorithm());
				break;
			}
			
			sig.writeBinaryString(signature);
			return sig.toByteArray();
		} finally {
			sig.close();
		}
	}
    /**
     * Called by the remote side to delete a key from the agent
     *
     * @param msg the message containin the key to delete
     *
     * @throws IOException if an IO error occurs
     */
    protected void onDeleteKey(SshAgentDeleteKey msg) throws IOException {
        if (keystore.deleteKey(msg.getPublicKey())) {
            sendAgentSuccess();
        } else {
            sendAgentFailure();
        }
    }

    /**
     * Called by the remote side when the agent is to be locked
     *
     * @param msg the message containing a password
     *
     * @throws IOException if an IO error occurs
     */
    protected void onLock(SshAgentLock msg) throws IOException {
        if (keystore.lock(msg.getPassword())) {
            sendAgentSuccess();
        } else {
            sendAgentFailure();
        }
    }

    /**
     * Called by the remote side when the agent is to be unlocked
     *
     * @param msg the message containin the password
     *
     * @throws IOException if an IO error occurs
     */
    protected void onUnlock(SshAgentUnlock msg) throws IOException {
        if (keystore.unlock(msg.getPassword())) {
            sendAgentSuccess();
        } else {
            sendAgentFailure();
        }
    }

    /**
     * The connection thread
     */
    public void run() {
        try {
            Log.info("Starting agent connection thread");

            byte[] lendata = new byte[4];
            byte[] msgdata;
            int len;
            int read;
            boolean alive = true;

            while (alive) {
                // Read the first 4 bytes to determine the length of the message
                len = 0;

                while (len < lendata.length) {
                    try {
                        read = 0;
                        read = in.read(lendata, len, lendata.length - len);

                        if (read >= 0) {
                            len += read;
                        } else {
                            alive = false;

                            break;
                        }
                    } catch (InterruptedIOException ex) {
                        if (ex.bytesTransferred > 0) {
                            len += ex.bytesTransferred;
                        }
                    }
                }

                if (alive) {
                    len = (int) ByteArrayReader.readInt(lendata, 0);
                    msgdata = new byte[len];
                    len = 0;

                    while (len < msgdata.length) {
                        try {
                            len += in.read(msgdata, len, msgdata.length - len);
                        } catch (InterruptedIOException ex1) {
                            len += ex1.bytesTransferred;
                        }
                    }

                    onMessageReceived(msgdata);
                }
            }
        } catch (IOException ex) {
            Log.info("The agent connection terminated", ex);
        } finally {
            try {
            	if(closeable!=null) {
            		closeable.close();
            	}
            } catch (Exception ex) {
            }
        }

        Log.info("Exiting agent connection thread");
        
    }

    /**
     * Process a message and route to the handler method
     *
     * @param msgdata the raw message received
     *
     * @throws IOException if an IO error occurs
     */
    protected void onMessageReceived(byte[] msgdata) throws IOException {
       try{
    	switch ((int) (msgdata[0] & 0xFF)) {

        case OpenSSHAgentMessages.SSH2_AGENTC_ADD_IDENTITY: {
            Log.info("Adding key to agent");

            SshAgentAddKey msg = new SshAgentAddKey();
            msg.fromByteArray(msgdata);
			onAddKey(msg);

            break;
        }

        case OpenSSHAgentMessages.SSH2_AGENTC_ADD_ID_CONSTRAINED: {
        
        	break;
        }
        
        case OpenSSHAgentMessages.SSH2_AGENTC_REQUEST_IDENTITIES: {
            Log.info("Listing agent keys");

            AgentMessage msg = new AgentMessage(OpenSSHAgentMessages.SSH2_AGENTC_REQUEST_IDENTITIES);
            msg.fromByteArray(msgdata);
            onListKeys(msg);

            break;
        }
        
        case OpenSSHAgentMessages.SSH2_AGENTC_REMOVE_IDENTITY: {
            Log.info("Deleting key from agent");

            SshAgentDeleteKey msg = new SshAgentDeleteKey();
            msg.fromByteArray(msgdata);
            onDeleteKey(msg);

            break;
        }
        
        case OpenSSHAgentMessages.SSH2_AGENTC_REMOVE_ALL_IDENTITIES: {
            Log.info("Deleting all keys from agent");

            AgentMessage msg = new AgentMessage(OpenSSHAgentMessages.SSH2_AGENTC_REMOVE_ALL_IDENTITIES);
            msg.fromByteArray(msgdata);
			onDeleteAllKeys(msg);

            break;
        }

        case OpenSSHAgentMessages.SSH2_AGENTC_SIGN_REQUEST: {
            Log.info("Performing agent private key operation");

            SshAgentPrivateKeyOp msg = new SshAgentPrivateKeyOp();
            msg.fromByteArray(msgdata);
            onPrivateKeyOp(msg);

            break;
        }

        case OpenSSHAgentMessages.SSH_AGENTC_LOCK: {
            Log.info("Locking agent");

            SshAgentLock msg = new SshAgentLock(isRFCAgent);
            msg.fromByteArray(msgdata);
            onLock(msg);

            break;
        }

        case OpenSSHAgentMessages.SSH_AGENTC_UNLOCK: {
            Log.info("Unlocking agent");

            SshAgentUnlock msg = new SshAgentUnlock(isRFCAgent);
            msg.fromByteArray(msgdata);
            onUnlock(msg);

            break;
        }


        default: {
           Log.info("Unrecognized message type " + String.valueOf(msgdata[0]) + " received");
        }
    	}
       } catch (InvalidMessageException e) {
			e.printStackTrace();
	   }
    }
}
