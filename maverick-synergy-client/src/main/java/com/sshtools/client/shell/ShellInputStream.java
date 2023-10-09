/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

package com.sshtools.client.shell;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.sshtools.common.logger.Log;

class ShellInputStream extends InputStream {
	
	private String beginCommandMarker;
	private byte[] endCommandMarker;
	private byte[] promptMarker;
	private int markerPos;
	private StringBuffer currentLine;
	private String cmd;
	private StringBuffer commandOutput = new StringBuffer();
	private boolean expectingEcho = true;
	private int exitCode = ExpectShell.EXIT_CODE_PROCESS_ACTIVE;
	private ExpectShell shell;
	private BufferedInputStream sessionIn;
	private boolean active = true;
	private boolean matchPromptMarker;

	ShellInputStream(ExpectShell shell, String beginCommandMarker, String endCommandMarker, String cmd, boolean matchPromptMarker, String promptMarker) {
		this.beginCommandMarker = beginCommandMarker;
		this.endCommandMarker = endCommandMarker.getBytes();
		this.matchPromptMarker = matchPromptMarker;
		this.promptMarker = promptMarker.getBytes();
		this.shell = shell;
		this.cmd = cmd;
		this.sessionIn = shell.sessionIn;
	}
	
	public String getCommand() {
		return cmd;
	}
	
	public int getExitCode() throws IllegalStateException {
		return exitCode;
	}
	
	public boolean isComplete() {
		return exitCode==ExpectShell.EXIT_CODE_PROCESS_ACTIVE;
	}
	
	public boolean hasSucceeded() {
		return exitCode == 0;
	}
	
	public String getCommandOutput() {
		return commandOutput.toString().trim();
	}
	
	private String readLine() throws IOException {
		
		sessionIn.mark(-1);
		
		StringBuffer line = new StringBuffer();
		int ch;
		
		do {
			if(!isActive()) {
				throw new EOFException();
			}
			
			ch = sessionIn.read();

			if(ch > -1) {
				line.append((char)ch);
				
				if(Boolean.getBoolean("maverick.verbose")) {
					Log.debug(line.toString());
				}
			}
		} while(ch != '\n' && ch != '\r' && ch != -1);
		
		sessionIn.mark(1);
		if(ch == '\r' && sessionIn.read()!='\n') {
			sessionIn.reset();
		}
		
		if((!isActive() || ch==-1) && line.toString().trim().length()==0)
			return null;
		else {
			if(Log.isDebugEnabled()) {
				Log.debug(line.toString());
			}
			return line.toString().trim();
		}
	}
	
	public int read(byte[] buf, int off, int len) throws IOException {
		
		int ch = read();
		if(ch > -1) {
			buf[off] = (byte) ch;
			return 1;
		}
		return -1;
	}
	
	public int read() throws IOException {
		
		int ch;
		
		if(expectingEcho) {
		
			String tmp;
			
			if(Log.isDebugEnabled())
				Log.debug("Expecting begin marker");
			do {
				tmp = readLine();
			} while(tmp!=null && !tmp.endsWith(beginCommandMarker));
			
			if(tmp==null) {
				if(Log.isDebugEnabled())
					Log.debug("Failed to read from shell whilst waiting for begin marker");
				shell.internalClose();
				return -1;
			}
			
			currentLine = new StringBuffer();
			expectingEcho = false;
			
			if(Log.isDebugEnabled())
				Log.debug("Found begin marker");
		} 

		int readLength = Math.max(endCommandMarker.length, promptMarker.length);
		sessionIn.mark(readLength);

		boolean endMarkerMatched = false;
		boolean promptMarkerMatched = false;
		boolean collectExitCode = true;
		byte[] selectedMarker = null;
		
		StringBuffer tmp = new StringBuffer();
		do {
			ch = sessionIn.read();
			
			if(!endMarkerMatched && !promptMarkerMatched) {
				if(markerPos < endCommandMarker.length && endCommandMarker[markerPos]==ch) {
					endMarkerMatched = true;
					readLength = endCommandMarker.length;
					selectedMarker = endCommandMarker;
				} else if(matchPromptMarker && promptMarker[markerPos]==ch) {
					promptMarkerMatched = true;
					readLength = promptMarker.length;
					selectedMarker = promptMarker;
					collectExitCode = false;
				} else
					break;
			} else if(endMarkerMatched) {
				if(endCommandMarker[markerPos]!=ch) {
					endMarkerMatched = false;
					break;
				}
			} else if(promptMarkerMatched) {
				if(promptMarker[markerPos]!=ch) {
					promptMarkerMatched = false;
					break;
				}
			}
			tmp.append((char)ch);
			
		} while(markerPos++ < readLength - 1 && (endMarkerMatched || promptMarkerMatched));

							
		if(selectedMarker!=null && markerPos == selectedMarker.length) {
			// We matched the marker!!!

			if(Log.isDebugEnabled())
				Log.debug(tmp.toString());
			cleanup(collectExitCode, collectExitCode ? "end" : "prompt");
			
			if(Boolean.getBoolean("maverick.discardShellInputBeforeEOF")) {
				byte[] tmp2 = new byte[255];
				sessionIn.read(tmp2);
				Log.debug("Discarded " + new String(tmp2, "UTF-8"));
			}
			return -1;
		} 
		
		sessionIn.reset();
		ch = sessionIn.read();
		
		if(ch==-1) {
			if(Log.isDebugEnabled()) {
				Log.debug("Stream ended before we could read an exit code");
			}
			// Cannot collect exit code since the stream is EOF
			cleanup(false, "EOF");
			return -1;
		}
		
		markerPos = 0;			
		currentLine.append((char)ch);
		commandOutput.append((char)ch);
		
		if(ch == '\n') {
			// End of a line
			if(Log.isDebugEnabled()) {
				Log.debug(currentLine.toString());
			}
			currentLine = new StringBuffer();
		} 
		
		sessionIn.mark(-1);
		return ch;
	}
	
	void cleanup(boolean collectExitCode, String markerType) throws IOException {
		
		if(Log.isDebugEnabled())
			Log.debug("Found " + markerType + " marker");
		
		if(collectExitCode)
			exitCode = collectExitCode();
		else
			exitCode = ExpectShell.EXIT_CODE_UNKNOWN;
		
		shell.state = ExpectShell.WAITING_FOR_COMMAND;
		active = false;
	}
	
	boolean isActive() {
		return active;
	}
	
	int collectExitCode() throws IOException {
		if(Log.isDebugEnabled())
			Log.debug("Looking for exit code");
		
		// Next bytes should be the exit code of the process, followed by a \n;
		StringBuffer tmp = new StringBuffer();
		char ch;
		int exitCode = -1;
		do {
			ch = (char)sessionIn.read();
			tmp.append(ch);
		} while(ch!='\n');
		
		
		try {
			String code = tmp.toString().trim();
			/**
			 * Powershell returns True or False for $?
			 */
			if("True".equals(code)) {
				exitCode = 0;
			} else if("False".equals(code)) {
				exitCode = 1;
			} else {
				exitCode = Integer.parseInt(tmp.toString().trim());
			}
			if(Log.isDebugEnabled())
				Log.debug("Exit code is " + exitCode);
		} catch (NumberFormatException e) {
			if(Log.isDebugEnabled())
				Log.debug("Failed to get exit code: " + tmp.toString().trim());
			exitCode = ExpectShell.EXIT_CODE_UNKNOWN;
		}
		return exitCode;
		
	}

	public void clearOutput() {
		commandOutput.setLength(0);
	}
}