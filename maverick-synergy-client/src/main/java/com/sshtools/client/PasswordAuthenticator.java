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
