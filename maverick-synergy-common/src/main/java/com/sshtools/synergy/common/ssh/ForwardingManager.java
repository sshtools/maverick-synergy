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
package com.sshtools.synergy.common.ssh;

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

	Map<Connection<T>, List<String>> portsByConnection = new HashMap<Connection<T>, List<String>>();
	protected Map<String, ForwardingFactory<T>> listeningPorts = Collections
			.synchronizedMap(new HashMap<String, ForwardingFactory<T>>());

	public ForwardingManager() {
	}
	
	public ForwardingFactory<T> getFactory(String addressToBind, int portToBind) {
		return listeningPorts.get(addressToBind + ":" + portToBind);
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

	public synchronized int startListening(String addressToBind, int portToBind, Connection<T> con,
			SocketListeningForwardingFactoryImpl<T> forwardingFactory) throws SshException {

		String key = addressToBind + ":" + portToBind;
		
		if (portToBind > 0 && isListening(portToBind)) {
			throw new SshException("Port " + portToBind + " already in use", SshException.FORWARDING_ERROR);
		}

		try {
			
			portToBind = forwardingFactory.bindInterface(addressToBind, portToBind, con.getConnectionProtocol(),
					forwardingFactory.getChannelType());
			key = addressToBind + ":" + portToBind;
			
			listeningPorts.put(key, forwardingFactory);
			
			if(!portsByConnection.containsKey(con)) {
				portsByConnection.put(con,  new ArrayList<String>());
			}
			
			portsByConnection.get(con).add(key);
			
			EventServiceImplementation.getInstance()
					.fireEvent((new Event(this, forwardingFactory.getStartedEventCode(), true))
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
				Log.debug("Could not instantiate remote forwarding channel factory", t);
		}

		throw new SshException("Failed to start listening socket on " + addressToBind + ":" + portToBind,
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

			ForwardingFactory<T> ff = (ForwardingFactory<T>) listeningPorts.get(key);

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
