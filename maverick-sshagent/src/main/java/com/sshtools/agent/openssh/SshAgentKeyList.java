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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sshtools.agent.AgentMessage;
import com.sshtools.agent.exceptions.InvalidMessageException;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;


public class SshAgentKeyList extends AgentMessage {
    
    private Map<SshPublicKey,String> keys;

    /**
     * Creates a new SshAgentKeyList object.
     *
     * @param keys
     */
    public SshAgentKeyList(Map<SshPublicKey,String> keys) {
        super(OpenSSHAgentMessages.SSH2_AGENT_IDENTITIES_ANSWER);
        this.keys = keys;
    }

    /**
     * Creates a new SshAgentKeyList object.
     */
    public SshAgentKeyList() {
        super(OpenSSHAgentMessages.SSH2_AGENTC_REQUEST_IDENTITIES);
        this.keys = new HashMap<SshPublicKey,String>();
    }

    /**
     *
     *
     * @return
     */
    public Map<SshPublicKey,String> getKeys() {
        return keys;
    }

    /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return getMessageType()==OpenSSHAgentMessages.SSH2_AGENT_IDENTITIES_ANSWER ? 
        		"SSH_AGENT_IDENTITIES_ANSWER" : "SSH_AGENTC_REQUEST_IDENTITIES";
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
            baw.writeInt(keys.size());

            Map.Entry<SshPublicKey,String> entry;
            Iterator<Map.Entry<SshPublicKey,String>> it = keys.entrySet().iterator();
            SshPublicKey key;
            String description;

            while (it.hasNext()) {
                entry = it.next();
                key = (SshPublicKey) entry.getKey();
                description = (String) entry.getValue();
                try {
					baw.writeBinaryString(key.getEncoded());
				} catch (com.sshtools.common.ssh.SshException e) {
					throw new SshIOException(e);
				}
                baw.writeString(description);
            }
        } catch (IOException ex) {
            throw new InvalidMessageException("Failed to write message data",SshException.AGENT_ERROR);
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
            int num = (int) bar.readInt();
            SshPublicKey key;
            String description;
            byte[] buf;

            for (int i = 0; i < num; i++) {
                buf = bar.readBinaryString();
                key =SshPublicKeyFileFactory.decodeSSH2PublicKey(buf);
                description = bar.readString();
                keys.put(key, description);
            }
        } catch (IOException ex) {
            throw new InvalidMessageException("Failed to read message data",SshException.AGENT_ERROR);
        }
    }
}
