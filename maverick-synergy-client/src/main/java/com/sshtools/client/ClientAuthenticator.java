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
