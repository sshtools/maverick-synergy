package com.sshtools.sshd;

/*-
 * #%L
 * Maverick Synergy SSHD
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import com.sshtools.common.auth.PasswordAuthenticationProvider;
import com.sshtools.common.auth.PasswordChangeException;
import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.BCrypt;

import java.io.IOException;

/**
 * Password authenticator driven by the {@code User} directives in {@code sshd.cfg}.
 *
 * <p>Each {@code User} entry has the form:</p>
 * <pre>User username $2a$12$...</pre>
 * <p>The second token is the BCrypt hash produced by {@link SynergyPasswd}.</p>
 *
 * <p>Authentication can be disabled globally with:</p>
 * <pre>PasswordAuthentication no</pre>
 */
public final class AdaptiveConfigPasswordAuthenticator extends PasswordAuthenticationProvider {

    private final AdaptiveConfiguration config;

    public AdaptiveConfigPasswordAuthenticator(AdaptiveConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean verifyPassword(SshConnection con, String username, String password)
            throws PasswordChangeException, IOException {

        if (!config.getBoolean(AdaptiveConfiguration.PASSWORD_AUTHENTICATION, true,
                con.getRemoteIPAddress())) {
            return false;
        }

        for (String entry : config.getMultipleConfig(AdaptiveConfiguration.USER)) {
            int space = entry.indexOf(' ');
            if (space < 0) {
                continue;
            }
            String entryUser = entry.substring(0, space).trim();
            String entryHash = entry.substring(space + 1).trim();
            if (entryUser.equals(username)) {
                return BCrypt.checkpw(password, entryHash);
            }
        }
        return false;
    }

    @Override
    public boolean changePassword(SshConnection con, String username, String oldPassword, String newPassword)
            throws PasswordChangeException, IOException {
        // Password changes are not supported via the config file.
        return false;
    }
}
