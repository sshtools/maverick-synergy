
package com.sshtools.agent.rfc;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;


public class SshAgentVersionResponse extends AgentMessage {
    /**  */
    
    UnsignedInteger32 version;

    /**
     * Creates a new SshAgentVersionResponse object.
     */
    public SshAgentVersionResponse() {
        super(RFCAgentMessages.SSH_AGENT_VERSION_RESPONSE);
    }

    /**
     * Creates a new SshAgentVersionResponse object.
     *
     * @param version
     */
    public SshAgentVersionResponse(int version) {
        super(RFCAgentMessages.SSH_AGENT_VERSION_RESPONSE);
        this.version = new UnsignedInteger32(version);
    }

    /**
     *
     *
     * @return
     */
    public int getVersion() {
        return version.intValue();
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_VERSION_RESPONSE";
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
            baw.writeUINT32(version);
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
            version = bar.readUINT32();
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
