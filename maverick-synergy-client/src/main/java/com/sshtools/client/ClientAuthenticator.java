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

import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.ByteArrayReader;

/**
 * Base interface for all client authentication methods.
 */
public interface ClientAuthenticator extends RequestFuture {

	/**
	 * The authentication mechanism name/.
	 * @return
	 */
	String getName();
	
	/**
	 * Start the authentication
	 * @param transport
	 * @param username
	 * @throws IOException 
	 * @throws SshException 
	 */
	void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException;

	/**
	 * Process an authentication message.
	 * @param msg
	 * @return
	 * @throws IOException
	 * @throws SshException 
	 */
	boolean processMessage(ByteArrayReader msg) throws IOException, SshException;

	/**
	 * Called by the API to indicate authentication success.
	 */
	void success();
	
	/**
	 * Called by the API to indicate authentication failure.
	 */
	void failure();

	boolean isMoreAuthenticationRequired();
	
	boolean isCancelled();
	
	void cancel();

	String[] getAuthenticationMethods();

	void success(boolean moreAuthenticationsRequired, String[] authenticationMethods);
}
