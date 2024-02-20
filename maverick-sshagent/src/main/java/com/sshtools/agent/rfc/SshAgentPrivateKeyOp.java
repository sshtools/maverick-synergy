package com.sshtools.agent.rfc;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
/**
 * Message for Agent Private key operation
 * @author Aruna Abesekara
 * 
 *
 */

public class SshAgentPrivateKeyOp extends AgentMessage {
   
    SshPublicKey pubkey;
    String operation;
    byte[] data;

    /**
     * Creates a new SshAgentPrivateKeyOp object.
     */
    public SshAgentPrivateKeyOp() {
        super(RFCAgentMessages.SSH_AGENT_PRIVATE_KEY_OP);
    }

    /**
     * Creates a new SshAgentPrivateKeyOp object.
     *
     * @param pubkey
     * @param operation
     * @param data
     */
    public SshAgentPrivateKeyOp(SshPublicKey pubkey, String operation,
        byte[] data) {
        super(RFCAgentMessages.SSH_AGENT_PRIVATE_KEY_OP);
        this.pubkey = pubkey;
        this.operation = operation;
        this.data = data;
    }

    /**
     *
     *
     * @return
     */
    public SshPublicKey getPublicKey() {
        return pubkey;
    }

    /**
     *
     *
     * @return
     */
    public String getOperation() {
        return operation;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getOperationData() {
        return data;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_PRIVATE_KEY_OP";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws java.io.IOException
     * @throws com.sshtools.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            baw.writeString(operation);
            baw.writeBinaryString(pubkey.getEncoded());
            baw.writeBinaryString(data);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        } catch (com.sshtools.common.ssh.SshException e) {
        	throw new InvalidMessageException(e.getMessage(),SshException.AGENT_ERROR);
		}
    }

    /**
     *
     *
     * @param bar
     *
     * @throws java.io.IOException
     * @throws com.sshtools.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            operation = bar.readString();
            pubkey = SshPublicKeyFileFactory.decodeSSH2PublicKey(bar.readBinaryString());
            data = bar.readBinaryString();
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
