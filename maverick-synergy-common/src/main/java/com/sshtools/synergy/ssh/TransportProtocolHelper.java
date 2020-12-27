/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.ssh;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteArrayWriter;

public class TransportProtocolHelper {

	public static byte[] generateKexInit(SshContext sshContext) throws SshException, IOException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			
			byte[] cookie = new byte[16];
			ComponentManager.getDefaultInstance().getRND().nextBytes(cookie);

			baw.write((byte) TransportProtocol.SSH_MSG_KEX_INIT);

			baw.write(cookie);

			String list = sshContext.supportedKeyExchanges().list(
					sshContext.getPreferredKeyExchange());

			baw.writeString(list);

			list = sshContext.getSupportedPublicKeys();

			baw.writeString(list);

			list = sshContext.supportedCiphersCS().list(
					sshContext.getPreferredCipherCS());

			baw.writeString(list);

			list = sshContext.supportedCiphersSC().list(
					sshContext.getPreferredCipherSC());

			baw.writeString(list);

			list = sshContext.supportedMacsCS().list(
					sshContext.getPreferredMacCS());

			baw.writeString(list);

			list = sshContext.supportedMacsSC().list(
					sshContext.getPreferredMacSC());

			baw.writeString(list);

			list = sshContext.supportedCompressionsCS().list(
					sshContext.getPreferredCompressionCS());

			baw.writeString(list);

			list = sshContext.supportedCompressionsSC().list(
					sshContext.getPreferredCompressionSC());

			baw.writeString(list);

			baw.writeInt(0);
			baw.writeInt(0);
			baw.write((byte) 0);
			baw.writeInt(0);
			
			return baw.toByteArray();
		}
	}

}
