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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client;

import java.io.IOException;

import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.util.ByteArrayReader;

/**
 * A simple base class for implementing non-interactive authentication methods. Use when the 
 * authentication method consists of a single message sent to the server. 
 */
public abstract class SimpleClientAuthenticator extends AbstractRequestFuture implements ClientAuthenticator {

	boolean moreAuthenticationsRequired;
	String[] authenticationMethods;
	
	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException {
		return false;
	}

	@Override
	public boolean isMoreAuthenticationRequired() {
		return moreAuthenticationsRequired;
	}
	
	@Override
	public String[] getAuthenticationMethods() {
		return authenticationMethods;
	}
	
	public void success() {
		this.moreAuthenticationsRequired = false;
		this.authenticationMethods = new String[0];
		done(true);
	}
	
	@Override
	public void success(boolean moreAuthenticationsRequired, String[] authenticationMethods) {
		this.moreAuthenticationsRequired = moreAuthenticationsRequired;
		this.authenticationMethods = authenticationMethods;
		done(true);
	}
	
	public void failure() {
		done(false);
	}
}
