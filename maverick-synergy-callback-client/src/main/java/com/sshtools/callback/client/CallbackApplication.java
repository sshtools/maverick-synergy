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
package com.sshtools.callback.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.DisconnectRequestFuture;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;

public class CallbackApplication {

	SshEngine ssh = new SshEngine();
	Set<CallbackClient> clients = new HashSet<CallbackClient>();
	ExecutorService executor;
	
	public CallbackApplication() {
		executor = getExecutorService();
		EventServiceImplementation.getInstance().addListener(new DisconnectionListener());
	}
	
	public SshEngine getSshEngine() {
		return ssh;
	}
	
	protected ExecutorService getExecutorService() {
		 return Executors.newCachedThreadPool();
	}
	
	protected CallbackClient createClient(CallbackConfiguration config, String hostname, int port, boolean onDemand) throws IOException {
		return new CallbackClient(config, this, hostname, port, onDemand) {
			
			@Override
			protected void onUpdateRequest(Connection<?> con, String updateInfo) {
				
			}
			
			@Override
			protected void configureContext(SshServerContext sshContext) throws IOException, SshException {
				
			}
		};
	}

	public void start(Collection<CallbackConfiguration> configs) {
		
		for(CallbackConfiguration config : configs) {
			
			try {
				start(config, config.getServerHost(), config.getServerPort());						
			} catch (Throwable e) {
				Log.error(String.format("Could not load configuration %s", config.getAgentName()), e);
			}
		}
	}
	
	public synchronized void start(CallbackConfiguration config) throws IOException {
		start(config, config.getServerHost(), config.getServerPort());
	}
	
	public synchronized void start(CallbackConfiguration config, String hostname, int port) throws IOException {
		start(createClient(config, hostname, port, false));
	}
	
	public synchronized void start(CallbackClient client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Starting client " + client.getConfig().getAgentName());
		}
		executor.execute(client);
	}
	
	void onClientConnected(CallbackClient client) {
		clients.add(client);
		onClientStart(client);
	}
	
	public boolean isConnected() {
		return ssh.isStarted() && !clients.isEmpty();
	}
	
	public Collection<CallbackClient> getClients() {
		return clients;
	}
	
	protected void onClientStart(CallbackClient client) {
		
	}
	
	protected void onClientStop(CallbackClient client) {
		
	}
	
	public synchronized void stop(CallbackClient client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Stopping callback client");
		}
		
		DisconnectRequestFuture future = client.stop();
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Callback client has disconnected [%s]", String.valueOf(future.isDone())));
		}
	}
	
	protected void reload(CallbackClient client) throws IOException {
		
	}
	
	public void stop() {
		
		for(CallbackClient client : new ArrayList<CallbackClient>(clients)) {
			stop(client);
		}
		
		executor.shutdownNow();
	}
	
	class DisconnectionListener implements EventListener {

		@Override
		public void processEvent(Event evt) {
			
			switch(evt.getId()) {
			case EventCodes.EVENT_DISCONNECTED:
				
				
				final Connection<?> con = (Connection<?>)evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
				
				if(!executor.isShutdown()) {
					executor.execute(new Runnable() {
						public void run() {
							if(con.containsProperty("callbackClient")) {
								CallbackClient client = (CallbackClient) con.getProperty("callbackClient");
								onClientStop(client);
								con.removeProperty("callbackClient");
								clients.remove(client);
								if(!client.isStopped()) {
									int count = 1;
									while(getSshEngine().isStarted()) {
										try {
											try {
												Thread.sleep(client.getConfig().getReconnectIntervalMs() * Math.min(count, 12));
											} catch (InterruptedException e1) {
											}
											reload(client);
											client.connect();
											break;
										} catch (IOException e) {
										}
										count++;
									}
								}
							}
						}
					});
				}
				
				break;
			default:
				break;
			}
		}
		
	}
	
}
