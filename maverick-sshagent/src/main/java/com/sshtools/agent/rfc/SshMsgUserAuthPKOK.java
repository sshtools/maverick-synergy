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


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.22 $
 */
public class SshMsgUserAuthPKOK extends AgentMessage {
  
    private String algorithm;
    private byte[] key;

    //private boolean ok;

    /**
     * Creates a new SshMsgUserAuthPKOK object.
     */
    public SshMsgUserAuthPKOK() {
        super(RFCAgentMessages.SSH_MSG_USERAUTH_PK_OK);
    }

    /**
     * Creates a new SshMsgUserAuthPKOK object.
     *
     * @param ok
     * @param algorithm
     * @param key
     */
    public SshMsgUserAuthPKOK( /*boolean ok,*/
        String algorithm, byte[] key) {
        super(RFCAgentMessages.SSH_MSG_USERAUTH_PK_OK);
        this.key = key;
    }

     /**
     *
     *
     * @return
     */
    public String getMessageName() {
        return "SSH_MSG_USERAUTH_PK_OK";
    }

    /**
     *
     *
     * @param baw
     *
     * @throws InvalidMessageException
     */
    public void constructByteArray(ByteArrayWriter baw)
        throws InvalidMessageException {
        try {
            //baw.write(ok ? 1 : 0);
            baw.writeString(algorithm);
            baw.writeBinaryString(key);
        } catch (IOException ioe) {
            throw new InvalidMessageException("Failed to write message data!",SshException.AGENT_ERROR);
        }
    }

    /**
     *
     *
     * @param bar
     *
     * @throws InvalidMessageException
     */
    public void constructMessage(ByteArrayReader bar)
        throws InvalidMessageException {
        try {
            algorithm = bar.readString();
            key = bar.readBinaryString();
        } catch (IOException ioe) {
            throw new InvalidMessageException("Failed to read message data!",SshException.AGENT_ERROR);
        }
    }
}
