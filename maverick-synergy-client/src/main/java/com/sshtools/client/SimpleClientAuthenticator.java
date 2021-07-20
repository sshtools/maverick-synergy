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

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.AbstractRequestFuture;
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
