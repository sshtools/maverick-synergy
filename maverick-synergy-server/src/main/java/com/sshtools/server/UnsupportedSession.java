/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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

package com.sshtools.server;

import java.io.IOException;
import java.nio.ByteBuffer;

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
    
    protected void processStdinData(ByteBuffer data) {
        // Do nothing
    }

    protected void processStderrData(ByteBuffer data) {
        // Do nothing
    }

    protected void onChannelClosed() {
        // Do nothing
    }

    protected boolean executeCommand(String cmd) {
        return false;
    }

    protected void changeWindowDimensions(int cols, int rows, int width, int height) {
        // Do nothing
    }

    public void onSessionOpen() {
        // Do nothing
    }

    protected void onLocalEOF() {
        // The local side is EOF no more data can be sent
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

    protected void processSignal(String signal) {
        // Do Nothing
    }

    protected void onRemoteEOF() {
        // The remote side is EOF no more data will be received
    }

    public boolean setEnvironmentVariable(String name, String value) {
        return false;
    }
}
