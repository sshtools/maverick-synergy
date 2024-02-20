package com.sshtools.agent.openssh;

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
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
/**
 * Message for Agent Private key operation
 * @author Aruna Abesekara
 * 
 *
 */

public class SshAgentPrivateKeyOp extends AgentMessage {
   
    SshPublicKey pubkey;
    byte[] data;
    UnsignedInteger32 flags;

    /**
     * Creates a new SshAgentPrivateKeyOp object.
     */
    public SshAgentPrivateKeyOp() {
        super(OpenSSHAgentMessages.SSH2_AGENTC_SIGN_REQUEST);
    }

    /**
     * Creates a new SshAgentPrivateKeyOp object.
     *
     * @param pubkey
     * @param operation
     * @param data
     */
    public SshAgentPrivateKeyOp(SshPublicKey pubkey, 
        byte[] data, UnsignedInteger32 flags) {
        super(OpenSSHAgentMessages.SSH2_AGENTC_SIGN_REQUEST);
        this.pubkey = pubkey;
        this.data = data;
        this.flags = flags;
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
    public UnsignedInteger32 getFlags() {
        return flags;
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
        return "SSH2_AGENTC_SIGN_REQUEST";
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
            baw.writeBinaryString(pubkey.getEncoded());
            baw.writeBinaryString(data);
            baw.writeUINT32(flags);
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
            pubkey = SshPublicKeyFileFactory.decodeSSH2PublicKey(bar.readBinaryString());
            data = bar.readBinaryString();
            if(bar.available() >= 4) {
            	flags = bar.readUINT32();
            } else {
            	flags = new UnsignedInteger32(1);
            }
        } catch (IOException ex) {
            throw new InvalidMessageException(ex.getMessage(),SshException.AGENT_ERROR);
        }
    }
}
