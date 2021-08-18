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

package com.sshtools.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ChannelNG;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.RemoteForward;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * Implements the client side of the SSH connection prototocol
 */
public class ConnectionProtocolClient extends ConnectionProtocol<SshClientContext> {

	
	
	Map<String,RemoteForward> remoteForwards = new HashMap<String,RemoteForward>();
	
	public ConnectionProtocolClient(TransportProtocol<SshClientContext> transport, String username) {
		super(transport, username);
	}

	@Override
	protected boolean isClient() {
		return true;
	}
	
	@Override
	protected void onStart() {

		SshClientContext context = getContext();
		con = context.getConnectionManager().registerConnection(this);

		addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(con, new Runnable() {
			public void run() {
				for (ConnectionStateListener stateListener : getContext().getStateListeners()) {
					stateListener.connected(con);
				}
			}
		}));

	}

	protected void onStop() {
		
	}
	/**
	 * Start local port forwarding. Listening on a local interface and forwarding the data to a host on the remote network.
	 * 
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return
	 * @throws UnauthorizedException
	 * @throws SshException
	 */
	public int startLocalForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort)
			throws UnauthorizedException, SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Requesting local forwarding on " + addressToBind + ":" + portToBind + " to " + destinationHost
					+ ":" + destinationPort);
		}

		if (!getContext().getForwardingPolicy().checkInterfacePermitted(con, addressToBind, portToBind)) {
			if(Log.isInfoEnabled()) {
				Log.info("User not permitted to forward on " + addressToBind + ":" + portToBind);
			}
			throw new UnauthorizedException();
		}

		int port = getContext().getForwardingManager().startListening(addressToBind, portToBind, con,
				new LocalForwardingFactoryImpl(destinationHost, destinationPort));
		
		if(Log.isInfoEnabled()) {
			Log.info("Local forwarding is now active on local interface " + addressToBind + ":" + portToBind 
					+ " forwarding to remote " + destinationHost + ":" + destinationPort);
		}
		
		return port;
	}
	
	
	public void stopLocalForwarding() {
		getContext().getForwardingManager().stopForwarding(getConnection());
	}
	
	public void stopLocalForwarding(String addressToBind, int portToBind) {
		stopLocalForwarding(addressToBind + ":" + portToBind);
	}
	
	public void stopLocalForwarding(String key) {
		getContext().getForwardingManager().stopForwarding(key, getConnection());
	}
	
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Canceling remote forwarding from " + addressToBind + ":" + portToBind);
		}
		ByteArrayWriter msg = new ByteArrayWriter();
		try {
			msg.writeString(addressToBind);
			msg.writeInt(portToBind);

			
			GlobalRequest request = new GlobalRequest("cancel-tcpip-forward", con, msg.toByteArray());

			sendGlobalRequest(request, true);
			request.waitForever();
			
			if(request.isSuccess()) {
				
				if(Log.isInfoEnabled()) {
					Log.info("Remote forwarding cancelled on remote interface " + addressToBind + ":" + portToBind);
				}
				
				remoteForwards.remove(addressToBind + ":" + portToBind);
				getConnection().setProperty("remoteForwards", remoteForwards);

			} else {
				throw new SshException("Cancel remote forwarding on interface " 
							+ addressToBind + ":" + portToBind + " failed", SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				msg.close();
			} catch (IOException e) {
			}
		}
	}

	public void stopRemoteForwarding() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Start remote port forwarding. Requests that the server starts a listening socket on the remote network and delivers
	 * data to a host on the local network.
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return
	 * @throws SshException
	 */
	public int startRemoteForwarding(String addressToBind,
			int portToBind, String destinationHost, int destinationPort) throws SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Requesting remote forwarding from " + addressToBind + ":" + portToBind + " to " + destinationHost
					+ ":" + destinationPort);
		}
		ByteArrayWriter msg = new ByteArrayWriter();
		try {
			msg.writeString(addressToBind);
			msg.writeInt(portToBind);

			
			GlobalRequest request = new GlobalRequest("tcpip-forward", con, msg.toByteArray());

			sendGlobalRequest(request, true);
			request.waitForever();
			
			if(request.isSuccess()) {
				
				if(request.getData().length > 0) {
					try (ByteArrayReader r = new ByteArrayReader(request.getData())) {
						portToBind = (int) r.readInt();
					}
				}
				
				if(Log.isInfoEnabled()) {
					Log.info("Remote forwarding is now active on remote interface " + addressToBind + ":" + portToBind 
							+ " forwarding to " + destinationHost + ":" + destinationPort);
				}
				
				remoteForwards.put(addressToBind + ":" + portToBind, new RemoteForward(destinationHost, destinationPort));
				getConnection().setProperty("remoteForwards", remoteForwards);
				
				return portToBind;
			} else {
				throw new SshException("Remote forwarding on interface " 
							+ addressToBind + ":" + portToBind + " failed", SshException.FORWARDING_ERROR);
			}
		} catch (IOException e) {
			throw new SshException(SshException.INTERNAL_ERROR, e);
		} finally {
			try {
				msg.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public SshClientContext getContext() {
		return getTransport().getContext();
	}

	/**
	 * Process remote forwarding cancel request. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processTCPIPCancel(ByteArrayReader bar, ByteArrayWriter msg) throws IOException {
		return false;
	}

	/**
	 * Process a request for remote forwarding. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processTCPIPForward(ByteArrayReader bar, ByteArrayWriter response) throws IOException {
		return false;
	}

	/**
	 * The name of the ssh service i.e. ssh-connection
	 */
	public String getName() {
		return "ssh-connection";
	}

	/**
	 * Create an SSH channel. This method delegates creation to the ChannelFactory installed on the current
	 * SshContext.
	 * @throws ChannelOpenException 
	 */
	@Override
	protected ChannelNG<SshClientContext> createChannel(String channeltype, Connection<SshClientContext> con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

}
