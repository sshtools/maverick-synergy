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
package com.sshtools.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.ssh.ChannelNG;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionProtocol;
import com.sshtools.common.ssh.ConnectionStateListener;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.RemoteForward;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

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
				for (ConnectionStateListener<SshClientContext> stateListener : getContext().getStateListeners()) {
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
	 */
	@Override
	protected ChannelNG<SshClientContext> createChannel(String channeltype, Connection<SshClientContext> con)
			throws UnsupportedChannelException, PermissionDeniedException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

}
