package com.maverick.agent.rfc;



import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.maverick.agent.openssh.OpenSSHAgentMessages;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentUnlock extends AgentMessage {
    /**  */
    
    String password;

    /**
     * Creates a new SshAgentUnlock object.
     */
    public SshAgentUnlock(boolean isRFCAgent) {
        super(isRFCAgent ? RFCAgentMessages.SSH_AGENT_UNLOCK : OpenSSHAgentMessages.SSH_AGENTC_UNLOCK);
    }

    /**
     * Creates a new SshAgentUnlock object.
     *
     * @param password
     */
    public SshAgentUnlock(boolean isRFCAgent, String password) {
        super(isRFCAgent ? RFCAgentMessages.SSH_AGENT_UNLOCK : OpenSSHAgentMessages.SSH_AGENTC_UNLOCK);
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
        return "SSH_AGENT_UNLOCK";
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
