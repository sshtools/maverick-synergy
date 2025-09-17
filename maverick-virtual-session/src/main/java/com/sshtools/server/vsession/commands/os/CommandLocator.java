package com.sshtools.server.vsession.commands.os;

/*-
 * #%L
 * Virtual Sessions
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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A utility class to locate native command executables based on the current operating system.
 * It assumes a specific installation directory structure.
 */
public class CommandLocator {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * Gets the full path to a command executable based on the detected OS.
     * The directory structure is assumed to be:
     * - bin/linux/debian/
     * - bin/osx/
     * - bin/windows/x64/
     *
     * @param commandName The name of the command (e.g., "runas", "verifyuser").
     * @return A Path object representing the location of the executable.
     * @throws UnsupportedOperationException if the operating system is not supported.
     */
    public static Path getCommandPath(String commandName) {
        String basePath = "bin";

        if (isWindows()) {
            // On Windows, executables typically have a .exe extension.
            return Paths.get(basePath, "windows", "x64", commandName + ".exe");
        } else if (isMac()) {
            return Paths.get(basePath, "osx", commandName);
        } else if (isLinux()) {
            // Assuming all Linux variants will use the debian build for this case.
            return Paths.get(basePath, "linux", "debian", commandName);
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + OS_NAME);
        }
    }

    /**
     * Checks if the current OS is Windows.
     * @return true if the OS is Windows, false otherwise.
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /**
     * Checks if the current OS is macOS (OSX).
     * @return true if the OS is macOS, false otherwise.
     */
    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    /**
     * Checks if the current OS is Linux.
     * @return true if the OS is Linux, false otherwise.
     */
    public static boolean isLinux() {
        return OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    }


    /**
     * Main method to demonstrate the functionality of the locator.
     */
    public static void main(String[] args) {
        System.out.println("Current OS detected: " + System.getProperty("os.name"));
        System.out.println("------------------------------------------");

        try {
            Path runasPath = getCommandPath("runas");
            Path verifyUserPath = getCommandPath("verifyuser");

            System.out.println("Path for 'runas': " + runasPath);
            System.out.println("Path for 'verifyuser': " + verifyUserPath);

        } catch (UnsupportedOperationException e) {
            System.err.println(e.getMessage());
        }
    }
}
