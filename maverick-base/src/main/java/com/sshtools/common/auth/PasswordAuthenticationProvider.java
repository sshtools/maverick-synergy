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