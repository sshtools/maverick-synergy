/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;

/**
 * This class provides management of remote forwarding requests.
 */
public class ForwardingManager<T extends SshContext> {
	
	public interface Listener {
		
	}
	
	final static String REMOTE_FORWARDS_KEY = "remoteForwards";

	private ForwardingFactory<T, ForwardingChannelFactory<T>> forwardingFactory;
	private Map<Connection<T>, List<String>> portsByConnection = new HashMap<Connection<T>, List<String>>();
	private List<RemoteForwardRequestHandler<T>> remoteForwardRequestHandlers = Collections.synchronizedList(new ArrayList<>());
	
	protected Map<String, ForwardingChannelFactory<T>> listeningPorts = Collections
			.synchronizedMap(new HashMap<String, ForwardingChannelFactory<T>>());

	public ForwardingManager() {
	}
	
	public ForwardingChannelFactory<T> getFactory(String addressToBind, int portToBind) {
		return listeningPorts.get(addressToBind + ":" + portToBind);
	}

	public ForwardingFactory<T, ForwardingChannelFactory<T>> getForwardingFactory() {
		return forwardingFactory;
	}

	public void setForwardingFactory(
			ForwardingFactory<T, ForwardingChannelFactory<T>> forwardingFactory) {
		this.forwardingFactory = forwardingFactory;
	}
	
	public void addRemoteForwardRequestHandler(RemoteForwardRequestHandler<T> handler) {
		remoteForwardRequestHandlers.add(handler);
	}
	
	public void removeRemoteForwardRequestHandler(RemoteForwardRequestHandler<T> handler) {
		remoteForwardRequestHandlers.remove(handler);
	}

	public List<RemoteForwardRequestHandler<T>> getRemoteForwardRequestHandlers() {
		return Collections.unmodifiableList(remoteForwardRequestHandlers);
	}

	/**
	 * Is there an existing forwarding listening on a particular port?
	 * 
	 * @param port
	 *            int
	 * @return boolean
	 */
	public synchronized boolean isListening(int port) {
		synchronized(listeningPorts) {
			return listeningPorts.containsKey(String.valueOf(port))
					|| listeningPorts.containsKey("0.0.0.0:" + port)
					|| listeningPorts.containsKey("::" + port);
		}
	}

	public void stopRemoteForwarding(ConnectionProtocol<T> con) throws SshException {
		synchronized(remoteForwardRequestHandlers) {

			if(Log.isInfoEnabled()) {
				Log.info("Canceling all remote forwarding for connection");
			}
			
			SshException exception = null;
			
			@SuppressWarnings("unchecked")
			var remoteForwards = (Map<String, RemoteForward>)con.getConnection().getProperty(REMOTE_FORWARDS_KEY);
			if(remoteForwards != null) {
				for(var m : remoteForwards.entrySet()) {
					var a = m.getKey().split(":");
					var addressToBind = a[0];
					var portToBind = a.length > 1 ? Integer.parseInt(a[1]) : 0;
					for(var handler : remoteForwardRequestHandlers) {
						if(handler.isHandled(addressToBind, portToBind, m.getValue().getHostToConnect(), m.getValue().getPortToConnect(), con)) {
							try {
								handler.stopRemoteForward(addressToBind, portToBind, m.getValue().getHostToConnect(), m.getValue().getPortToConnect(), con);
							} catch (SshException e) {
								exception = e;
							}
						}
					}					
				}
			}
			remoteForwards.clear();
			con.getConnection().removeProperty(REMOTE_FORWARDS_KEY);
			if(exception != null)
				throw exception;
			
		}
	}

	public void stopRemoteForwarding(String addressToBind, int portToBind, ConnectionProtocol<T> connection) throws SshException {
		synchronized(remoteForwardRequestHandlers) {

			if(Log.isInfoEnabled()) {
				Log.info("Canceling remote forwarding from " + addressToBind + ":" + portToBind);
			}
			
			@SuppressWarnings("unchecked")
			var remoteForwards = (Map<String, RemoteForward>)connection.getConnection().getProperty(REMOTE_FORWARDS_KEY);
			var remoteForwardKey = addressToBind + ":" + portToBind;
			if(remoteForwards == null || !remoteForwards.containsKey(remoteForwardKey)) {
				if(Log.isDebugEnabled())
					Log.debug("No known remote forward for " + addressToBind + ":" + portToBind);
				return;
			}
			
			var remoteForward = remoteForwards.get(remoteForwardKey);
			
			for(var handler : remoteForwardRequestHandlers) {
				if(handler.isHandled(addressToBind, portToBind, remoteForward.getHostToConnect(), remoteForward.getPortToConnect(), connection)) {
					handler.stopRemoteForward(addressToBind, portToBind, remoteForward.getHostToConnect(), remoteForward.getPortToConnect(), connection);
					remoteForwards.remove(addressToBind + ":" + portToBind);
					if(remoteForwards.isEmpty())
						connection.getConnection().removeProperty(REMOTE_FORWARDS_KEY);
					return;
				}
			}
			throw new SshException(SshException.INTERNAL_ERROR, "Nothing handled closing the remote forward.");
		}
	}

