

package com.sshtools.client.shell;

import java.io.IOException;
import java.io.InputStream;

import com.sshtools.common.logger.Log;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;

public class ShellController implements ShellReader, ShellWriter {

	protected ExpectShell shell;
	protected ShellMatcher matcher = null;
	protected int readlimit = 32768;
	protected InputStream in;
	
	
	ShellController(ExpectShell shell, ShellMatcher matcher, InputStream in) {
		this.shell = shell;
		this.matcher = matcher;
		this.in = in;
	}
	
	public void setMatcher(ShellMatcher matcher) {
		this.matcher = matcher;
	}
	/* (non-Javadoc)
	 * @see com.maverick.ssh.ShellWriter#interrupt()
	 */
	public void interrupt() throws IOException {
    	shell.type(new String(new char[] { 3 }));
    }
    
    /* (non-Javadoc)
	 * @see com.maverick.ssh.ShellWriter#type(java.lang.String)
	 */
	public synchronized void type(String string) throws IOException {
    	shell.type(string);
    }

    /* (non-Javadoc)
	 * @see com.maverick.ssh.ShellWriter#carriageReturn()
	 */
	public synchronized void carriageReturn() throws IOException {
    	shell.carriageReturn();
    }

    /* (non-Javadoc)
	 * @see com.maverick.ssh.ShellWriter#typeAndReturn(java.lang.String)
	 */
	public synchronized void typeAndReturn(String string) throws IOException {
    	shell.typeAndReturn(string);
    }

    /**
     * Consume the output of the command until the pattern matches. This version of expect will return with 
     * the output at the end of the matched pattern.
     *   
     * @param pattern
     * @return
     * @throws ShellTimeoutException
     * @throws SshException 
     */
	public synchronized boolean expect(String pattern) throws ShellTimeoutException, SshException {
		return expect(pattern, false, 0, 0);
	}

    /**
     * Consume the output of the command until the pattern matches. Use the consumeRemainingLine variable to
     * indicate if output should start at the end of the matched pattern (consumeRemainingLine=false) or at
     * the begninng of the next line (consumeRemainingLine=true)
     *   
     * @param pattern
     * @param consumeRemainingLine
     * @return
     * @throws ShellTimeoutException
     * @throws SshException 
     */
	public synchronized boolean expect(String pattern, boolean consumeRemainingLine) throws ShellTimeoutException, SshException {
		return expect(pattern, consumeRemainingLine, 0, 0);
	}

    /**
     * Consume the output of the command until the pattern matches. Use the consumeRemainingLine variable to
     * indicate if output should start at the end of the matched pattern (consumeRemainingLine=false) or at
     * the begninng of the next line (consumeRemainingLine=true)
     *   
     * @param pattern
     * @param timeout
     * @return
     * @throws ShellTimeoutException
     * @throws SshException 
     */
	public synchronized boolean expect(String pattern, long timeout) throws ShellTimeoutException, SshException {
		return expect(pattern, false, timeout, 0);
	}
	/**
     * Consume the output of the command until the pattern matches. This version of expect will not consume the
     * whole line and will return with the output at the end of the matched pattern.
     *   
     * @param pattern
     * @param consumeRemainingLine
     * @param timeout
     * @param maxLines
     * @return
     * @throws ShellTimeoutException
	 * @throws SshException 
     */
	public synchronized boolean expect(String pattern, boolean consumeRemainingLine, long timeout) throws ShellTimeoutException, SshException {
		return expect(pattern, consumeRemainingLine, timeout, 0);
	}
	
	/**
	 * Perform expect on the next line of output only
	 * @param pattern
	 * @return
	 * @throws ShellTimeoutException
	 * @throws SshException 
	 */
	public synchronized boolean expectNextLine(String pattern) throws ShellTimeoutException, SshException {
		return expect(pattern, false, 0, 1);
	}
	
	/**
	 * Perform expect on the next line of output only
	 * 
	 * @param pattern
	 * @param consumeRemainingLine
	 * @return
	 * @throws ShellTimeoutException
	 * @throws SshException 
	 */
	public synchronized boolean expectNextLine(String pattern, boolean consumeRemainingLine) throws ShellTimeoutException, SshException {
		return expect(pattern, consumeRemainingLine, 0, 1);
	}
	
