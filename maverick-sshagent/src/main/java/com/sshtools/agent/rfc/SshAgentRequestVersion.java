package com.sshtools.agent.rfc;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentRequestVersion extends AgentMessage {
    String version;

    /**
     * Creates a new SshAgentRequestVersion object.
     */
    public SshAgentRequestVersion() {
        super(RFCAgentMessages.SSH_AGENT_REQUEST_VERSION);
    }

    /**
     * Creates a new SshAgentRequestVersion object.
     *
     * @param version
     */
    public SshAgentRequestVersion(String version) {
        super(RFCAgentMessages.SSH_AGENT_REQUEST_VERSION);
        this.version = version;
    }

    /**
     *
     *
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_REQUEST_VERSION";
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
            baw.writeString(version);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
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
            version = bar.readString();
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
