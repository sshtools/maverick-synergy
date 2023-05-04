package com.sshtools.agent.openssh;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentOperationComplete extends AgentMessage {
    
    private byte[] data;

    /**
     * Creates a new SshAgentOperationComplete object.
     */
    public SshAgentOperationComplete() {
        super(OpenSSHAgentMessages.SSH2_AGENT_SIGN_RESPONSE);
    }

    /**
     * Creates a new SshAgentOperationComplete object.
     *
     * @param data
     */
    public SshAgentOperationComplete(byte[] data) {
        super(OpenSSHAgentMessages.SSH2_AGENT_SIGN_RESPONSE);
        this.data = data;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getData() {
        return data;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH2_AGENT_SIGN_RESPONSE";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws java.io.IOException
     * @throws com.sshtools.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            baw.writeBinaryString(data);
        } catch (IOException ioe) {
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
     */
    public void constructMessage(ByteArrayReader bar)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            data = bar.readBinaryString();
        } catch (IOException ioe) {
        }
    }
}
