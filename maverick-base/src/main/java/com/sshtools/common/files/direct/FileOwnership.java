package com.sshtools.common.files.direct;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserPrincipal;

/**
 * A container for file ownership principals (user and group).
 * An instance of this class can be created once and then applied to multiple paths
 * after they have been created.
 */
class FileOwnership {
    private final UserPrincipal owner;
    private final GroupPrincipal group; // This will be null on non-POSIX systems like Windows

    /**
     * Constructs a FileOwnership object.
     * @param owner The user principal.
     * @param group The group principal (can be null).
     */
    public FileOwnership(UserPrincipal owner, GroupPrincipal group) {
        this.owner = owner;
        this.group = group;
    }

    /**
     * Applies the stored owner and group to a given file path.
     * This is intended to be called *after* the file has been created.
     * @param path The path to the file or directory.
     * @throws IOException if an I/O error occurs.
     */
    public void applyTo(Path path) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // On Windows, we only set the owner.
            FileOwnerAttributeView ownerView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
            if (ownerView != null) {
                ownerView.setOwner(owner);
                System.out.println("Successfully set owner on Windows to: " + owner.getName());
            } else {
                System.err.println("Could not get FileOwnerAttributeView for " + path);
            }
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // On POSIX systems, we set both owner and group.
            PosixFileAttributeView posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (posixView != null) {
                posixView.setOwner(owner);
                System.out.println("Successfully set owner to: " + owner.getName());
                if (group != null) {
                    posixView.setGroup(group);
                    System.out.println("Successfully set group to: " + group.getName());
                }
            } else {
                System.err.println("Could not get PosixFileAttributeView for " + path);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported OS for setting ownership: " + os);
        }
    }
}
