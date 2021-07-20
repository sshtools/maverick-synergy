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

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public abstract class AbstractCommand implements Command {
	private String name;
	private String subsystem;
	private String signature;
	private String description;
	
	private boolean builtIn = false;
	
	protected int exitCode = 0;
	
	
	public boolean isHidden() {
		return false;
	}

	public AbstractCommand(String name, String subsystem, String usage, String description) {
		this.name = name;
		this.subsystem = subsystem;
		this.signature = usage;
		this.description = description;
	}

	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}

	public String getSubsystem() {
		return subsystem;
	}

	public String getCommandName() {
		return name;
	}

	public String getUsage() {
		return signature;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	protected void setBuiltIn(boolean builtIn) {
		this.builtIn = builtIn;
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}

	protected void setSignature(String signature) {
		this.signature = signature;
	}

	protected void setDescription(String description) {
		this.description = description;
	}
	
	public int getExitCode() {
		return exitCode;
	}
	
	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		
	}
}
