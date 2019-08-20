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
package com.sshtools.client;

import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.EncodingUtils;

class AuthenticationMessage implements SshMessage {

	

	byte[] username;
	byte[] servicename;
	byte[] methodname;

	AuthenticationMessage(String username, String servicename, String methodname) {
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
