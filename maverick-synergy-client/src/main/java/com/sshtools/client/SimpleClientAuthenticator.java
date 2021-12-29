/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;

/**
 * A simple base class for implementing non-interactive authentication methods. Use when the 
 * authentication method consists of a single message sent to the server. 
 */
public abstract class SimpleClientAuthenticator extends AbstractRequestFuture implements ClientAuthenticator {

	boolean moreAuthenticationsRequired;
	String[] authenticationMethods;
	boolean cancelled;
	
	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException, SshException {
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
		
		if(Log.isDebugEnabled()) {
			Log.debug("{} authentication succeeded", getName());
		}
		
		this.moreAuthenticationsRequired = false;
		this.authenticationMethods = new String[0];
		done(true);
	}
	
	@Override
	public void success(boolean moreAuthenticationsRequired, String[] authenticationMethods) {

		if(Log.isDebugEnabled()) {
			Log.debug("{} authentication succeeded partial={}", getName(), String.valueOf(moreAuthenticationsRequired));
		}
		
		this.moreAuthenticationsRequired = moreAuthenticationsRequired;
		this.authenticationMethods = authenticationMethods;
		done(true);
	}
	
	public void failure() {
		
		if(Log.isDebugEnabled()) {
			Log.debug("{} authentication failed", getName());
		}
		done(false);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void cancel() {
		cancelled = true;
		if(Log.isDebugEnabled()) {
			Log.debug("{} authentication cancelled", getName());
		}
		done(false);
	}
}
