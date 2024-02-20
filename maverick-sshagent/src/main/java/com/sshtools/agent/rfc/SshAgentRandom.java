package com.sshtools.agent.rfc;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentRandom extends AgentMessage {
    
    private int length;

    /**
     * Creates a new SshAgentRandom object.
     */
    public SshAgentRandom() {
        super(RFCAgentMessages.SSH_AGENT_RANDOM);
    }

    /**
     * Creates a new SshAgentRandom object.
     *
     * @param length
     */
    public SshAgentRandom(int length) {
        super(RFCAgentMessages.SSH_AGENT_RANDOM);
        this.length = length;
    }

    /**
     *
     *
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_RANDOM";
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
            baw.writeInt(length);
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
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
            length = (int) bar.readInt();
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
