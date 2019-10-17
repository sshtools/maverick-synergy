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
import java.util.Objects;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.EncodingUtils;

/**
 * Implements the password authentication method.
 */
public class PasswordAuthenticator extends SimpleClientAuthenticator {

	String password;
	
	public PasswordAuthenticator() {
		
	}
	public PasswordAuthenticator(String password) {
		this.password = password;
	}
	
	public PasswordAuthenticator(char[] password) {
		this.password = new String(password);
	}
	
	public String getPassword() {
		return password;
	}
	
	private byte[] getPasswordBytes() {
		return EncodingUtils.getUTF8Bytes(getPassword());
	}
	
	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws SshException {
		
		
		byte[] tmp = getPasswordBytes();
		if(Objects.isNull(tmp)) {
			throw new SshException("Password not set!",
					SshException.BAD_API_USAGE);
		}
		
		transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "password") {

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				
				super.writeMessageIntoBuffer(buf);
				
				buf.put((byte)0);
				
				byte[] password = getPasswordBytes();
				buf.putInt(password.length);
				buf.put(password);
				
				return true;
			}			
		});
		
	}
	

	@Override
	public String getName() {
		return "password";
	}
}
