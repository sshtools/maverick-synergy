/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
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
