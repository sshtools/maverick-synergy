package com.sshtools.client;

import java.nio.ByteBuffer;

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
	public void authenticate(TransportProtocolClient transport, String username) {
		
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
