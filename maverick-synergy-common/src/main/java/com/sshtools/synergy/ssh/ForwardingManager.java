package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
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
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.forwarding.ForwardingHandle;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.forwarding.ForwardingRequest.ForwardingRequestBuilder;
import com.sshtools.common.forwarding.ForwardingRequest.Protocol;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;

/**
 * This class provides management of remote forwarding requests.
 */
public class ForwardingManager<T extends SshContext> {

	/**
	 * Attach a handle to a connection.
	 * 
	 * @param key key
	 * @param conn connection
	 * @param handle handle
	 * @return handle handle
	 */
	public static ForwardingHandle attachToConnection(Connection<?> conn, ForwardingHandle handle) {
		List<ForwardingHandle> hndls = conn.getProperty(handle.type().key());
		if(hndls == null) {
			hndls = Collections.synchronizedList(new ArrayList<>());
			conn.setProperty(handle.type().key(), hndls);
		}
		hndls.add(handle);
		return handle;
	}
	
	/**
	 * Detach a handle from a connection.
	 * 
	 * @param key key
	 * @param conn connection
	 * @param handle handle
	 * @return handle was removed
	 */
	public static boolean detachFromConnection(Connection<?> conn, ForwardingHandle handle) {
		List<ForwardingHandle> hndls = conn.getProperty(handle.type().key());
		if(hndls != null) {
			var removed = hndls.remove(handle);
			if(hndls.isEmpty())
				conn.removeProperty(handle.type().key());
			return removed;
		}
		return false;
	}

