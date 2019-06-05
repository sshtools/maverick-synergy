package com.sshtools.server.vshell;

import java.util.ArrayList;
import java.util.List;

import com.sshtools.server.vshell.CmdLine.Condition;


public class LineParser {

	private Environment environment;

	public LineParser(Environment environment) {
		this.environment = environment;
	}

	public List<String> parse(String commandline, int lastExitCode) {
		List<String> args = new ArrayList<String>();
		StringBuilder cmd = new StringBuilder();
		StringBuilder line = new StringBuilder();
		boolean quoted = false;
		StringBuilder var = null;
		boolean bracketedVar = false;
		boolean escaped = false;
		
		for (int i = 0; i < commandline.length(); i++) {
			char ch = commandline.charAt(i);
			
			if (ch == '\"' && !escaped) {
				quoted = !quoted;
			} else if (ch == '$' && var == null && !escaped) {
				var = new StringBuilder();
			} else if (ch == '\\' && !escaped) {
				escaped = true;
				line.append(ch);
				continue;
			} else if ((ch == ' ') && !quoted) {
				args.add(cmd.toString().trim());
				cmd.setLength(0);
			} else if (var != null && ch == '{' && !bracketedVar) {
				bracketedVar = true;
			} else if (var != null && ch == '}' && bracketedVar) {
				bracketedVar = false;
				cmd.append(environment.getOrDefault(var.toString(), ""));
				var = null;
				// finish and process var
			} else if (var != null && ch == ' ' && !bracketedVar) {
				// finish and process var
				cmd.append(environment.getOrDefault(var.toString(), ""));
				var = null;
			} else if (var != null) {
				var.append(ch);
			} else {
				cmd.append(ch);
			}
			escaped = false;
			line.append(ch);
		}
		if (var != null) {
			if(var.toString().equals("?")) {
				cmd.append(String.valueOf(lastExitCode));
			} else {
				cmd.append(environment.getOrDefault(var.toString(), ""));
			}
		}
		args.add(cmd.toString().trim());
		
		// Add last command
		return args;
	}
	
	public List<CmdLine> parseCommands(String str, int lastExitCode) {
		
		ArrayList<CmdLine> commands = new ArrayList<CmdLine>();
		
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder cmdline = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
		
			if (ch == '\"' && !escaped) {
				quoted = !quoted;
			} else if(ch == '\\' && !escaped) {
				escaped = true;
				continue;
			} else if(ch=='&' && !escaped && !quoted) {
				boolean doubleAmp = false;
				if(str.length() > i+1 && str.charAt(i+1)=='&') {
					doubleAmp = true;
					i++;
				}
				
				commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode), doubleAmp ? Condition.ExecNextCommandOnSuccess : Condition.ExecNextCommand, !doubleAmp));
				cmdline.setLength(0);
				continue;
			} else if(ch ==';' && !escaped && !quoted) {
				commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode),Condition.ExecNextCommand, false));
				cmdline.setLength(0);
				continue;
			}
			
			cmdline.append(ch);
			escaped = false;
		}
		
		if(cmdline.toString().trim().length() > 0) {
			commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode), Condition.ExecNextCommand, false));
		}
		
		return commands;		
	}

}
