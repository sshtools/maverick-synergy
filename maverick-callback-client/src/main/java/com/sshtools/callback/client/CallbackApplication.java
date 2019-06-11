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

public abstract class CallbackApplication<T extends CallbackConfiguration> {

	
	
	SshEngine ssh = new SshEngine();
	Set<CallbackClient<T>> clients = new HashSet<CallbackClient<T>>();
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
	
	protected abstract CallbackClient<T> createClient(T config, String hostname, int port, boolean onDemand) throws IOException; 

	public void start(Collection<T> configs) {
		
		for(T config : configs) {
			
			try {
				start(config, config.getServerHost(), config.getServerPort());						
			} catch (Throwable e) {
				Log.error(String.format("Could not load configuration %s", config.getAgentName()), e);
			}
		}
	}
	
	public synchronized void start(T config) throws IOException {
		start(config, config.getServerHost(), config.getServerPort());
	}
	
	public synchronized void start(T config, String hostname, int port) throws IOException {
		start(createClient(config, hostname, port, false));
	}
	
	public synchronized void start(CallbackClient<T> client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Starting client " + client.getConfig().getAgentName());
		}
		executor.execute(client);
	}
	
	void onClientConnected(CallbackClient<T> client) {
		clients.add(client);
		onClientStart(client);
	}
	
	public boolean isConnected() {
		return ssh.isStarted() && !clients.isEmpty();
	}
	
	public Collection<CallbackClient<T>> getClients() {
		return clients;
	}
	
	protected void onClientStart(CallbackClient<T> client) {
		
	}
	
	protected void onClientStop(CallbackClient<T> client) {
		
	}
	
	public synchronized void stop(CallbackClient<T> client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Stopping callback client");
		}
		
		DisconnectRequestFuture future = client.stop();
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Callback client has disconnected [%s]", String.valueOf(future.isDone())));
		}
	}
	
	protected abstract void reload(CallbackClient<T> client) throws IOException;
	
	public void stop() {
		
		for(CallbackClient<T> client : new ArrayList<CallbackClient<T>>(clients)) {
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
								@SuppressWarnings("unchecked")
								CallbackClient<T> client = (CallbackClient<T>) con.getProperty("callbackClient");
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
