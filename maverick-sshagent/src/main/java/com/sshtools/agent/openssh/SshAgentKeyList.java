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

package com.sshtools.agent.openssh;

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
