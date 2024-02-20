package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
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

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.util.ByteArrayWriter;

public class TransportProtocolHelper {

	public static byte[] generateKexInit(SshContext sshContext, boolean supportsExtInfo, String extInfo, String strictTransport) throws SshException, IOException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			
			byte[] cookie = new byte[16];
			ComponentManager.getDefaultInstance().getRND().nextBytes(cookie);

			baw.write((byte) TransportProtocol.SSH_MSG_KEX_INIT);

			baw.write(cookie);

			String list = sshContext.supportedKeyExchanges().list(
					sshContext.getPreferredKeyExchange());

			if(supportsExtInfo) {
				list += "," + extInfo;
			}
			baw.writeString(list + "," + strictTransport);

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
