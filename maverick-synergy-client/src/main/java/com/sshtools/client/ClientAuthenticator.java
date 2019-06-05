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

import com.sshtools.common.util.ByteArrayReader;

/**
 * Base interface for all client authentication methods.
 */
public interface ClientAuthenticator {

	/**
	 * The authentication mechanism name/.
	 * @return
	 */
	String getName();
	
	/**
	 * Start the authentication
	 * @param transport
	 * @param username
	 */
	void authenticate(TransportProtocolClient transport, String username);

	/**
	 * Process an authentication message.
	 * @param msg
	 * @return
	 * @throws IOException
	 */
	boolean processMessage(ByteArrayReader msg) throws IOException;

	/**
	 * Called by the API to indicate authentication success.
	 */
	void success();
	
	/**
	 * Called by the API to indicate authentication failure.
	 */
	void failure();
}
