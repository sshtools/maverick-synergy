package com.sshtools.server.vsession;

/*-
 * #%L
 * Virtual Sessions
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.util.Iterator;
import java.util.List;

public class CmdLine {

	public enum Condition { Background, ExecNextCommandOnSuccess, ExecNextCommandOnFailure, ExecNextCommand };
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
