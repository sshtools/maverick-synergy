package com.sshtools.server.vshell;

import java.util.Iterator;
import java.util.List;

public class CmdLine {

	enum Condition { Background, ExecNextCommandOnSuccess, ExecNextCommandOnFailure, ExecNextCommand };
	//enum Redirection { NoRedirection, PipeToNextCommand, PipeToFile };
	
	String line;
	List<String> args;
	Condition condition;
	//Redirection pipe;
	int exitCode = 0;
	boolean background;
	
	public CmdLine(String line, List<String> args, Condition condition, boolean background) {
		this.args = args;
		this.condition = condition;
		this.line = line;
		//this.pipe = pipe;
		this.background = background;
		for (Iterator<String> it = args.iterator(); it.hasNext();) {
			if (it.next().equals("")) {
				it.remove();
			}
		}
	}
	public List<String> getArgs() {
		return args;
	}
	public String getLine() {
		return line;
	}
	public String getCommand() {
		return args.get(0);
	}
	public String[] getArgArray() {
		return args.toArray(new String[0]);
	}
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	public int getExitCode() {
		return exitCode;
	}
	public Condition getCondition() {
		return condition;
	}
//	public Redirection getPipe() {
//		return pipe;
//	}
	public boolean isBackground() {
		return background;
	}

}
