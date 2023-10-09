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
