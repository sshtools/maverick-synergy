
package com.sshtools.server;

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;

/**
 * This is a basic session that provides a message to the user to inform them
 * that a shell or command cannot be executed because the server does not
 * support an interactive session.
 * 
 * 
 */
public class UnsupportedSession extends SessionChannelNG {

    String message = "This server does not support an interactive session.\r\nGoodbye.\r\n";

    
    public UnsupportedSession(SshConnection con) {
    	super(con);
    }

    protected boolean executeCommand(String cmd) {
        return false;
    }

    protected boolean startShell() {
    	
    	con.executeTask(new ConnectionAwareTask(con) {
			
			@Override
			protected void doTask() {
				try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                try {
                	UnsupportedSession.this.sendChannelDataAndBlock(message.getBytes());
					UnsupportedSession.this.close();
				} catch (IOException e) {
					Log.error("Channel I/O error", e);
				}
			}
		});
    	
 
        return true;
    }

    protected boolean allocatePseudoTerminal(String parm1, int parm2, int parm3, int parm4, int parm5, byte[] parm6) {
        return true;
    }
    

    public boolean setEnvironmentVariable(String name, String value) {
        return false;
    }

	@Override
	protected void changeWindowDimensions(int cols, int rows, int width, int height) {

	}

	@Override
	protected void onLocalEOF() {

	}

	@Override
	protected void processSignal(String signal) {
		
	}
}
