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

				/******************************************************************************
 * Copyright (c) 2003-2009 SSHTools Ltd. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code and
 * its use is subject to the terms of the SSHTools Software License. You may not use
 * this file except in compliance with the license terms.
 *
 * You should have received a copy of the SSHTools Software License along with this
 * software; see the file LICENSE.html.  If not, write to or contact:
 *
 *	SSHTools Ltd
 *	No. 13 Carlton Business Center
 *	Station Road
 *	Cartlon
 *	United Kingdom
 *	NG4 3AA
 *
 *	Tel:
 *	+44 (0)845 643 4496
 *	Fax:
 *	+44 (0)845 643 4498
 *
 *	Sales:
 *	sales@sshtools.com
 *	Support:
 *	support@sshtools.com 
 *
 * WWW:       http://www.sshtools.com
 *****************************************************************************/

			
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