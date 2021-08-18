/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.agent.rfc;

import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
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
