package com.maverick.agent.client;

import java.io.IOException;
import java.net.Socket;

import com.maverick.ssh.ChannelEventListener;
import com.maverick.ssh.SshChannel;
import com.maverick.ssh2.Ssh2Channel;
import com.sshtools.common.logger.Log;
import com.sshtools.common.util.IOUtil;

public class AgentSocketForwardingChannel extends Ssh2Channel implements ChannelEventListener{

	
	Socket socket;
	
	public AgentSocketForwardingChannel(String name, Socket socket) {
		super(name, 1024000, 32768);
		this.socket = socket;
		addChannelEventListener(this);
	}

	public void channelOpened(SshChannel channel) {
		Thread t = new Thread() {
			public void run() {
				try {
					IOUtil.copy(socket.getInputStream(), getOutputStream());
				} catch (IOException e) {
					Log.error("I/O error during socket transfer", e);
					close();
				}
			}
		};
		
		t.start();
	}

	public void channelClosing(SshChannel channel) {
	}

	public void channelClosed(SshChannel channel) {
	}

	public void channelEOF(SshChannel channel) {
	}

	public void dataReceived(SshChannel channel, byte[] data, int off, int len) {
		try {
			socket.getOutputStream().write(data, off, len);
		} catch (IOException e) {
			Log.error("I/O error during socket transfer", e);
			close();
		}
	}

	public void dataSent(SshChannel channel, byte[] data, int off, int len) {
	}

	public void extendedDataReceived(SshChannel channel, byte[] data, int off, int len, int extendedDataType) {
	}

}
