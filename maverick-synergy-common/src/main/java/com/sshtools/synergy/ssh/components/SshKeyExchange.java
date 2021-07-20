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

package com.sshtools.synergy.ssh.components;

import java.io.IOException;
import java.math.BigInteger;

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshComponent;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.synergy.ssh.SshContext;
import com.sshtools.synergy.ssh.SshTransport;

/**
 * Base interface for SSH2 key exchange implementations. 
 * @author Lee David Painter
 *
 */
public interface SshKeyExchange<T extends SshContext> extends SshComponent, SecureComponent {

	String getHashAlgorithm();
	
	void test() throws IOException, SshException;
	
    public void init(SshTransport<T> transport, String clientId, String serverId, byte[] clientKexInit,
            byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey, boolean firstPacketFollows,
            boolean useFirstPacket) throws IOException, SshException;

	public void setReceivedNewKeys(boolean b);

	public boolean processMessage(byte[] msg) throws SshException, IOException;

	public void setSentNewKeys(boolean b);

	public byte[] getExchangeHash();

	public boolean hasReceivedNewKeys();

	public boolean hasSentNewKeys();

	public BigInteger getSecret();

	public String getAlgorithm();

	public byte[] getHostKey();
	
	String getProvider();

	byte[] getSignature();
}
