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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.agent.rfc;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Vector;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.ForwardingNotice;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.agent.server.SshAgentConnection;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Represents a single connection on the agent server.
 *
 */
public class RFCAgentConnection implements Runnable, SshAgentConnection {
    private 
    InputStream in;
    OutputStream out;
    KeyStore keystore;
    Thread thread;
    Vector<ForwardingNotice> forwardingNodes = new Vector<ForwardingNotice>();
    Closeable closeable;
    boolean isRFCAgent = true;
    
    public RFCAgentConnection(KeyStore keystore, InputStream in, OutputStream out, Closeable closeable) {
        this.keystore = keystore;
        this.in = in;
        this.out = out;
        this.closeable = closeable;
        thread = new Thread(this);
        thread.start();
    }
    
    /**
     * Send a success message.
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentSuccess() throws IOException {
        AgentMessage msg = new AgentMessage(RFCAgentMessages.SSH_AGENT_SUCCESS);
        sendMessage(msg);
    }

    /**
     * Send a failure message
     *
     * @param errorcode the error code of the failure
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentFailure(int errorcode) throws IOException {
        SshAgentFailure msg = new SshAgentFailure(errorcode);
        sendMessage(msg);
    }

    /**
     * Send the version response; this class currently implements version 2
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendVersionResponse() throws IOException {
         SshAgentVersionResponse msg = new SshAgentVersionResponse(RFCAgentMessages.SSH_AGENT_VERSION);
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
     * Send some random data to the remote side.
     *
     * @param data some random data
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendRandomData(byte[] data) throws IOException {
    	SshAgentRandomData msg = new SshAgentRandomData(data);
        sendMessage(msg);
    }

    /**
     * Send the agent alive message. This is sent to test whether the agent is
     * still active
     *
     * @param padding some random padding for the message
     *
     * @throws IOException if an IO error occurs
     */
    protected void sendAgentAlive(byte[] padding) throws IOException {
		if (!keystore.isLocked()) {
			SshAgentAlive msg = new SshAgentAlive(padding);
			sendMessage(msg);
		} else {
			sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
		}
        
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
     * Called when a forwarding notice is recceived from the remote side.
     *
     * @param msg the forwarding notice
     */
    protected void onForwardingNotice(SshAgentForwardingNotice msg) {
        forwardingNodes.add(new ForwardingNotice(msg.getRemoteHostname(),
                msg.getRemoteIPAddress(), msg.getRemotePort()));
    }

    /**
     * Called when the remote side requests the version number of this
     * protocol.
     *
     * @param msg the version request message
     *
     * @throws IOException if an IO error occurs
     */
    protected void onRequestVersion(SshAgentRequestVersion msg)
        throws IOException {
    	
        sendVersionResponse();
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
    			sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
    		}
        }else{
        	sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
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
    		keystore.deleteAllKeys();
            sendAgentSuccess();
        }else{
        	sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
        }
        
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
        	sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
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
            if (msg.getOperation().equals("sign")) {
                sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_SUITABLE);
            } else if (msg.getOperation().equals("hash-and-sign")) {
                byte[] sig = keystore.performHashAndSign(msg.getPublicKey(),
                        forwardingNodes, msg.getOperationData(), 0);
                sendOperationComplete(sig);
            } else if (msg.getOperation().equals("decrypt")) {
                sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_SUITABLE);
            } else if (msg.getOperation().equals("ssh1-challenge-response")) {
                sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_SUITABLE);
            } else {
                sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_UNSUPPORTED_OP);
            }
        } catch (KeyTimeoutException ex) {
            sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_TIMEOUT);
        } catch (InvalidMessageException ex) {
            sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_FOUND);
        } catch (SshException e) {
        	sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_SUITABLE);
			
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
            sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_KEY_NOT_FOUND);
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
            sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
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
            sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
        }
    }

    /**
     * Called when a ping message is received
     *
     * @param msg the ping message containing some padding
     *
     * @throws IOException if an IO error occurs
     */
    protected void onPing(SshAgentPing msg) throws IOException {
        sendAgentAlive(msg.getPadding());
    }

    /**
     * Called when the remote side sends a random message
     *
     * @param msg the random message
     *
     * @throws IOException if an IO error occurs
     */
    protected void onRandom(SshAgentRandom msg) throws IOException {
    	if(!keystore.isLocked()){
    		if (msg.getLength() > 0) {
                byte[] random = new byte[msg.getLength()];
                new SecureRandom().nextBytes(random);
                sendRandomData(random);
            } else {
                sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_FAILURE);
            }
        }else{
        	sendAgentFailure(RFCAgentMessages.SSH_AGENT_ERROR_DENIED);
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
            Log.info("The agent connection terminated");
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
        case RFCAgentMessages.SSH_AGENT_FORWARDING_NOTICE: {
            Log.info("Agent forwarding notice received");

            SshAgentForwardingNotice msg = new SshAgentForwardingNotice();
            msg.fromByteArray(msgdata);
            onForwardingNotice(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_REQUEST_VERSION: {
            Log.info("Agent version request received");

            SshAgentRequestVersion msg = new SshAgentRequestVersion();
            msg.fromByteArray(msgdata);
			onRequestVersion(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_ADD_KEY: {
            Log.info("Adding key to agent");

            SshAgentAddKey msg = new SshAgentAddKey();
            msg.fromByteArray(msgdata);
			onAddKey(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_DELETE_ALL_KEYS: {
            Log.info("Deleting all keys from agent");

            AgentMessage msg = new AgentMessage(RFCAgentMessages.SSH_AGENT_DELETE_ALL_KEYS);
            msg.fromByteArray(msgdata);
			onDeleteAllKeys(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_LIST_KEYS: {
            Log.info("Listing agent keys");

            AgentMessage msg = new AgentMessage(RFCAgentMessages.SSH_AGENT_LIST_KEYS);
            msg.fromByteArray(msgdata);
            onListKeys(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_PRIVATE_KEY_OP: {
            Log.info("Performing agent private key operation");

            SshAgentPrivateKeyOp msg = new SshAgentPrivateKeyOp();
            msg.fromByteArray(msgdata);
            onPrivateKeyOp(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_DELETE_KEY: {
            Log.info("Deleting key from agent");

            SshAgentDeleteKey msg = new SshAgentDeleteKey();
            msg.fromByteArray(msgdata);
            onDeleteKey(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_LOCK: {
            Log.info("Locking agent");

            SshAgentLock msg = new SshAgentLock(isRFCAgent);
            msg.fromByteArray(msgdata);
            onLock(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_UNLOCK: {
            Log.info("Unlocking agent");

            SshAgentUnlock msg = new SshAgentUnlock(isRFCAgent);
            msg.fromByteArray(msgdata);
            onUnlock(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_PING: {
            Log.info("Ping Ping Ping Ping Ping");

            SshAgentPing msg = new SshAgentPing();
            msg.fromByteArray(msgdata);
            onPing(msg);

            break;
        }

        case RFCAgentMessages.SSH_AGENT_RANDOM: {
            Log.info("Random message received");

            SshAgentRandom msg = new SshAgentRandom();
            msg.fromByteArray(msgdata);
            onRandom(msg);

            break;
        }

        default:
            throw new IOException("Unrecognized message type " +
                String.valueOf(msgdata[0]) + " received");
        }
       } catch (InvalidMessageException e) {
			e.printStackTrace();
	   }
       System.out.println("Key store size :"+keystore.size());
    }
}
