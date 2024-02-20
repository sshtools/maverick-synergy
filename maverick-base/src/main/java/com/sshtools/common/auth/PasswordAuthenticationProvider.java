package com.sshtools.common.auth;

import java.io.IOException;

import com.sshtools.common.ssh.SshConnection;

/**
 * <p>Implement this interface to customize the authentication of users logging
 * into your server. To install your provider you must configure the servers
 * <a href="../ConfigurationContext.html">ConfigurationContext</a> within the
 * servers configure method.</p>
 *
 * <p>This interface has been updated to include session id in all method
 * calls to make it more consistent.</p>
 *
 * @author Lee David Painter
 */
public abstract class PasswordAuthenticationProvider implements Authenticator {

	
	public String getName() {
		return "password";
	}
	
    /**
     * Implement this method to log the user into the system.
     * @param sessionid
     * @param username
     * @param password
     * @param ipAddress
     * @return boolean
     * @throws PasswordChangeException throw this exception if the users password requires a changing.
     */
    public abstract boolean verifyPassword(SshConnection con, String username, String password)
                    throws PasswordChangeException, IOException;

    /**
     * Implement this method to change the users password
     * @param sessionid
     * @param username
     * @param oldpassword
     * @param newpassword
     * @return boolean
     */
    public abstract boolean changePassword(SshConnection con, String username, String oldpassword, String newpassword) 
    		throws PasswordChangeException, IOException;

    
}
