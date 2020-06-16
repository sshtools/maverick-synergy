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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sshtools.common.auth.InMemoryMutualKeyAuthenticationStore;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.DisconnectRequestFuture;
import com.sshtools.common.nio.ProtocolContext;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;

public class CallbackClient {

	SshEngine ssh = new SshEngine();
	Set<CallbackSession> clients = new HashSet<CallbackSession>();
	ExecutorService executor;
	List<SshKeyPair> hostKeys = new ArrayList<>();
	ChannelFactory<SshServerContext> channelFactory = new DefaultServerChannelFactory();
	List<Object> defaultPolicies = new ArrayList<>();
	FileFactory fileFactory;
	
	public CallbackClient() {
		executor = getExecutorService();
		EventServiceImplementation.getInstance().addListener(new DisconnectionListener());
	}
	
	public SshEngine getSshEngine() {
		return ssh;
	}
	
	protected ExecutorService getExecutorService() {
		 return Executors.newCachedThreadPool();
	}

	public void setDefaultPolicies(Object... policies) {
		defaultPolicies.addAll(Arrays.asList(policies));
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
		start(new CallbackSession(config, this, hostname, port, false));
	}
	
	public synchronized void start(CallbackSession client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Starting client " + client.getConfig().getAgentName());
		}
		executor.execute(client);
	}
	
	void onClientConnected(CallbackSession client) {
		clients.add(client);
		onClientStart(client);
	}
	
	public boolean isConnected() {
		return ssh.isStarted() && !clients.isEmpty();
	}
	
	public Collection<CallbackSession> getClients() {
		return clients;
	}
	
	protected void onClientStart(CallbackSession client) {
		
	}
	
	protected void onClientStop(CallbackSession client) {
		
	}
	
	public synchronized void stop(CallbackSession client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Stopping callback client");
		}
		
		DisconnectRequestFuture future = client.stop();
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Callback client has disconnected [%s]", String.valueOf(future.isDone())));
		}
	}
	
	protected void reload(CallbackSession client) throws IOException {
		
	}
	
	public void stop() {
		
		for(CallbackSession client : new ArrayList<CallbackSession>(clients)) {
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
								CallbackSession client = (CallbackSession) con.getProperty("callbackClient");
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

	public ProtocolContext createContext(SshEngineContext daemonContext, CallbackConfiguration config) throws IOException, SshException {
		
		SshServerContext sshContext = new SshServerContext(getSshEngine(), JCEComponentManager.getDefaultInstance());
		
		for(SshKeyPair key : hostKeys) {
			sshContext.addHostKey(key);
		}
				
		for(Object policy : defaultPolicies) {
			sshContext.setPolicy(policy.getClass(), policy);
		}
		
		sshContext.setSoftwareVersionComments(CallbackSession.CALLBACK_IDENTIFIER + config.getAgentName());
		
		InMemoryMutualKeyAuthenticationStore authenticationStore = new InMemoryMutualKeyAuthenticationStore();
		authenticationStore.addKey(config.getAgentName(), config.getPrivateKey(), config.getPublicKey());
		MutualCallbackAuthenticationProvider provider = new MutualCallbackAuthenticationProvider(authenticationStore);
		sshContext.setAuthenicationMechanismFactory(new CallbackAuthenticationMechanismFactory<>(provider));
		sshContext.getPolicy(AuthenticationPolicy.class).addRequiredMechanism(
				MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION);
		
		sshContext.setSendIgnorePacketOnIdle(true);
		
		configureForwarding(sshContext, config);
		configureChannels(sshContext, config);
		configureFilesystem(sshContext, config);
		
		configureContext(sshContext, config);
				
		return sshContext;
	}

	protected void configureContext(SshServerContext sshContext, CallbackConfiguration config) {
	}

	protected void configureFilesystem(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.getPolicy(FileSystemPolicy.class).setFileFactory(fileFactory);
	}

	protected void configureChannels(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.setChannelFactory(channelFactory);
	}

	protected void configureForwarding(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.getForwardingPolicy().allowForwarding();
	}

	public void addHostKey(SshKeyPair pair) {
		this.hostKeys.add(pair);
	}

	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}

	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}
	
}
