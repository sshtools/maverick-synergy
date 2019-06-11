package com.maverick.agent.rfc;


import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.maverick.agent.openssh.OpenSSHAgentMessages;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentLock extends AgentMessage {
    
    String password;

    /**
     * Creates a new SshAgentLock object.
     */
    public SshAgentLock(boolean isRFCAgent) {
        super(isRFCAgent ? RFCAgentMessages.SSH_AGENT_LOCK : OpenSSHAgentMessages.SSH_AGENTC_LOCK);
    }

    /**
     * Creates a new SshAgentLock object.
     *
     * @param password
     */
    public SshAgentLock(boolean isRFCAgent, String password) {
        super(isRFCAgent ? RFCAgentMessages.SSH_AGENT_LOCK : OpenSSHAgentMessages.SSH_AGENTC_LOCK);
        this.password = password;
    }

    /**
     *
     *
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_LOCK";
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
            baw.writeString(password);
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
            password = bar.readString();
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
