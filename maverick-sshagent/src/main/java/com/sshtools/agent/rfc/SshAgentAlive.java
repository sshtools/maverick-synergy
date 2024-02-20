package com.sshtools.agent.rfc;

/*-
 * #%L
 * Key Agent
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */


import java.io.IOException;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentAlive extends AgentMessage {
    
    private byte[] padding;

    /**
     * Creates a new SshAgentAlive object.
     */
    public SshAgentAlive() {
        super(RFCAgentMessages.SSH_AGENT_ALIVE);
    }

    /**
     * Creates a new SshAgentAlive object.
     *
     * @param padding
     */
    public SshAgentAlive(byte[] padding) {
        super(RFCAgentMessages.SSH_AGENT_ALIVE);
        this.padding = padding;
    }

    /**
     *
     *
     * @return
     */
    public byte[] getPadding() {
        return padding;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_AGENT_ALIVE";
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
            baw.write(padding);
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
            padding = new byte[bar.available()];
            bar.read(padding);
        } catch (IOException ioe) {
            throw new InvalidMessageException(ioe.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
