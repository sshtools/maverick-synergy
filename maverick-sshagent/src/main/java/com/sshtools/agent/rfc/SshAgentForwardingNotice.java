package com.maverick.agent.rfc;

import java.io.IOException;

import com.maverick.agent.AgentMessage;
import com.maverick.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;


public class SshAgentForwardingNotice extends AgentMessage {
    String remoteHostname;
    String remoteIPAddress;
    UnsignedInteger32 remotePort;

    /**
     * Creates a new SshAgentForwardingNotice object.
     */
    public SshAgentForwardingNotice() {
        super(RFCAgentMessages.SSH_AGENT_FORWARDING_NOTICE);
    }

    /**
     * Creates a new SshAgentForwardingNotice object.
     *
     * @param remoteHostname
     * @param remoteIPAddress
     * @param remotePort
     */
    public SshAgentForwardingNotice(String remoteHostname,
        String remoteIPAddress, int remotePort) {
        super(RFCAgentMessages.SSH_AGENT_FORWARDING_NOTICE);
        this.remoteHostname = remoteHostname;
        this.remoteIPAddress = remoteIPAddress;
        this.remotePort = new UnsignedInteger32(remotePort);
    }

    /**
     *
     *
     * @return
     */
    public String getRemoteHostname() {
        return remoteHostname;
    }

    /**
     *
     *
     * @return
     */
    public String getRemoteIPAddress() {
        return remoteIPAddress;
    }

    /**
     *
     *
     * @return
     */
    public int getRemotePort() {
        return remotePort.intValue();
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_FORWARDING_NOTICE";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws java.io.IOException
     * @throws com.maverick.agent.exceptions.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            baw.writeString(remoteHostname);
            baw.writeString(remoteIPAddress);
            baw.writeUINT32(remotePort);
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(), SshException.AGENT_ERROR);
        }
    }

    /**
     *
     *
     * @param bar
     *
     * @throws java.io.IOException
     * @throws com.maverick.agent.exceptions.j2ssh.transport.InvalidMessageException DOCUMENT
     *         ME!
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws java.io.IOException, 
            InvalidMessageException {
        try {
            remoteHostname = bar.readString();
            remoteIPAddress = bar.readString();
            remotePort = bar.readUINT32();
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(), SshException.AGENT_ERROR);
        }
    }
}
