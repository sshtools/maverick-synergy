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

import com.sshtools.common.auth.AbstractPublicKeyAuthenticationProvider;
import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.publickey.authorized.AuthorizedKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshPublicKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Public-key authenticator driven by the {@code AuthorizedKeysFile} directive in {@code sshd.cfg}.
 *
 * <p>The path may contain {@code %USERNAME%} which is substituted with the
 * connecting user's username at authentication time. Relative paths are
 * resolved under the user's UNIX home directory ({@code /home/<username>}).</p>
 *
 * <p>Example configuration:</p>
 * <pre>AuthorizedKeysFile /home/%USERNAME%/.ssh/authorized_keys</pre>
 *
 * <p>Public-key authentication can be disabled globally with:</p>
 * <pre>PubkeyAuthentication no</pre>
 */
public final class AdaptiveConfigPublicKeyAuthenticator extends AbstractPublicKeyAuthenticationProvider {

    private static final String DEFAULT_AUTHORIZED_KEYS = ".ssh/authorized_keys";

    private final AdaptiveConfiguration config;

    public AdaptiveConfigPublicKeyAuthenticator(AdaptiveConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
        if (!config.getBoolean(AdaptiveConfiguration.PUBKEY_AUTHENTICATION, true,
                con.getRemoteIPAddress())) {
            return false;
        }

        String template = config.getProperty(AdaptiveConfiguration.AUTHORIZED_KEYS_FILE,
                DEFAULT_AUTHORIZED_KEYS, con.getRemoteIPAddress());
        String resolved = template.replace("%USERNAME%", con.getUsername());

        Path path = Paths.get(resolved);
        if (!path.isAbsolute()) {
            // Resolve relative to the user's UNIX home directory.
            path = Paths.get("/home", con.getUsername()).resolve(resolved);
        }

        if (!Files.isReadable(path)) {
            return false;
        }

        try (InputStream in = Files.newInputStream(path)) {
            AuthorizedKeyFile file = new AuthorizedKeyFile();
            file.load(in);
            return file.isAuthorizedKey(key);
        }
    }
}
