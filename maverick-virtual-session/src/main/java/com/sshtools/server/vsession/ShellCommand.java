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
