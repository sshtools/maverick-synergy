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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventException;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventTrigger;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ConnectionAwareTask;
import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.GlobalRequest;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshConnectionManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.synergy.nio.SshEngine;

public class Connection<T extends SshContext> implements EventTrigger, SshConnection {
	
	TransportProtocol<? extends Context> transport;
	ConnectionProtocol<T> connection;
	String username;
	boolean closed = false;
	Date startTime = new Date();
	HashMap<String,Object> properties = new HashMap<String,Object>();
	InetSocketAddress remoteAddress;
	InetSocketAddress localAddress;
	T context;
	
	List<EventListener> listeners = new ArrayList<EventListener>();
	Locale locale;
	
	public Connection(T context) {
		this.context = context;
		listeners.add(new EventListener() {
			
			@Override
			public void processEvent(Event evt) {
				if(evt.getId()==EventCodes.EVENT_DISCONNECTED) {
					if(!getAuthenticatedFuture().isDone()) {
						getAuthenticatedFuture().authenticated(false);
					}
				}
			}
		});
	}
	
	@Override
	public synchronized void addEventListener(EventListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public synchronized void removeEventListener(EventListener listener) {
		listeners.remove(listener);
	}

    public synchronized void fireEvent(Event evt)
    {
        EventException lastException = null;
        // Process global listeners
        for(EventListener listener : listeners) {
            try {
                listener.processEvent(evt);
            } catch(Throwable t) {
                if(t instanceof EventException) {
                    lastException = (EventException)t;
                    }
                else {
                    if(Log.isWarnEnabled()) {
                        Log.warn("Caught exception from event listener", t);
                    }
                }
            }
        }

        if (lastException != null)
            throw lastException;
    }
    
	public AuthenticatedFuture getAuthenticatedFuture() {
		return transport.getAuthenticatedFuture();
	}
	
	public String getSessionId() {
		return transport.getUUID();
	}
	
	public String getRemoteIdentification() {
		return transport.getRemoteIdentification();
	}
	
	@Override
	public ConnectionAwareTask addTask(ConnectionAwareTask r) {
		context.getExecutorService().execute(r);
		return r;
	}
	
	public ConnectionAwareTask addTask(Runnable r) {
		var t = new ConnectionTaskWrapper(this, r);
		context.getExecutorService().execute(t);
		return t;
	}
	
	public <R> Future<R> executeTask(Callable<R> task) {
		return context.getExecutorService().submit(task);
	}
	
	public void executeTask(Runnable r) {
		context.getExecutorService().submit(r);
	}
	
	public String getUUID() {
		return transport.getUUID();
	}
	
	public String getUsername() {
		return username;
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public long getTotalBytesIn() {
		return transport.incomingBytes;
	}
	
	public long getTotalBytesOut() {
		return transport.outgoingBytes;
	}
	
	@Deprecated
	/**
	 * @deprecated Use getRemoteIPAddress and getRemotePort instead.
	 */
	public InetAddress getRemoteAddress() {
   		return remoteAddress.getAddress();
	}
	
	public String getRemoteIPAddress() {
		return remoteAddress.getHostString();
	}
	
	public int getRemotePort() {
		return remoteAddress.getPort();
	}
	
	@Deprecated
	/**
	 * @deprecated Use getRemoteIPAddress and getRemotePort instead.
	 */
	public SocketAddress getRemoteSocketAddress(){ 
		return remoteAddress;
	}
	
	@Deprecated
	/**
	 * @deprecated Use getLocalIPAddress and getLocalPort instead.
	 */
	public SocketAddress getLocalSocketAddress() {
		return localAddress;
	}
	
	@Deprecated
	/**
	 * @deprecated Use getLocalIPAddress and getLocalPort instead.
	 */
	public InetAddress getLocalAddress() {
		return localAddress.getAddress();
	}
	
	public String getLocalIPAddress() {
		return localAddress.getHostString();
	}
	
	public int getLocalPort() {
		return localAddress.getPort();	
	}
	
	public boolean isDisconnected() {
		return getDisconnectFuture().isDone();
	}
	
	public boolean isDisconnecting() {
		return transport.isDisonnecting();
	}
	
	public void disconnect() {
		disconnect("By Application");
	}
	
	public void disconnect(String reason) {
		if(!closed)
			transport.disconnect(TransportProtocol.BY_APPLICATION, reason);
	}
	
	public Object getProperty(String name) {
		return properties.get(name);
	}
	
	public void setProperty(String name, Object val) {
		properties.put(name, val);
	}
	
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}
	
	public boolean isAuthenticated() {
		return connection!=null;
	}
	
	public T getContext() {
		return context;
	}

	public boolean containsProperty(String name) {
		return properties.containsKey(name);
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public ConnectionProtocol<T> getConnectionProtocol() {
		return connection;
	}

	public void removeProperty(String name) {
		properties.remove(name);
	}
	
	public String getHostKeyAlgorithm() {
		return transport.getHostKeyAlgorithm();
	}
	
	public String getCipherInUseCS() {
		return transport.getCipherCS();
	}
	
	public String getCipherInUseSC() {
		return transport.getCipherSC();
	}
	
	public String getMacInUseCS() {
		return transport.getMacCS();
	}
	
	public String getMacInUseSC() {
		return transport.getMacSC();
	}
	
	public String getCompressionInUseCS() {
		return transport.getCompressionCS();
	}
	
	public String getCompressionInUseSC() {
		return transport.getCompressionSC();
	}

	public void close() {
		this.closed = true;
		listeners.clear();
	}

	@Override
	public Locale getLocale() {
		return Objects.isNull(locale) ? context.getLocale() : locale;
	}

	@Override
	public SshConnectionManager getConnectionManager() {
		return context.getConnectionManager();
	}

	@Override
	public boolean isConnected() {
		return transport.isConnected();
	}

	@Override
	public void addTask(Integer queue, ConnectionAwareTask r) {
		transport.addTask(queue, r);
	}
	
	@Override
	public int getSessionCount() {
		int count = 0;
		for(ChannelNG<T> channel : connection.getActiveChannels()) {
			if(channel.getChannelType().equals("session")) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void disconnect(int reason, String message) {
		transport.disconnect(reason, message);
	}

	@Override
	public String getServerVersion() {
		return SshEngine.getVersion();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void openChannel(Channel channel) {
		if(connection == null)
			throw new IllegalStateException("Not connected.");
		connection.openChannel((ChannelNG<T>)channel);
	}
	
	public void startLogging(Level level) throws IOException {
		context.getConnectionManager().startLogging(this, level);
	}
	
	public void startLogging() throws IOException {
		context.getConnectionManager().startLogging(this);
	}

	public AbstractRequestFuture getDisconnectFuture() {
		return transport.disconnectFuture;
	}

	public SshPublicKey getHostKey() {
		return transport.getHostKey();
	}

	@Override
	public String[] getRemotePublicKeys() {
		return transport.getRemotePublicKeys();
	}

	@Override
	public String[] getRemoteKeyExchanges() {
		return transport.getRemoteKeyExchanges();
	}

	@Override
	public String[] getRemoteCompressionsCS() {
		return transport.getRemoteCompressionsCS();
	}

	@Override
	public String[] getRemoteCompressionsSC() {	
		return transport.getRemoteCompressionsSC();
	}

	@Override
	public String[] getRemoteCiphersCS() {
		return transport.getRemoteCiphersCS();
	}

	@Override
	public String[] getRemoteCiphersSC() {
		return transport.getRemoteCiphersSC();
	}

	@Override
	public String[] getRemoteMacsCS() {
		return transport.getRemoteMacsCS();
	}

	@Override
	public String[] getRemoteMacsSC() {
		return transport.getRemoteMacsSC();
	}

	@Override
	public String getKeyExchangeInUse() {
		return transport.getKeyExchangeInUse();
	}

	@Override
	public String getHostKeyInUse() {
		return transport.getHostKeyInUse();
	}

	public String getLocalIdentification() {
		return transport.getLocalIdentification();
	}

	@Override
	public void sendGlobalRequest(GlobalRequest request, boolean wantReply) {
		connection.sendGlobalRequest(request, wantReply);;
	}

	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}
	
	public void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

}
