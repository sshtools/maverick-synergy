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
import java.nio.file.FileSystems;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;

/**
 * A utility class for looking up file ownership information.
 */
final class FileOwnershipUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private FileOwnershipUtil() {}

    /**
     * Looks up user and group principals and returns them in a FileOwnership object.
     * This object acts as a container for the ownership details, which can then be
     * applied to a file after its creation.
     *
     * @param ownerUsername The name of the user.
     * @param groupName The name of the group (ignored on Windows).
     * @return A FileOwnership object containing the looked-up principals.
     * @throws IOException if the user or group cannot be found, or an I/O error occurs.
     */
    public static FileOwnership lookupOwnership(String ownerUsername, String groupName) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        String os = System.getProperty("os.name").toLowerCase();

        UserPrincipal owner;
        try {
            owner = lookupService.lookupPrincipalByName(ownerUsername);
        } catch (UserPrincipalNotFoundException e) {
            System.err.println("User '" + ownerUsername + "' not found.");
            throw e;
        }
        
        GroupPrincipal group = null;
        if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            try {
                group = lookupService.lookupPrincipalByGroupName(groupName);
            } catch (UserPrincipalNotFoundException e) {
                System.err.println("Group '" + groupName + "' not found on POSIX system.");
                throw e;
            }
        }

        return new FileOwnership(owner, group);
    }
}
