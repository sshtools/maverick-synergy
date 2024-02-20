package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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