	/**
	 * Get all forwards of a specified type that are attached to connection. If
	 * there are no remote forwards, an empty list will be returned. This list is
	 * not modifiable, and may be iterated over to close any forwards contained
	 * within (the list itself will not be updated, so this method must be called
	 * again to obtain a new list).
	 * 
	 * @param type type
	 * @param con  connection
	 * @return remote forwards for connection
	 */
	public static List<ForwardingHandle> attached(ForwardingRequest.ForwardingType type, ConnectionProtocol<?> con) {
		List<ForwardingHandle> remoteForwards = con.getConnection().getProperty(type.key());
		return remoteForwards == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(remoteForwards));
	}
	
	private final List<ForwardingFactory<T, ForwardingChannelFactory<T>>> forwardingFactories = new ArrayList<>();
	private final List<RemoteForwardRequestHandler<T>> remoteForwardRequestHandlers = Collections.synchronizedList(new ArrayList<>());
	private final Map<ForwardingHandle, ForwardingChannelFactory<T>> listening = new ConcurrentHashMap<>();

	public ForwardingManager() {
	}

	/**
	 * Deprecated, no known use.
	 * 
	 * @param addressToBind address to bind
	 * @param portToBind port ot bind
	 * @return factory
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public ForwardingChannelFactory<T> getFactory(String addressToBind, int portToBind) {
		synchronized(listening) {
			var key = addressToBind + ":" + portToBind;
			return listening.entrySet().stream().filter(l -> l.getKey().request().bindName().equals(key)).findFirst().map(ent -> ent.getValue()).orElse(null);
		}
	}

	/**
	 * Get the forwarding factory.
	 * <p>
	 * Deprecated, there may now be multiple {@link ForwardingFactory} registered.
	 * 
	 * @return forwarding factory.
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public ForwardingFactory<T, ForwardingChannelFactory<T>> getForwardingFactory() {
		return forwardingFactories.isEmpty() ? null : forwardingFactories.get(0);
	}

	/**
	 * Get the forwarding factories.
	 * 
	 * @return forwarding factories.
	 */
	public List<ForwardingFactory<T, ForwardingChannelFactory<T>>> getForwardingFactories() {
		return Collections.unmodifiableList(forwardingFactories);
	}

	/**
	 * Set the forwarding factory. Note, will replace ALL other forwarding factories.
	 * 
	 * @param forwardingFactory forwarding factory.
	 */
	public void setForwardingFactory(
			ForwardingFactory<T, ForwardingChannelFactory<T>> forwardingFactory) {
		forwardingFactories.clear();
		forwardingFactories.add(forwardingFactory);
	}

	/**
	 * Add a forwarding factory, used to create local forwarding tunnels.
	 * 
	 * @param forwardingFactory forwarding factory.
	 */
	public void addForwardingFactory(
			ForwardingFactory<T, ForwardingChannelFactory<T>> forwardingFactory) {
		forwardingFactories.add(0, forwardingFactory);
	}

	/**
	 * Remove a forwarding factory, used to create local forwarding tunnels.
	 * 
	 * @param forwardingFactory forwarding factory.
	 */
	public void removeForwardingFactory(
			ForwardingFactory<T, ForwardingChannelFactory<T>> forwardingFactory) {
		forwardingFactories.remove(forwardingFactory);
	}
	
	/**
	 * Add a remote forward request handler, used to handle incoming remote forwarding
	 * requests.
	 * 
	 * @param handler handler to add
	 */
	public void addRemoteForwardRequestHandler(RemoteForwardRequestHandler<T> handler) {
		remoteForwardRequestHandlers.add(handler);
	}

	
	/**
	 * Remove a remote forward request handler, used to handle incoming remote forwarding
	 * requests.
	 * 
	 * @param handler handler to remove
	 */
	public void removeRemoteForwardRequestHandler(RemoteForwardRequestHandler<T> handler) {
		remoteForwardRequestHandlers.remove(handler);
	}

	/**
	 * Get an unmodifiable list of remote forward request handles, used to handle incoming remote
	 * forwarding requests.
	 * 
	 * @return handlers
	 */
	public List<RemoteForwardRequestHandler<T>> getRemoteForwardRequestHandlers() {
		return Collections.unmodifiableList(remoteForwardRequestHandlers);
	}

	/**
	 * Is there an existing listener bound to a particular port?
	 * <p> 
	 * Note, this assumes that a forward is bound to all addresses (where appropriate), so will return
	 * <code>true</code> if <strong>any</strong> forward exists that has the same port
	 * numbers, regardless of the bound address.
	 * <p>
	 * Deprecated. You should get the list of forwards, such as with {@link #getLocalBinds()}
	 * and iterate the handles yourself and then {@link ForwardingHandle#conflicts(ForwardingHandle, ForwardingRequest)}
	 * to see if any potentially new forward would be incompatible with an existing one. This method 
	 * takes care of the possible different types of forward (TCP vs Unix Domain Sockets)
	 * 
	 * @param port port
	 * @return port in use local
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public synchronized boolean isListening(int port) {
		synchronized(listening) {
			var req = ForwardingRequestBuilder.create().
					withBindAll().
					withPort(port).
					build();
			for(var hndl : listening.keySet()) {
				if(ForwardingHandle.conflicts(hndl, req)) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Get all started bound listening forwards on this host. If there are no listening
	 * forwards, an empty list will be returned. This list is not modifiable,
	 * and may be iterated over to close any forwards contained within (the list itself
	 * will not be updated, so this method must be called again to obtain a new list).
	 * <p>
	 * In the context on an <em>SSH Client</em>, this will be all of the <strong>Local</strong>
	 * port forwards that have been started.
	 * <p>
	 * In the context on an <em>SSH Server</em>, this will be all of the <strong>Remote</strong>
	 * port forwards that all clients have requested.  
	 * 
	 * @return local binds
	 */
	public List<ForwardingHandle> getLocalBinds() {
		return Collections.unmodifiableList(new ArrayList<>(listening.keySet()));
	}
	
	/**
	 * Get all started bound listening forwards for a given connection. If there are no listening
	 * forwards, an empty list will be returned. This list is not modifiable,
	 * and may be iterated over to close any forwards contained within (the list itself
	 * will not be updated, so this method must be called again to obtain a new list).
	 * <p>
	 * In the context on an <em>SSH Client</em>, this will be all of the <strong>Local</strong>
	 * port forwards that this clients connection has started.
	 * <p>
	 * In the context on an <em>SSH Server</em>, this will be all of the <strong>Remote</strong>
	 * port forwards that this connection has requested.  
	 * 
	 * @return local binds
	 */
	public List<ForwardingHandle> getLocalBinds(ConnectionProtocol<T> con) {
		return attached(ForwardingRequest.ForwardingType.LOCAL, con);
	}
	
	/**
	 * Get all started remote forwarding binds for a given connection. If there are no
	 * remote forward binds, an empty list will be returned. This list is not modifiable,
	 * and may be iterated over to close any forwards contained within (the list itself
	 * will not be updated, so this method must be called again to obtain a new list).
	 * 
	 * @param con connection
	 * @return remote forwards for connection
	 * @see #bindRemote(ForwardingRequest, ConnectionProtocol)
	 * @see #closeAllRemote(ConnectionProtocol)
	 */
	public List<ForwardingHandle> getRemoteBinds(ConnectionProtocol<T> con) {
		return attached(ForwardingRequest.ForwardingType.REMOTE, con);
	}

	/**
	 * Stop listening for remote forwarding for a given connection.
	 * <p>
	 * Deprecated, see {@link #closeAllRemote(ConnectionProtocol)}.
	 * 
	 * @param con connection
	 * @throws IOException on error closing tunnels
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopRemoteForwarding(ConnectionProtocol<T> con) throws SshException {
		try {
			closeAllRemote(con);
		}
		catch(IOException ioe) {
			if(ioe.getCause() instanceof SshException sshe)
				throw sshe;
			else
				throw new UncheckedIOException(ioe);
		}
	}

	/**
	 * Requests that the remote end unbinds all listening resources such as TCP or Unix 
	 * Domain Sockets for this connection and stops forwarding any connections from 
	 * those to this host.
	 * 
	 * @param con connection
	 * @throws IOException on error closing tunnels
	 * @see #closeAllRemote(ConnectionProtocol)
	 * @see #getRemoteBinds(ConnectionProtocol)
	 */
	public void closeAllRemote(ConnectionProtocol<T> con) throws IOException {

		if(Log.isInfoEnabled()) {
			Log.info("Canceling all remote forwarding for connection");
		}
		
		IOException exception = null;		
		for(var h : getRemoteBinds(con)) {
			try {
				h.close();
			}
			catch(IOException e) {
				exception = e;
			}
		}
		if(exception != null)
			throw exception;
			
	}

	/**
	 * Stop remote forwarding for a given connection, remote address and port.
	 * <p>
	 * Deprecated, no replacement. Use {@link #getRemoteBinds(ConnectionProtocol)} and
	 * close the required handles yourself.
	 * 
	 * @param hostToBind address
	 * @param portToBind port
	 * @param connection connect
	 * @throws SshException on SSH error
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopRemoteForwarding(String hostToBind, int portToBind, ConnectionProtocol<T> connection) throws SshException {
		synchronized(remoteForwardRequestHandlers) {

			if(Log.isInfoEnabled()) {
				Log.info("Canceling remote forwarding from " + hostToBind + ":" + portToBind);
			}
			
			var it = getRemoteBinds(connection).stream().
					filter(rf -> rf.request().bindAddress().equals(hostToBind) 
							&& rf.boundPort().orElse(0) == portToBind).
					iterator();
			
			if(it.hasNext()) {
				IOException exception = null;
				while(it.hasNext()) {
					try {
						it.next().close();
					} catch (IOException e) {
						exception = e;
					}
				}
				if(exception != null) {
					if(exception.getCause() instanceof SshException sshe)
						throw sshe;
					else
						throw new UncheckedIOException(exception);
				}
			}
			else {
				throw new SshException(SshException.INTERNAL_ERROR, "Nothing handled closing the remote forward.");
			}
		}
	}

	/**
	 * Requests that the remote end start a new remote forwarding. 
	 * <p>
	 * Deprecated, see {@link #bindRemote(ForwardingRequest, ConnectionProtocol)}
	 * 
	 * @param addressToBind address to bind
	 * @param portToBind port to bind
	 * @param destinationHost destination host
	 * @param destinationPort destination port
	 * @param con connection
	 * @return bound port
	 * @throws SshException on SSH error
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public int startRemoteForwarding(String addressToBind, int portToBind, String destinationHost,
			int destinationPort, ConnectionProtocol<T> con) throws SshException {
		var bldr = ForwardingRequestBuilder.create().
				withProtocol(Protocol.TCP);
		if(addressToBind != null) {
			bldr.withBindAddress(addressToBind).
				withBindPort(portToBind);
		}
		
		if(destinationHost != null) {
			bldr.withDestinationAddress(destinationHost).
				withDestinationPort(destinationPort);
		}
		
		return bindRemote(bldr.
				build(), con).boundPort().orElse(0);
	}

	/**
	 * Requests that the remote end of the connection create and binds to a listening resources, such
	 * as TCP or Unix Domain Socket server, and forward any connections to those to this host.
	 * 
	 * @param request request 
	 * @param con connection
	 * @return forwarding handle
	 * @throws SshException on error
	 */
	public ForwardingHandle bindRemote(ForwardingRequest request, ConnectionProtocol<T> con) throws SshException {
		synchronized(remoteForwardRequestHandlers) {
			for(var handler : remoteForwardRequestHandlers) {
				if(handler.isHandled(request, con)) {
					return attachToConnection(
						con.getConnection(), 
						ForwardingHandle.onClose(handler.startRemoteForward(request, con),  hndl -> {
							detachFromConnection(con.getConnection(), hndl);
						})
					);
				}
			}
			throw new SshException(SshException.INTERNAL_ERROR, "Nothing handled the remote forwarding request.");
		}
	}

	/**
	 * Start listening for local port forwards.
	 * <p>
	 * Deprecated, see {@link #bindLocal(ForwardingRequest, Connection)}.
	 * 
	 * @param addressToBind address to bind
	 * @param portToBind port to bind
	 * @param con connection
	 * @param destinationHost destination host
	 * @param destinationPort destination port
	 * @return bound port
	 * @throws SshException on SSH error
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public synchronized int startListening(String addressToBind, int portToBind, Connection<T> con, String destinationHost, int destinationPort) throws SshException {
		
		var bldr = ForwardingRequestBuilder.create().
				withProtocol(Protocol.TCP);
		
		if(addressToBind != null) {
			bldr.withBindAddress(addressToBind).
				withBindPort(portToBind);
		}
		
		if(destinationHost != null) {
			bldr.withDestinationAddress(destinationHost).
				withDestinationPort(destinationPort);
		}
		
		return bindLocal(bldr.
				build(), con).boundPort().orElse(0);

	}

	/**
	 * Create and bind to a local resource such as a listening TCP or unix domain socket that will
	 * accept connections and forward them to the other end of the connection.
	 * <p>
	 * If this is running in the context of an <em>SSH Client</em>, then the listening service
	 * will be created on <strong>this</strong> this host. 
	 * <p>
	 * If this is running in the context of an <em>SSH Server</em>, then a request will be sent
	 * to the remote end to open a listening service. 
	 * 
	 * @param request request
	 * @param con connection
	 * @return handle
	 * @throws SshException on SSH error
	 */
	public synchronized ForwardingHandle bindLocal(ForwardingRequest request, Connection<T> con) throws SshException {

		synchronized(listening) {
			for(var other : listening.keySet()) {
				if(ForwardingHandle.conflicts(other, request))
					throw new SshException(MessageFormat.format("{0} already in use", request.bindName()), SshException.FORWARDING_ERROR);
			}
		}
		
		Throwable cause = null;
		
		for(var forwardingFactory : forwardingFactories) {
			if(!forwardingFactory.isHandled(request))
				continue;
			
			var forwardingChannelFactory = forwardingFactory.createChannelFactory(request);
	
			try {
				
				var handle = attachToConnection(con, ForwardingHandle.onClose(forwardingChannelFactory.bindInterface(request, con.getConnectionProtocol(), forwardingChannelFactory.getChannelType()), hndl -> {
					try {
						listening.remove(hndl);
						detachFromConnection(con, hndl);
					}
					finally {
						EventServiceImplementation.getInstance().fireEvent((new Event(this, forwardingChannelFactory.getStoppedEventCode(), true))
								.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, con)
								.addAttribute(EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_ENTRANCE, request.bindName())
								.addAttribute(EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_HANDLE,
										hndl)
						);
					}
				}));
				
				listening.put(handle, forwardingChannelFactory);
				
				EventServiceImplementation.getInstance()
						.fireEvent((new Event(this, forwardingChannelFactory.getStartedEventCode(), true))
								.addAttribute(EventCodes.ATTRIBUTE_CONNECTION, con)
								.addAttribute(EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_ENTRANCE,
										request.bindName())
								.addAttribute(EventCodes.ATTRIBUTE_FORWARDING_TUNNEL_HANDLE,
										handle));
	
				if(Log.isDebugEnabled()) {
					Log.debug("Listening for new connections on {}. Actually bound to {}", 
							request.bindName(), handle.boundPath().orElseGet(() -> String.valueOf(handle.boundPort().orElse(0))));
				}
	
				return handle;
	
			} catch (IOException ex) {
				/* TODO the catching of both of these exceptions just seems wrong.
				 *      Doing this, you'll never know (unless you enable DEBUG 
				 *      log) why the bind failed. Surely if the factory claims it
				 *      CAN handle the request, the we should throw the exception
				 *      out of this method  and not try any more factories. Is there
				 *      any case where we rely on canHandled is being treated only
				 *      as some kind of initial check?
				 */
				
				if(Log.isDebugEnabled())
					Log.debug("Exception caught on socket bind", ex);
				
				cause = ex;
			} catch (Throwable t) {
				if(Log.isDebugEnabled())
					Log.debug("Could not instantiate forwarding channel factory", t);

				cause = t;
			}
		}

		throw new SshException("Failed to start listening socket on " + request.bindName(),
				SshException.FORWARDING_ERROR, cause);

	}

	/**
	 * Stop all local forwards for a given connection.
	 * <p>
	 * Deprecated, renamed for consistency to {@link #closeAllLocal(Connection)}.
	 * 
	 * @param con connection
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopForwarding(Connection<T> con) {
		closeAllLocal(con);
	}

	/**
	 * Stop all local listening forwards for a given connection. No exceptions will be thrown if
	 * stopping any forward fails. If you wish to catch exceptions, using {@link #getLocalBinds()}
	 * and iterate and close handles yourself.
	 * 
	 * @param con connection
	 */
	public void closeAllLocal(Connection<T> con) {
		for(var fwd : attached(ForwardingRequest.ForwardingType.LOCAL, con.getConnectionProtocol())) {
			try {
				fwd.close();
			} catch (IOException e) {
			}
		}
	}


	/**
	 * Stop all forwards for a given connection and key.
	 * <p>
	 * Deprecated. Use {@link #getLocalBinds()}, iterate and {@link ForwardingHandle#close()}
	 * all handles yourself.
	 * 
	 * @param con connection
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public void stopForwarding(String key, Connection<T> con) {
		for (var fwd : getLocalBinds()) {
			if (fwd.request().bindName().equals(key)) {
				try {
					fwd.close();
				} catch (IOException e) {
				}
				return;
			}
		}
	}

	/**
	 * Stop remote forwarding.
	 * 
	 * @param addressToBind address to bind
	 * @param portToBind port to bind
	 * @param dropActiveTunnels drop active tunnels
	 * @param connection connection
	 * @return tunnels were stopped
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public synchronized boolean stopListening(String addressToBind, int portToBind, boolean dropActiveTunnels,
			Connection<T> connection) {
		return stopListening(addressToBind + ":" + String.valueOf(portToBind), dropActiveTunnels, connection);
	}

	/**
	 * Stop remote forwarding.
	 * 
	 * @param key key
	 * @param dropActiveTunnels drop active tunnels
	 * @param connection connection
	 * @return tunnels were stopped
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public synchronized boolean stopListening(String key, boolean dropActiveTunnels,
			Connection<T> connection) {

		if(Log.isDebugEnabled()) {
			Log.debug("Forwarding cancelled for address " + key);
		}
		
		for(var bnd : getLocalBinds(connection.getConnectionProtocol())) {
			if(bnd.request().bindName().equals(key)) {
				try {
					bnd.close();
				} catch (IOException e) {
					if(Log.isDebugEnabled()) {
						Log.debug("Failed to stop listening on " + key, e);
					}
					return false;
				}

				if(Log.isDebugEnabled()) {
					Log.debug("Stopped listening on " + key);
				}
				return true;
			}
		}

		if(Log.isDebugEnabled()) {
			Log.debug("Failed to stop listening on " + key);
		}
		return false;
	}

	/**
	 * For sub-classes to implement X11 forwarding.
	 * <p>
	 * Deprecated. As every year passes, X11 forwarding becomes less and less useful. Many 
	 * Linux distributions are now shipping without X11 at all. It may still be useful in legacy
	 * cases, so this API will remain for a little while longer, bit it's use is discouraged.
	 * Being as X11 forwarding has never been implemented in this version of Maverick, and 
	 * nobody has asked for it, it is unlikely to affect anybody. 
	 * <p>
	 *  
	 * 
	 * @param singleConnection single connection
	 * @param protocol protocol
	 * @param cookie cookie
	 * @param screen screen
	 * @param connection connection
	 * @return success
	 */
	@Deprecated(forRemoval = true, since = "3.2.0")
	public boolean startX11Forwarding(boolean singleConnection, String protocol, byte[] cookie, int screen,
			ConnectionProtocol<T> connection) {
		return false;
	}
}
