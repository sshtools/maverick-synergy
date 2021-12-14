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
