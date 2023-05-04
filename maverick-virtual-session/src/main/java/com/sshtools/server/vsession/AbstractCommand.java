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
