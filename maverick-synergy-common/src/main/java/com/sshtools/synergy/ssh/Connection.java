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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
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
	HashMap<String,Object> properties = new HashMap<>();
	InetSocketAddress remoteAddress;
	InetSocketAddress localAddress;
	T context;

	List<EventListener> listeners = new CopyOnWriteArrayList<>();
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

    @Override
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

        if (lastException != null) {
			throw lastException;
		}
    }

	@Override
	public AuthenticatedFuture getAuthenticatedFuture() {
		return transport.getAuthenticatedFuture();
	}

	@Override
	public String getSessionId() {
		return transport.getUUID();
	}

	@Override
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

	@Override
	public void executeTask(Runnable r) {
		context.getExecutorService().submit(r);
	}

	@Override
	public String getUUID() {
		return transport.getUUID();
	}

	@Override
	public String getUsername() {
		return username;
	}

	public Date getStartTime() {
		return startTime;
	}

	@Override
	public long getTotalBytesIn() {
		return transport.incomingBytes;
	}

	@Override
	public long getTotalBytesOut() {
		return transport.outgoingBytes;
	}

	@Override
	public InetAddress getLocalAddress() {
		return localAddress.getAddress();
	}

	@Override
	public String getRemoteIPAddress() {
		return remoteAddress.getHostString();
	}

	@Override
	public int getRemotePort() {
		return remoteAddress.getPort();
	}

	public String getLocalIPAddress() {
		return localAddress.getHostString();
	}

	@Override
	public int getLocalPort() {
		return localAddress.getPort();
	}

	public boolean isDisconnected() {
		return getDisconnectFuture().isDone();
	}

	@Override
	public boolean isDisconnecting() {
		return transport.isDisonnecting();
	}

	@Override
	public void disconnect() {
		disconnect("By Application");
	}

	@Override
	public void disconnect(String reason) {
		if(!closed) {
			transport.disconnect(TransportProtocol.BY_APPLICATION, reason);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O> O getProperty(String name) {
		return (O)properties.get(name);
	}

	@Override
	public void setProperty(String name, Object val) {
		properties.put(name, val);
	}

	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public boolean isAuthenticated() {
		return connection!=null;
	}

	@Override
	public T getContext() {
		return context;
	}

	@Override
	public boolean containsProperty(String name) {
		return properties.containsKey(name);
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

	public ConnectionProtocol<T> getConnectionProtocol() {
		return connection;
	}

	@Override
	public void removeProperty(String name) {
		properties.remove(name);
	}

	public String getHostKeyAlgorithm() {
		return transport.getHostKeyAlgorithm();
	}

	@Override
	public String getCipherInUseCS() {
		return transport.getCipherCS();
	}

	@Override
	public String getCipherInUseSC() {
		return transport.getCipherSC();
	}

	@Override
	public String getMacInUseCS() {
		return transport.getMacCS();
	}

	@Override
	public String getMacInUseSC() {
		return transport.getMacSC();
	}

	@Override
	public String getCompressionInUseCS() {
		return transport.getCompressionCS();
	}

	@Override
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
		if(connection == null) {
			throw new IllegalStateException("Not connected.");
		}
		connection.openChannel((ChannelNG<T>)channel);
	}

	@Override
	public void startLogging(Level level) throws IOException {
		context.getConnectionManager().startLogging(this, level);
	}

	@Override
	public void startLogging() throws IOException {
		context.getConnectionManager().startLogging(this);
	}

	@Override
	public AbstractRequestFuture getDisconnectFuture() {
		return transport.disconnectFuture;
	}

	@Override
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
	public void sendGlobalRequest(GlobalRequest request) {
		connection.sendGlobalRequest(request);
	}
	
	@Override
	public void sendGlobalRequestAndWait(GlobalRequest request, long timeout) {
		connection.sendGlobalRequestAndWait(request, timeout);
	}
	
	@Override
	@Deprecated
	public void sendGlobalRequest(GlobalRequest request, boolean wantReply) {
		connection.sendGlobalRequest(request);
	}

	public void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	public void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String toString() {
		return transport.getUUID();
	}
	
	

}
