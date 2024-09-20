package com.sshtools.server;

/*-
 * #%L
 * Server API
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

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.synergy.ssh.TerminalModes;

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

    @Override
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

    @Override
	protected boolean allocatePseudoTerminal(String parm1, int parm2, int parm3, int parm4, int parm5, TerminalModes parm6) {
        return true;
    }


    @Override
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
