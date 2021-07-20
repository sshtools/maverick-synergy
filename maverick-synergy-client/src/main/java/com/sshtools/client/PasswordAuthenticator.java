
package com.sshtools.client;

import java.nio.ByteBuffer;
import java.util.Objects;

import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.util.EncodingUtils;

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
			cancel();
			return;
		}
		
		transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "password") {

			@Override
			public boolean writeMessageIntoBuffer(ByteBuffer buf) {
				
				super.writeMessageIntoBuffer(buf);
				
				buf.put((byte)0);
				
				buf.putInt(tmp.length);
				buf.put(tmp);
				
				return true;
			}			
		});
		
	}
	

	@Override
	public String getName() {
		return "password";
	}
}
