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

import com.sshtools.common.config.AdaptiveConfiguration;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.files.vfs.VirtualFileFactory;
import com.sshtools.common.files.vfs.VirtualMountTemplate;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.ssh.SshConnection;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link FileFactory} implementation driven by the {@code VirtualMount} directives in {@code sshd.cfg}.
 *
 * <p>Each {@code VirtualMount} entry has the form:</p>
 * <pre>VirtualMount /virtual/path /real/path/on/host</pre>
 *
 * <p>The real path may contain {@code %USERNAME%} which is substituted with the
 * connecting user's username at connection time.</p>
 *
 * <p>Example:</p>
 * <pre>
 * VirtualMount / /home/%USERNAME%
 * VirtualMount /data /srv/shared
 * </pre>
 *
 * <p>The first entry becomes the default (root) mount. Subsequent entries are additional mounts.
 * If no {@code VirtualMount} directives are present, the user's UNIX home directory
 * ({@code /home/<username>}) is mounted at {@code /}.</p>
 */
public final class AdaptiveConfigFileFactory implements FileFactory {

    private final AdaptiveConfiguration config;

    public AdaptiveConfigFileFactory(AdaptiveConfiguration config) {
        this.config = config;
    }

    @Override
    public AbstractFileFactory<?> getFileFactory(SshConnection con) throws IOException, PermissionDeniedException {
        String username = con.getUsername();
        String[] mountEntries = config.getMultipleConfig(AdaptiveConfiguration.VIRTUAL_MOUNT);

        if (mountEntries.length == 0) {
            // Default: mount the user's UNIX home directory at /
            return buildFactory(username, "/", "/home/" + username);
        }

        VirtualMountTemplate defaultMount = null;
        List<VirtualMountTemplate> additionalMounts = new ArrayList<>();

        for (String entry : mountEntries) {
            String expanded = entry.trim().replace("%USERNAME%", username);
            int space = expanded.indexOf(' ');
            if (space < 0) {
                continue;
            }
            String virtualPath = expanded.substring(0, space).trim();
            String realPath = expanded.substring(space + 1).trim();

            NioFileFactory nff = NioFileFactoryBuilder.create()
                    .withHome(Paths.get(realPath))
                    .withoutSandbox()
                    .build();

            VirtualMountTemplate template = new VirtualMountTemplate(virtualPath, realPath, nff, false);

            if (defaultMount == null) {
                defaultMount = template;
            } else {
                additionalMounts.add(template);
            }
        }

        if (defaultMount == null) {
            return buildFactory(username, "/", "/home/" + username);
        }

        try {
            return new VirtualFileFactory(defaultMount,
                    additionalMounts.toArray(new VirtualMountTemplate[0]));
        } catch (PermissionDeniedException e) {
            throw new IOException("Failed to create virtual filesystem for user " + username, e);
        }
    }

    private AbstractFileFactory<?> buildFactory(String username, String virtualPath, String realPath)
            throws IOException {
        NioFileFactory nff = NioFileFactoryBuilder.create()
                .withHome(Paths.get(realPath))
                .withoutSandbox()
                .build();
        try {
            return new VirtualFileFactory(new VirtualMountTemplate(virtualPath, realPath, nff, false));
        } catch (PermissionDeniedException e) {
            throw new IOException("Failed to create virtual filesystem for user " + username, e);
        }
    }
}
