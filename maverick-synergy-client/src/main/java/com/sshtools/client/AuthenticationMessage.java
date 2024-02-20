package com.sshtools.client;

/*-
 * #%L
 * Client API
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

import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.synergy.util.EncodingUtils;

public class AuthenticationMessage implements SshMessage {

	byte[] username;
	byte[] servicename;
	byte[] methodname;

	public AuthenticationMessage(String username, String servicename, String methodname) {
		this.username = EncodingUtils.getUTF8Bytes(username);
		this.servicename = EncodingUtils.getUTF8Bytes(servicename);
		this.methodname = EncodingUtils.getUTF8Bytes(methodname);
	}

	@Override
	public boolean writeMessageIntoBuffer(ByteBuffer buf) {

		buf.put((byte) AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
		buf.putInt(username.length);
		buf.put(username);
		buf.putInt(servicename.length);
		buf.put(servicename);
		buf.putInt(methodname.length);
		buf.put(methodname);

		return true;
	}
	
	@Override
	public void messageSent(Long sequenceNo) {

		if(Log.isDebugEnabled()) {
			Log.info("SSH_MSG_USERAUTH_REQUEST sent method="
					+ EncodingUtils.getUTF8String(methodname) + " username="
					+ EncodingUtils.getUTF8String(username));
		}
	}
}
