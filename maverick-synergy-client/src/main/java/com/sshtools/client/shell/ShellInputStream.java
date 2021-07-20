

package com.sshtools.client.shell;

import java.io.BufferedInputStream;
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

	private static boolean verboseDebug = Boolean.getBoolean("maverick.shell.verbose");
	
	ShellInputStream(ExpectShell shell, String beginCommandMarker, String endCommandMarker, String cmd, boolean matchPromptMarker, String promptMarker) {
		this.beginCommandMarker = beginCommandMarker;
		this.endCommandMarker = endCommandMarker.getBytes();
		this.matchPromptMarker = matchPromptMarker;
		this.promptMarker = promptMarker.getBytes();
		this.shell = shell;
		this.cmd = cmd;
		this.sessionIn = shell.sessionIn;
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
			ch = sessionIn.read();

			if(ch > -1)
				line.append((char)ch);
		} while(ch != '\n' && ch != '\r' && ch != -1);
		
		sessionIn.mark(1);
		if(ch == '\r' && sessionIn.read()!='\n') {
			sessionIn.reset();
		}
		
		if(ch==-1 && line.toString().trim().length()==0)
			return null;
		else
			return line.toString().trim();
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
				Log.debug(cmd + ": Expecting begin marker");
			do {
				tmp = readLine();
			} while(tmp!=null && !tmp.endsWith(beginCommandMarker));
			
			if(tmp==null) {
				if(Log.isDebugEnabled())
					Log.debug(cmd + ": Failed to read from shell whilst waiting for begin marker");
				shell.internalClose();
				return -1;
			}
			
			currentLine = new StringBuffer();
			expectingEcho = false;
			
			if(Log.isDebugEnabled())
				Log.debug(cmd + ": Found begin marker");
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
				Log.debug(cmd + ": " + tmp.toString());
			cleanup(collectExitCode, collectExitCode ? "end" : "prompt");
			return -1;
		} 
		
		sessionIn.reset();
		ch = sessionIn.read();
		
		if(ch==-1) {
			// Cannot collect exit code since the stream is EOF
			cleanup(false, "EOF");
			return -1;
		}
		
		markerPos = 0;			
		currentLine.append((char)ch);
		commandOutput.append((char)ch);
		
		if(ch == '\n') {
			// End of a line
			currentLine = new StringBuffer();
		} 
		
		if(verboseDebug && Log.isDebugEnabled())
			Log.debug(cmd + ": Current Line [" + currentLine.toString() + "]");
		
		sessionIn.mark(-1);
		return ch;
	}
	
	void cleanup(boolean collectExitCode, String markerType) throws IOException {
		
		if(Log.isDebugEnabled())
			Log.debug(cmd + ": Found " + markerType + " marker");
		
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
			Log.debug(cmd + ": Looking for exit code");
		
		// Next bytes should be the exit code of the process, followed by a \n;
		StringBuffer tmp = new StringBuffer();
		char ch;
		int exitCode = -1;
		do {
			ch = (char)sessionIn.read();
			tmp.append(ch);
		} while(ch!='\n');
		
		
		try {
			exitCode = Integer.parseInt(tmp.toString().trim());
			if(Log.isDebugEnabled())
				Log.debug(cmd + ": Exit code is " + exitCode);
		} catch (NumberFormatException e) {
			if(Log.isDebugEnabled())
				Log.debug(cmd + ": Failed to get exit code: " + tmp.toString().trim());
			exitCode = ExpectShell.EXIT_CODE_UNKNOWN;
		}
		return exitCode;
		
	}
}