	/**
	 * Perform expect on the next line of output only
	 * 
	 * @param pattern
	 * @param consumeRemainingLine
	 * @param timeout
	 * @return
	 * @throws ShellTimeoutException
	 * @throws SshException 
	 */
	public synchronized boolean expectNextLine(String pattern, boolean consumeRemainingLine, long timeout) throws ShellTimeoutException, SshException {
		return expect(pattern, consumeRemainingLine, timeout, 1);
	}
	/**
     * Consume the output of the command until the pattern matches. This version of expect will not consume the
     * whole line and will return with the output at the end of the matched pattern.
     *   
     * @param pattern
     * @param consumeRemainingLine
     * @param timeout
     * @param maxLines
     * @return
     * @throws ShellTimeoutException
     */
	public synchronized boolean expect(String pattern, boolean consumeRemainingLine, long timeout, long maxLines) throws ShellTimeoutException, SshException {

		StringBuffer line = new StringBuffer();
		long time = System.currentTimeMillis();
		long lines = 0;
		
		while (System.currentTimeMillis() - time < timeout || timeout == 0) {
			
			if(maxLines > 0 && lines >= maxLines)
				return false;
			
			try {
				int ch = in.read();
				if(ch == -1) {
					return false;
				}
				if(ch != '\n' && ch!='\r') {
					line.append((char)ch);
				} 
				
				switch(matcher.matches(line.toString(), pattern)) {
				case CONTENT_DOES_NOT_MATCH:
					return false;
				case CONTENT_MATCHES:
					if(Log.isDebugEnabled())
						Log.debug("Matched: [" + pattern + "] " + line.toString());
					if(consumeRemainingLine && ch!='\n' && ch != -1) {
						do {
							
						} while(ch!='\n' && ch != -1);
					}
					if(Log.isDebugEnabled()) {
						Log.debug("Shell output: " + line.toString());
					}
					return true;
				default:
					break;
				}
				
				if(ch == '\n') {
					lines++;
					if(Log.isDebugEnabled())
						Log.debug("Shell output: " + line.toString());
					line.delete(0, line.length());
				}

			} catch(SshIOException e) {
				if(e.getRealException().getReason()==SshException.MESSAGE_TIMEOUT) {
					// Ignore this exception as shell will throw its own ShellTimeoutException
				} else {
					throw e.getRealException();
				}
		    } catch (IOException e) {
				throw new SshException(e);
			} 
		}

		throw new ShellTimeoutException();
	}
	
	public boolean isActive() {
		return shell.inStartup();
	}
	
	/* (non-Javadoc)
	 * @see com.maverick.ssh.ShellReader#readLine()
	 */
	public synchronized String readLine() throws SshException, ShellTimeoutException {
		return readLine(0);
	}
	
	/* (non-Javadoc)
	 * @see com.maverick.ssh.ShellReader#readLine(long)
	 */
	public synchronized String readLine(long timeout) throws SshException, ShellTimeoutException {
		
		if(!isActive())
			return null;
		
		StringBuffer line = new StringBuffer();
		int ch;
		
		long time = System.currentTimeMillis();

		do {
			try {
				ch = in.read();
				if(ch == -1 || ch == '\n') {
					if(line.length()==0 && ch==-1)
						return null;
					return line.toString();
				}
				if(ch != '\n' && ch!='\r') {
					line.append((char)ch);
				}
			} catch (SshIOException e) {
				if(e.getRealException().getReason()==SshException.MESSAGE_TIMEOUT) {
					// Ignore this exception as shell will throw its own ShellTimeoutException
				} else {
					throw e.getRealException();
				}
			} catch(IOException e) {
				throw new SshException(e);
			}
			
			
		} while (System.currentTimeMillis() - time < timeout || timeout == 0);
		
		throw new ShellTimeoutException();

	}

	public int getReadlimit() {
		return readlimit;
	}

	public void setReadlimit(int readlimit) {
		this.readlimit = readlimit;
	}
	
	
}
