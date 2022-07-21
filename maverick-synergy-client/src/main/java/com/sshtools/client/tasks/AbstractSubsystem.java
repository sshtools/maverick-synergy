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

package com.sshtools.client.tasks;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.IOUtils;
import com.sshtools.synergy.ssh.ByteArrays;
import com.sshtools.synergy.ssh.PacketPool;

/**
 * An abstract task for connecting to an SSH subsystem.
 */
public abstract class AbstractSubsystem {
	
	
	protected long timeout = 60000;
	DataInputStream in;
	UUID taskUUID = UUID.randomUUID();
	protected SshConnection con;
	protected SessionChannelNG session;
	
	public AbstractSubsystem(SshConnection con) {
		this.con = con;
		session = new SessionChannelNG(
				getMaximumPacketSize(),
				getMaximumWindowSize(), 
				getMaximumWindowSize(),
				getMinimumWindowSize(),
				false);
		
		session.addEventListener(new ChannelEventListener() {

			@Override
			public void onChannelClose(Channel channel) {
				IOUtils.closeStream(in);
				onCloseSession((SessionChannelNG) channel);
			}

		});

		con.openChannel(session);
		in = new DataInputStream(session.getInputStream());
		if(!session.getOpenFuture().waitFor(timeout).isSuccess()) {
			throw new IllegalStateException(
					"Could not open session channel");
		}
		
		con.setProperty(taskUUID.toString(), session);
	}

	protected abstract int getMinimumWindowSize();

	protected abstract int getMaximumWindowSize();

	protected abstract int getMaximumPacketSize();

	protected SessionChannelNG getSession() {
		return (SessionChannelNG) con.getProperty(taskUUID.toString());
	}
	
	public synchronized byte[] nextMessage() throws SshException {
		
		int len = -1; 
		  try {
	        len = in.readInt();

	        if(len < 0)
	            throw new SshException("Negative message length in SFTP protocol.",
	                                   SshException.PROTOCOL_VIOLATION);

	        if(len > con.getContext().getMaximumPacketLength())
	            throw new SshException("Invalid message length in SFTP protocol [" + len + "]",
	                                   SshException.PROTOCOL_VIOLATION);

	        byte[] msg = ByteArrays.getInstance().getByteArray();
	        in.readFully(msg, 0, len);

	        return msg;
	    } catch(OutOfMemoryError ex) {
	        throw new SshException("Invalid message length in SFTP protocol [" + len + "]",
	                                   SshException.PROTOCOL_VIOLATION);
	    } catch (EOFException ex) {
	        getSession().close();
	        throw new SshException("The channel unexpectedly terminated",
	                               SshException.CHANNEL_FAILURE);
	    } catch (IOException ex) {

	        if(ex instanceof SshIOException)
	            throw ((SshIOException)ex).getRealException();

	        getSession().close();
	        
	        throw new SshException(SshException.CHANNEL_FAILURE, ex);
	    }
	}
	
	protected void onCloseSession(SessionChannelNG session) {
		try {
			in.close();
		} catch (IOException e) {
		}
	}
	
	public void sendMessage(Packet msg) throws SshException {
		
		SessionChannelNG session = getSession();
		msg.finish();
		try {
			session.sendChannelDataAndBlock(msg.array(), 0, msg.size(), new PacketReturner(msg));
		} catch (IOException e) {
			Log.error("Channel I/O error", e);
		}
	}

	class PacketReturner implements Runnable {
		Packet msg;
		
		PacketReturner(Packet msg) {
			this.msg = msg;
		}
		
		public void run() {
			PacketPool.getInstance().putPacket(msg);
			msg = null;
		}
	}
}