	public int startRemoteForwarding(String addressToBind, int portToBind, String destinationHost,
			int destinationPort, ConnectionProtocol<T> con) throws SshException {
		synchronized(remoteForwardRequestHandlers) {
			for(var handler : remoteForwardRequestHandlers) {
				if(handler.isHandled(addressToBind, portToBind, destinationHost, destinationPort, con)) {
					portToBind = handler.startRemoteForward(addressToBind, portToBind, destinationHost, destinationPort, con);
					@SuppressWarnings("unchecked")
					var remoteForwards = (Map<String, RemoteForward>)con.getConnection().getProperty(REMOTE_FORWARDS_KEY);
					if(remoteForwards == null) {
						remoteForwards = new HashMap<>();
						con.getConnection().setProperty(REMOTE_FORWARDS_KEY, remoteForwards);
						
					}
					remoteForwards.put(addressToBind + ":" + portToBind, new RemoteForward(destinationHost, destinationPort));
					return portToBind;
				}
			}
			throw new SshException(SshException.INTERNAL_ERROR, "Nothing handled the remote forwarding request.");
		}
	}

	public synchronized int startListening(String addressToBind, int portToBind, Connection<T> con, String destinationHost, int destinationPort) throws SshException {

		String key = addressToBind + ":" + portToBind;
		
		if (portToBind > 0 && isListening(portToBind)) {
			throw new SshException("Port " + portToBind + " already in use", SshException.FORWARDING_ERROR);
		}
		
		var forwardingChannelFactory = forwardingFactory.createChannelFactory(destinationHost, destinationPort);

		try {
			
			portToBind = forwardingChannelFactory.bindInterface(addressToBind, portToBind, con.getConnectionProtocol(),
					forwardingChannelFactory.getChannelType());
			key = addressToBind + ":" + portToBind;
			
			listeningPorts.put(key, forwardingChannelFactory);
			
			if(!portsByConnection.containsKey(con)) {
				portsByConnection.put(con,  new ArrayList<String>());
			}
			
			portsByConnection.get(con).add(key);
			
			EventServiceImplementation.getInstance()
					.fireEvent((new Event(this, forwardingChannelFactory.getStartedEventCode(), true))
							.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, con)
							.addAttribute(EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_ENTRANCE,
									addressToBind + ":" + portToBind));

			if(Log.isDebugEnabled())
				Log.debug("Listening for new connections on " + addressToBind + ":" + portToBind);

			return portToBind;

		} catch (IOException ex) {
			if(Log.isDebugEnabled())
				Log.debug("Exception caught on socket bind", ex);
		} catch (Throwable t) {
			if(Log.isDebugEnabled())
				Log.debug("Could not instantiate forwarding channel factory", t);
		}

		throw new SshException("Failed to start listening socket on " + addressToBind + (portToBind > 0 ? ":" + portToBind : ""),
				SshException.FORWARDING_ERROR);

	}
	
	public void stopForwarding(Connection<T> con) {
		
		if (portsByConnection.containsKey(con)) {
			List<String> keys = new ArrayList<String>(portsByConnection.get(con));
			for (String key : keys) {
				stopListening(key, true, con);
			}
		}
		
	}
	
	public void stopForwarding(String key, Connection<T> con) {
		if(portsByConnection.containsKey(con)) {
			List<String> keys = portsByConnection.get(con);
			if(keys.contains(key)) {
				stopListening(key, true, con);
			}
		}
	}

	/**
	 * Stop remote forwarding.
	 * 
	 * @param addressToBind
	 *            String
	 * @param portToBind
	 *            int
	 * @param dropActiveTunnels
	 *            boolean
	 * @param connection
	 *            ConnectionProtocol
	 * @return boolean
	 */
	public synchronized boolean stopListening(String addressToBind, int portToBind, boolean dropActiveTunnels,
			Connection<T> connection) {

		String key = addressToBind + ":" + String.valueOf(portToBind);
		return stopListening(key, dropActiveTunnels, connection);
	}
	
	public synchronized boolean stopListening(String key, boolean dropActiveTunnels,
			Connection<T> connection) {

		if(Log.isDebugEnabled()) {
			Log.debug("Forwarding cancelled for address " + key);
		}
		if (listeningPorts.containsKey(key)) {

			ForwardingChannelFactory<T> ff = (ForwardingChannelFactory<T>) listeningPorts.get(key);

			if (ff.belongsTo(connection.getConnectionProtocol())) {
				ff.stopListening(dropActiveTunnels);
				portsByConnection.get(connection).remove(key);
				listeningPorts.remove(key);
				EventServiceImplementation.getInstance().fireEvent((new Event(this, ff.getStoppedEventCode(), true))
						.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, connection).addAttribute(
								EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_ENTRANCE, key));

				if(Log.isDebugEnabled()) {
					Log.debug("Stopped listening on " + key);
				}
			}
			return true;
		}

		if(Log.isDebugEnabled()) {
			Log.debug("Failed to stop listening on " + key);
		}
		return false;
	}

	public boolean startX11Forwarding(boolean singleConnection, String protocol, byte[] cookie, int screen,
			ConnectionProtocol<T> connection) {
		return false;
	}
}
