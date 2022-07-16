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
