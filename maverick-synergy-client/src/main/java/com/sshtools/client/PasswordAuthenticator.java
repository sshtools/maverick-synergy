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
import java.util.Objects;
import java.util.function.Supplier;

import com.sshtools.common.ssh.SshException;
import com.sshtools.synergy.util.EncodingUtils;

/**
 * Implements the password authentication method.
 */
public class PasswordAuthenticator extends SimpleClientAuthenticator {
	
	@FunctionalInterface
	public interface PasswordPrompt extends Supplier<String>, NotifiedPrompt {
		@Override
		default void completed(boolean success, String value, ClientAuthenticator authenticator) {
		}
	}

	public static PasswordAuthenticator of(PasswordPrompt password) {
		var pa = new PasswordAuthenticator();
		pa.password = password;
		return pa;
	}

	private PasswordPrompt password;
	private String lastPassword;
	
	public PasswordAuthenticator() {
	}

	@Deprecated(since="3.1.0")
	public PasswordAuthenticator(Supplier<String> supplier) {
		this.password = new PasswordPrompt() {
			@Override
			public String get() {
				return supplier.get();
			}
		};
	}
	
	public PasswordAuthenticator(String password) {
		this.password = () -> password;
	}
	
	public PasswordAuthenticator(char[] password) {
		this.password = () -> new String(password);
	}
	
	public String getPassword() {
		return password.get();
	}
	
	@Override
	public synchronized void done(boolean success) {
		try {
			super.done(success);
		}
		finally {
			password.completed(success, lastPassword, this);
		}
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws SshException {
		
		lastPassword = getPassword();
		byte[] tmp = EncodingUtils.getUTF8Bytes(lastPassword);
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
