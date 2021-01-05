/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.server.vsession;

public abstract class ShellCommand extends AbstractCommand {

	public ShellCommand(String name, String subsystem, String signature, String description) {
		super(name, subsystem, signature, description);
	}

	public final static String SUBSYSTEM_SHELL = "Shell";
	public final static String SUBSYSTEM_SSHD = "Sshd";
	public final static String SUBSYSTEM_HELP = "Help";
	public static final String SUBSYSTEM_FILESYSTEM = "File System";
	public static final String SUBSYSTEM_POLICY = "Policy";
	public static final String SUBSYSTEM_JVM = "JVM";
	public static final String SUBSYSTEM_SYSTEM = "System";
	public static final String SUBSYSTEM_TEXT_EDITING = "Text Editing";
	public static final String SUBSYSTEM_CALLBACK = "Callback";
	
	


}
