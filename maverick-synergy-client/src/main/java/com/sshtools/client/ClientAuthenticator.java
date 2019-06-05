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
